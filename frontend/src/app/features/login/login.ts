import { Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';
import { RUTA_CAMBIO_PASSWORD } from '../../core/auth/guards';
import { mensajeDeError } from '../../core/http/problema';
import { formatearRut, normalizarRut, rutValidator } from '../../shared/rut/rut';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Ruta a la que volver tras ingresar (query param ?volver=). */
  readonly volver = input<string>();

  protected readonly form = inject(NonNullableFormBuilder).group({
    rut: ['', [Validators.required, rutValidator]],
    password: ['', Validators.required],
  });
  protected readonly ingresando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly verPassword = signal(false);

  protected formatearRutIngresado(): void {
    const c = this.form.controls.rut;
    if (normalizarRut(c.value)) c.setValue(formatearRut(c.value));
  }

  protected ingresar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { rut, password } = this.form.getRawValue();
    this.ingresando.set(true);
    this.error.set(null);
    this.auth.login(rut, password).subscribe({
      next: (u) => {
        const destino = u.debeCambiarPassword ? RUTA_CAMBIO_PASSWORD : this.destinoSeguro();
        this.router.navigateByUrl(destino);
      },
      error: (e) => {
        this.error.set(mensajeDeError(e, 'No se pudo iniciar sesión.'));
        this.form.controls.password.reset();
        this.ingresando.set(false);
      },
    });
  }

  /** Solo rutas internas (evita redirecciones a otros sitios). */
  private destinoSeguro(): string {
    const v = this.volver();
    return v && v.startsWith('/') && !v.startsWith('//') && !v.startsWith('/login') ? v : '/inicio';
  }
}
