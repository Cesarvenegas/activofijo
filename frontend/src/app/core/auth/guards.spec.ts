import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { AuthService, UsuarioActual } from './auth.service';
import { autenticadoGuard, invitadoGuard, rolGuard } from './guards';

const USUARIO: UsuarioActual = {
  id: 1, rut: '11111111-1', rutFormateado: '11.111.111-1', nombre: 'Ana', email: null, telefonoAnexo: null,
  cargo: null, rol: 'OPERADOR', debeCambiarPassword: false, ultimoAcceso: null,
};

describe('guards', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  function conSesion(u: UsuarioActual): void {
    auth.login(u.rut, 'x').subscribe();
    http.expectOne('/api/v1/auth/login').flush(u);
  }

  function ejecutar(guard: typeof autenticadoGuard, url: string) {
    return TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
    );
  }

  function url(resultado: unknown): string {
    return TestBed.inject(Router).serializeUrl(resultado as UrlTree);
  }

  it('sin sesión envía al login recordando la ruta', () => {
    expect(url(ejecutar(autenticadoGuard, '/usuarios'))).toBe('/login?volver=%2Fusuarios');
  });

  it('con sesión deja pasar', () => {
    conSesion(USUARIO);
    expect(ejecutar(autenticadoGuard, '/inicio')).toBe(true);
  });

  it('con clave temporal solo permite Mi cuenta', () => {
    conSesion({ ...USUARIO, debeCambiarPassword: true });
    expect(url(ejecutar(autenticadoGuard, '/inicio'))).toBe('/mi-cuenta');
    expect(ejecutar(autenticadoGuard, '/mi-cuenta')).toBe(true);
  });

  it('el login redirige al inicio si ya hay sesión', () => {
    expect(ejecutar(invitadoGuard, '/login')).toBe(true);
    conSesion(USUARIO);
    expect(url(ejecutar(invitadoGuard, '/login'))).toBe('/inicio');
  });

  it('rolGuard exige el rol', () => {
    conSesion(USUARIO);
    expect(url(ejecutar(rolGuard('ADMIN'), '/usuarios'))).toBe('/inicio');
    expect(ejecutar(rolGuard('ADMIN', 'OPERADOR'), '/x')).toBe(true);
  });
});
