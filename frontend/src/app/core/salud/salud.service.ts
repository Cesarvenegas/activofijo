import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';

/** Respuesta de GET /api/v1/salud (ver SaludDto en el backend). */
export interface SaludDto {
  aplicacion: string;
  version: string;
  estado: 'OK' | 'ERROR';
  baseDatos: { estado: 'OK' | 'ERROR'; version: string | null; mensaje: string | null };
  fecha: string;
}

/** Resultado ya interpretado para la UI: nunca lanza error. */
export type EstadoSistema =
  | { tipo: 'OK'; salud: SaludDto }
  | { tipo: 'SIN_BASE_DATOS'; salud: SaludDto }
  | { tipo: 'SIN_SERVIDOR'; mensaje: string };

@Injectable({ providedIn: 'root' })
export class SaludService {
  private readonly http = inject(HttpClient);

  consultar(): Observable<EstadoSistema> {
    return this.http.get<SaludDto>('/api/v1/salud').pipe(
      map((salud): EstadoSistema => ({ tipo: 'OK', salud })),
      catchError((err: HttpErrorResponse) => of(interpretarError(err))),
    );
  }
}

function interpretarError(err: HttpErrorResponse): EstadoSistema {
  // 503 con cuerpo de /salud: el backend está vivo pero la base de datos no responde.
  if (err.status === 503 && err.error?.baseDatos) {
    return { tipo: 'SIN_BASE_DATOS', salud: err.error as SaludDto };
  }
  // 0, 500, 502, 504 sin cuerpo propio: el backend no está levantado (o el proxy no lo alcanza).
  return {
    tipo: 'SIN_SERVIDOR',
    mensaje: 'No se pudo contactar al servidor. Verifique que el backend esté en ejecución.',
  };
}
