import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Observable, map } from 'rxjs';
import { NOMBRE_ROL, Rol } from '../../core/auth/auth.service';
import { mensajeDeError } from '../../core/http/problema';
import { formatearRut, normalizarRut, rutValidator } from '../../shared/rut/rut';
import { PasswordTemporal, Usuario, UsuariosService } from './usuarios.service';

/** Resultado al cerrar: el usuario editado, o la clave temporal si se creó uno nuevo. */
export type ResultadoFormUsuario = { tipo: 'editado'; usuario: Usuario } | { tipo: 'creado'; resultado: PasswordTemporal };

@Component({
  selector: 'app-usuario-form-dialog',
  imports: [
    ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './usuario-form-dialog.html',
  styles: `
    form { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; padding-top: 8px; }
    .completo { grid-column: 1 / -1; }
    .error { color: var(--mat-sys-error); margin: 0 0 8px; }
    @media (max-width: 599px) { form { grid-template-columns: 1fr; } }
  `,
})
export class UsuarioFormDialog {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly service = inject(UsuariosService);
  private readonly ref = inject<MatDialogRef<UsuarioFormDialog, ResultadoFormUsuario>>(MatDialogRef);
  protected readonly usuario = inject<Usuario | null>(MAT_DIALOG_DATA);

  protected readonly roles = Object.entries(NOMBRE_ROL) as [Rol, string][];
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.group({
    rut: [this.usuario?.rutFormateado ?? '', [Validators.required, rutValidator]],
    nombre: [this.usuario?.nombre ?? '', [Validators.required, Validators.maxLength(150)]],
    email: [this.usuario?.email ?? '', [Validators.email, Validators.maxLength(150)]],
    telefonoAnexo: [this.usuario?.telefonoAnexo ?? '', Validators.maxLength(30)],
    cargo: [this.usuario?.cargo ?? '', Validators.maxLength(100)],
    rol: [this.usuario?.rol ?? ('CONSULTA' as Rol), Validators.required],
  });

  constructor() {
    if (this.usuario) {
      this.form.controls.rut.disable();   // el RUN es el login: no se modifica
    }
  }

  /** Al salir del campo, muestra el RUN con puntos. */
  protected formatearRutIngresado(): void {
    const c = this.form.controls.rut;
    if (normalizarRut(c.value)) c.setValue(formatearRut(c.value));
  }

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const datos = {
      nombre: v.nombre.trim(),
      email: v.email.trim() || null,
      telefonoAnexo: v.telefonoAnexo.trim() || null,
      cargo: v.cargo.trim() || null,
      rol: v.rol,
    };
    this.guardando.set(true);
    this.error.set(null);
    const peticion: Observable<ResultadoFormUsuario> = this.usuario
      ? this.service.editar(this.usuario.id, datos).pipe(map((usuario) => ({ tipo: 'editado' as const, usuario })))
      : this.service.crear({ ...datos, rut: v.rut }).pipe(map((resultado) => ({ tipo: 'creado' as const, resultado })));
    peticion.subscribe({
      next: (r) => this.ref.close(r),
      error: (e) => {
        this.error.set(mensajeDeError(e, 'No se pudo guardar el usuario.'));
        this.guardando.set(false);
      },
    });
  }
}
