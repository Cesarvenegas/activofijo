import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, Rol } from './auth.service';

export const RUTA_CAMBIO_PASSWORD = '/mi-cuenta';

/** Exige sesión. Con clave temporal, solo deja entrar a "Mi cuenta" para cambiarla. */
export const autenticadoGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const usuario = auth.usuario();
  if (!usuario) {
    return router.createUrlTree(['/login'], { queryParams: { volver: state.url } });
  }
  if (usuario.debeCambiarPassword && !state.url.startsWith(RUTA_CAMBIO_PASSWORD)) {
    return router.createUrlTree([RUTA_CAMBIO_PASSWORD]);
  }
  return true;
};

/** La pantalla de login solo para quien no tiene sesión. */
export const invitadoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.autenticado() ? inject(Router).createUrlTree(['/inicio']) : true;
};

/** Exige uno de los roles indicados. */
export function rolGuard(...roles: Rol[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    return auth.tieneRol(...roles) ? true : inject(Router).createUrlTree(['/inicio']);
  };
}
