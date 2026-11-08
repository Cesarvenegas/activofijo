import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { Login } from './login';

describe('Login', () => {
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  async function crear() {
    const fixture = TestBed.createComponent(Login);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const escribir = (sel: string, valor: string) => {
      const input = el.querySelector<HTMLInputElement>(sel)!;
      input.value = valor;
      input.dispatchEvent(new Event('input'));
      input.dispatchEvent(new Event('blur'));
    };
    const enviar = async () => {
      el.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
      await fixture.whenStable();
    };
    return { fixture, el, escribir, enviar };
  }

  it('formatea el RUN y entra al inicio', async () => {
    const { el, escribir, enviar } = await crear();
    escribir('input[formcontrolname=rut]', '111111111');
    escribir('input[formcontrolname=password]', 'Kromas2026');
    expect(el.querySelector<HTMLInputElement>('input[formcontrolname=rut]')!.value).toBe('11.111.111-1');

    await enviar();
    const req = http.expectOne('/api/v1/auth/login');
    expect(req.request.body).toEqual({ rut: '11.111.111-1', password: 'Kromas2026' });
    req.flush({ id: 1, rut: '11111111-1', rol: 'ADMIN', debeCambiarPassword: false });

    expect(router.navigateByUrl).toHaveBeenCalledWith('/inicio');
  });

  it('con clave temporal va a Mi cuenta', async () => {
    const { escribir, enviar } = await crear();
    escribir('input[formcontrolname=rut]', '11.111.111-1');
    escribir('input[formcontrolname=password]', 'Temporal99');
    await enviar();
    http.expectOne('/api/v1/auth/login').flush({ id: 1, rol: 'ADMIN', debeCambiarPassword: true });

    expect(router.navigateByUrl).toHaveBeenCalledWith('/mi-cuenta');
  });

  it('muestra el mensaje del servidor si falla', async () => {
    const { fixture, el, escribir, enviar } = await crear();
    escribir('input[formcontrolname=rut]', '11.111.111-1');
    escribir('input[formcontrolname=password]', 'mala');
    await enviar();
    http.expectOne('/api/v1/auth/login').flush(
      { detail: 'RUN o contraseña incorrectos', codigo: 'CREDENCIALES_INVALIDAS' },
      { status: 401, statusText: 'Unauthorized' },
    );
    await fixture.whenStable();

    expect(el.querySelector('[role=alert]')?.textContent).toContain('RUN o contraseña incorrectos');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('no envía si el RUN es inválido', async () => {
    const { escribir, enviar } = await crear();
    escribir('input[formcontrolname=rut]', '11.111.111-2');
    escribir('input[formcontrolname=password]', 'Kromas2026');
    await enviar();

    http.expectNone('/api/v1/auth/login');
  });
});
