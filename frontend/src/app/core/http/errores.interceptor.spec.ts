import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { vi } from 'vitest';
import { AuthService } from '../auth/auth.service';
import { erroresInterceptor } from './errores.interceptor';

describe('erroresInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let router: Router;
  let snack: { open: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    snack = { open: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([erroresInterceptor])),
        provideHttpClientTesting(),
        { provide: MatSnackBar, useValue: snack },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  it('401 olvida la sesión y lleva al login', () => {
    const auth = TestBed.inject(AuthService);
    const espia = vi.spyOn(auth, 'sesionExpirada');
    http.get('/api/v1/usuarios').subscribe({ error: () => undefined });
    backend.expectOne('/api/v1/usuarios').flush({ codigo: 'NO_AUTENTICADO' }, { status: 401, statusText: '' });

    expect(espia).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], expect.anything());
  });

  it('403 por clave temporal lleva a Mi cuenta', () => {
    http.get('/api/v1/usuarios').subscribe({ error: () => undefined });
    backend.expectOne('/api/v1/usuarios')
      .flush({ codigo: 'DEBE_CAMBIAR_PASSWORD' }, { status: 403, statusText: '' });

    expect(router.navigateByUrl).toHaveBeenCalledWith('/mi-cuenta');
  });

  it('403 sin permiso muestra un aviso', () => {
    http.get('/api/v1/usuarios').subscribe({ error: () => undefined });
    backend.expectOne('/api/v1/usuarios').flush({ codigo: 'SIN_PERMISO' }, { status: 403, statusText: '' });

    expect(snack.open).toHaveBeenCalledWith('No tiene permisos para realizar esta operación.', 'Cerrar', expect.anything());
  });

  it('no interviene en el login (lo maneja la pantalla)', () => {
    http.post('/api/v1/auth/login', {}).subscribe({ error: () => undefined });
    backend.expectOne('/api/v1/auth/login').flush({}, { status: 401, statusText: '' });

    expect(router.navigate).not.toHaveBeenCalled();
  });
});
