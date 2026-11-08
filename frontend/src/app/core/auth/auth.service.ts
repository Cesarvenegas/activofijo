import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, firstValueFrom, map, of, switchMap, tap } from 'rxjs';

export type Rol = 'ADMIN' | 'OPERADOR' | 'CONSULTA';

export const NOMBRE_ROL: Record<Rol, string> = {
  ADMIN: 'Administrador',
  OPERADOR: 'Operador',
  CONSULTA: 'Consulta',
};

export interface UsuarioActual {
  id: number;
  rut: string;
  rutFormateado: string;
  nombre: string;
  email: string | null;
  telefonoAnexo: string | null;
  cargo: string | null;
  rol: Rol;
  debeCambiarPassword: boolean;
  ultimoAcceso: string | null;
}

/** Sesión del usuario. La sesión vive en una cookie HttpOnly; aquí solo se guarda quién es. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly _usuario = signal<UsuarioActual | null>(null);

  readonly usuario = this._usuario.asReadonly();
  readonly autenticado = computed(() => this._usuario() !== null);

  /** Al iniciar la app: obtiene la cookie CSRF y, si hay sesión abierta, el usuario. */
  inicializar(): Promise<void> {
    return firstValueFrom(
      this.http.get<void>('/api/v1/auth/csrf').pipe(
        switchMap(() => this.http.get<UsuarioActual>('/api/v1/auth/yo')),
        map((u) => this._usuario.set(u)),
        // Sin sesión (401) o sin servidor: la app parte en el login
        catchError(() => of(this._usuario.set(null))),
      ),
    );
  }

  login(rut: string, password: string): Observable<UsuarioActual> {
    return this.http
      .post<UsuarioActual>('/api/v1/auth/login', { rut, password })
      .pipe(tap((u) => this._usuario.set(u)));
  }

  /** Cierra la sesión y pide un token CSRF nuevo para el próximo login. */
  logout(): Observable<void> {
    return this.http.post<void>('/api/v1/auth/logout', null).pipe(
      catchError(() => of(undefined)),   // si la sesión ya expiró, igual se sale
      switchMap(() => this.http.get<void>('/api/v1/auth/csrf').pipe(catchError(() => of(undefined)))),
      tap(() => this._usuario.set(null)),
    );
  }

  cambiarPassword(passwordActual: string, passwordNueva: string): Observable<UsuarioActual> {
    return this.http
      .post<UsuarioActual>('/api/v1/auth/cambiar-password', { passwordActual, passwordNueva })
      .pipe(tap((u) => this._usuario.set(u)));
  }

  /** Olvida al usuario localmente (la sesión del servidor ya expiró). */
  sesionExpirada(): void {
    this._usuario.set(null);
  }

  tieneRol(...roles: Rol[]): boolean {
    const u = this._usuario();
    return !!u && roles.includes(u.rol);
  }
}
