import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEsCl from '@angular/common/locales/es-CL';
import { Inicio } from './inicio';
import { SaludDto } from '../../core/salud/salud.service';

registerLocaleData(localeEsCl, 'es-CL');

const SALUD_OK: SaludDto = {
  aplicacion: 'kromas-inventa-gov',
  version: '0.0.1-SNAPSHOT',
  estado: 'OK',
  baseDatos: { estado: 'OK', version: 'PostgreSQL 17.6', mensaje: null },
  fecha: '2026-11-08T17:30:00-03:00',
};

describe('Inicio', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Inicio],
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: LOCALE_ID, useValue: 'es-CL' }],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function renderizarCon(responder: (req: ReturnType<HttpTestingController['expectOne']>) => void) {
    const fixture = TestBed.createComponent(Inicio);
    responder(http.expectOne('/api/v1/salud'));
    await fixture.whenStable();
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).querySelector('[data-testid="estado-bd"]')?.textContent ?? '';
  }

  it('muestra "Base de datos: OK" cuando el backend responde bien', async () => {
    const texto = await renderizarCon((req) => req.flush(SALUD_OK));
    expect(texto).toContain('Base de datos: OK');
  });

  it('muestra error claro cuando la base de datos no responde (503)', async () => {
    const caida: SaludDto = {
      ...SALUD_OK,
      estado: 'ERROR',
      baseDatos: { estado: 'ERROR', version: null, mensaje: 'No se pudo conectar con la base de datos' },
    };
    const texto = await renderizarCon((req) =>
      req.flush(caida, { status: 503, statusText: 'Service Unavailable' }),
    );
    expect(texto).toContain('Base de datos: sin conexión');
  });

  it('muestra error claro cuando el backend está apagado', async () => {
    const texto = await renderizarCon((req) => req.error(new ProgressEvent('error'), { status: 0 }));
    expect(texto).toContain('No se pudo contactar al servidor');
  });
});
