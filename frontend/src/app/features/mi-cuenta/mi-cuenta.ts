import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AbstractControl, FormGroupDirective, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors,
  Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService, NOMBRE_ROL } from '../../core/auth/auth.service';
import { mensajeDeError } from '../../core/http/problema';

/** Misma política que el backend (PoliticaPassword). */
function politicaPassword(c: AbstractControl): ValidationErrors | null {
  const v = (c.value as string) ?? '';
  if (!v) return null;
  return v.length >= 8 && /[A-Za-zÀ-ÿ]/.test(v) && /\d/.test(v) ? null : { politica: true };
}

function coinciden(grupo: AbstractControl): ValidationErrors | null {
  const nueva = grupo.get('passwordNueva')?.value;
  const confirmacion = grupo.get('confirmacion')?.value;
  return nueva && confirmacion && nueva !== confirmacion ? { noCoinciden: true } : null;
}

@Component({
  selector: 'app-mi-cuenta',
  imports: [DatePipe, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule,
    MatIconModule, MatProgressSpinnerModule],
  templateUrl: './mi-cuenta.html',
  styleUrl: './mi-cuenta.scss',
})
export class MiCuenta {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly usuario = this.auth.usuario;
  protected readonly nombreRol = NOMBRE_ROL;
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      passwordActual: ['', Validators.required],
      passwordNueva: ['', [Validators.required, politicaPassword]],
      confirmacion: ['', Validators.required],
    },
    { validators: coinciden },
  );

  protected cambiar(formDir: FormGroupDirective): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const eraObligatorio = this.usuario()?.debeCambiarPassword ?? false;
    const { passwordActual, passwordNueva } = this.form.getRawValue();
    this.guardando.set(true);
    this.error.set(null);
    this.auth.cambiarPassword(passwordActual, passwordNueva).subscribe({
      next: () => {
        this.guardando.set(false);
        formDir.resetForm();
        this.snackBar.open('Contraseña actualizada', 'Cerrar', { duration: 4000 });
        if (eraObligatorio) this.router.navigateByUrl('/inicio');
      },
      error: (e) => {
        this.error.set(mensajeDeError(e, 'No se pudo cambiar la contraseña.'));
        this.guardando.set(false);
      },
    });
  }
}
