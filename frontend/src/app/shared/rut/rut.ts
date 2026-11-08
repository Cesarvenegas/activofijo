import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Utilidades de RUN/RUT chileno (misma lógica que cl.activofijo.comun.rut.Rut en el backend). */

export function calcularDv(cuerpo: number): string {
  let suma = 0;
  let multiplicador = 2;
  let resto = cuerpo;
  while (resto > 0) {
    suma += (resto % 10) * multiplicador;
    resto = Math.floor(resto / 10);
    multiplicador = multiplicador === 7 ? 2 : multiplicador + 1;
  }
  const dv = 11 - (suma % 11);
  return dv === 11 ? '0' : dv === 10 ? 'K' : String(dv);
}

/** "12.345.678-k" → "12345678-K"; null si no es válido. */
export function normalizarRut(texto: string | null | undefined): string | null {
  if (!texto) return null;
  const limpio = texto.replace(/[.\-\s]/g, '').toUpperCase();
  if (limpio.length < 2 || limpio.length > 9) return null;
  const cuerpoTxt = limpio.slice(0, -1);
  const dv = limpio.slice(-1);
  if (!/^\d+$/.test(cuerpoTxt)) return null;
  const cuerpo = Number(cuerpoTxt);
  if (cuerpo < 1_000_000 || calcularDv(cuerpo) !== dv) return null;
  return `${cuerpo}-${dv}`;
}

export function esRutValido(texto: string | null | undefined): boolean {
  return normalizarRut(texto) !== null;
}

/** "12345678-5" → "12.345.678-5". Si no es válido devuelve el texto tal cual. */
export function formatearRut(texto: string | null | undefined): string {
  const normalizado = normalizarRut(texto);
  if (!normalizado) return texto ?? '';
  const [cuerpo, dv] = normalizado.split('-');
  return `${cuerpo.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}-${dv}`;
}

/** Validador de formularios: vacío es válido (combinar con Validators.required). */
export const rutValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor = control.value as string | null;
  if (!valor) return null;
  return esRutValido(valor) ? null : { rut: true };
};
