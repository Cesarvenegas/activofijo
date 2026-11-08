import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { RUTA_CAMBIO_PASSWORD } from '../auth/guards';
import { problemaDe } from './problema';

/** Las respuestas de estas rutas las maneja la pantalla que las llama. */
const RUTAS_PROPIAS = ['/api/v1/auth/login', '/api/v1/auth/yo', '/api/v1/auth/csrf', '/api/v1/salud'];

/**
 * Manejo global de errores HTTP:
 * 401 → sesión expirada, ir al login · 403 DEBE_CAMBIAR_PASSWORD → Mi cuenta · otro 403 → aviso.
 */
export const erroresInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const auth = inject(AuthService);
  const snackBar = inject(MatSnackBar);

  return next(req).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && !RUTAS_PROPIAS.some((r) => req.url.startsWith(r))) {
        const codigo = problemaDe(err).codigo;
        if (err.status === 401) {
          auth.sesionExpirada();
          snackBar.open('Su sesión expiró. Ingrese nuevamente.', 'Cerrar', { duration: 5000 });
          router.navigate(['/login'], { queryParams: { volver: router.url } });
        } else if (err.status === 403 && codigo === 'DEBE_CAMBIAR_PASSWORD') {
          router.navigateByUrl(RUTA_CAMBIO_PASSWORD);
        } else if (err.status === 403 && codigo === 'CSRF_INVALIDO') {
          snackBar.open('La sesión de seguridad expiró. Recargue la página.', 'Cerrar', { duration: 6000 });
        } else if (err.status === 403) {
          snackBar.open('No tiene permisos para realizar esta operación.', 'Cerrar', { duration: 5000 });
        }
      }
      return throwError(() => err);
    }),
  );
};
