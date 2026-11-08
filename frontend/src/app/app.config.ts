import {
  ApplicationConfig, LOCALE_ID, inject, provideAppInitializer, provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEsCl from '@angular/common/locales/es-CL';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { MAT_ICON_DEFAULT_OPTIONS } from '@angular/material/icon';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { routes } from './app.routes';
import { AuthService } from './core/auth/auth.service';
import { erroresInterceptor } from './core/http/errores.interceptor';
import { PaginadorEs } from './core/i18n/paginador-es';

registerLocaleData(localeEsCl, 'es-CL');

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    // Angular envía automáticamente la cookie XSRF-TOKEN como cabecera X-XSRF-TOKEN en POST/PUT/PATCH/DELETE
    provideHttpClient(withFetch(), withInterceptors([erroresInterceptor])),
    // Antes de mostrar la app: token CSRF + ¿hay sesión abierta?
    provideAppInitializer(() => inject(AuthService).inicializar()),
    { provide: LOCALE_ID, useValue: 'es-CL' },
    { provide: MAT_ICON_DEFAULT_OPTIONS, useValue: { fontSet: 'material-symbols-outlined' } },
    { provide: MatPaginatorIntl, useClass: PaginadorEs },
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline' } },
  ],
};
