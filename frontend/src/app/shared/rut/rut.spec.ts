import { FormControl } from '@angular/forms';
import { calcularDv, esRutValido, formatearRut, normalizarRut, rutValidator } from './rut';

describe('rut', () => {
  it('calcula el dígito verificador', () => {
    expect(calcularDv(11111111)).toBe('1');
    expect(calcularDv(12345678)).toBe('5');
    expect(calcularDv(10000013)).toBe('K');
    expect(calcularDv(10000004)).toBe('0');
  });

  it('normaliza distintas escrituras', () => {
    expect(normalizarRut('12.345.678-5')).toBe('12345678-5');
    expect(normalizarRut('123456785')).toBe('12345678-5');
    expect(normalizarRut(' 10.000.013-k ')).toBe('10000013-K');
  });

  it('rechaza RUN inválidos', () => {
    for (const r of ['', null, '12.345.678-9', 'abc', '999999-3', '12a45678-5']) {
      expect(esRutValido(r)).toBe(false);
    }
  });

  it('formatea con puntos', () => {
    expect(formatearRut('12345678-5')).toBe('12.345.678-5');
    expect(formatearRut('1000005k')).toBe('1.000.005-K');
    expect(formatearRut('basura')).toBe('basura');
  });

  it('validador de formulario', () => {
    expect(rutValidator(new FormControl(''))).toBeNull();
    expect(rutValidator(new FormControl('11.111.111-1'))).toBeNull();
    expect(rutValidator(new FormControl('11.111.111-2'))).toEqual({ rut: true });
  });
});
