import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Rol } from '../../core/auth/auth.service';
import { ConsultaTabla, Pagina } from '../../shared/tabla-filtrable/tabla-filtrable';

export interface Usuario {
  id: number;
  rut: string;
  rutFormateado: string;
  nombre: string;
  email: string | null;
  telefonoAnexo: string | null;
  cargo: string | null;
  rol: Rol;
  activo: boolean;
  debeCambiarPassword: boolean;
  bloqueado: boolean;
  ultimoAcceso: string | null;
  creadoEn: string;
}

export interface FiltroUsuarios {
  texto?: string;
  rol?: Rol | '';
  activo?: boolean | '';
}

export interface UsuarioDatos {
  nombre: string;
  email: string | null;
  telefonoAnexo: string | null;
  cargo: string | null;
  rol: Rol;
}

export interface PasswordTemporal {
  usuario: Usuario;
  passwordTemporal: string;
}

@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/v1/usuarios';

  listar(c: FiltroUsuarios & ConsultaTabla): Observable<Pagina<Usuario>> {
    let params = new HttpParams().set('page', c.page).set('size', c.size);
    if (c.sort) params = params.set('sort', c.sort);
    if (c.texto?.trim()) params = params.set('texto', c.texto.trim());
    if (c.rol) params = params.set('rol', c.rol);
    if (c.activo !== '' && c.activo !== undefined) params = params.set('activo', c.activo);
    return this.http.get<Pagina<Usuario>>(this.url, { params });
  }

  crear(datos: UsuarioDatos & { rut: string }): Observable<PasswordTemporal> {
    return this.http.post<PasswordTemporal>(this.url, datos);
  }

  editar(id: number, datos: UsuarioDatos): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.url}/${id}`, datos);
  }

  cambiarActivo(id: number, activo: boolean): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.url}/${id}/activo`, { activo });
  }

  resetearPassword(id: number): Observable<PasswordTemporal> {
    return this.http.post<PasswordTemporal>(`${this.url}/${id}/resetear-password`, null);
  }
}
