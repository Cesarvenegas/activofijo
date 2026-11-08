import { HttpErrorResponse } from '@angular/common/http';

/** Cuerpo ProblemDetail (RFC 9457) que devuelve el backend. */
export interface Problema {
  title?: string;
  status?: number;
  detail?: string;
  codigo?: string;
  errores?: { campo: string; mensaje: string }[];
}

export function problemaDe(err: unknown): Problema {
  if (err instanceof HttpErrorResponse && err.error && typeof err.error === 'object') {
    return err.error as Problema;
  }
  return {};
}

/** Mensaje legible para mostrar al usuario a partir de un error HTTP. */
export function mensajeDeError(err: unknown, porDefecto = 'Ocurrió un error inesperado.'): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0 || err.status === 502 || err.status === 504) {
      return 'No se pudo contactar al servidor. Verifique su conexión.';
    }
    const p = problemaDe(err);
    if (p.errores?.length) {
      return p.errores.map((e) => e.mensaje).join('. ');
    }
    if (p.detail) return p.detail;
  }
  return porDefecto;
}
