import { Routes } from '@angular/router';
import { Layout } from './core/layout/layout';
import { autenticadoGuard, invitadoGuard, rolGuard } from './core/auth/guards';

const TITULO = 'Kromas Inventa Gov';

export const routes: Routes = [
  {
    path: 'login',
    title: `Ingresar · ${TITULO}`,
    canActivate: [invitadoGuard],
    loadComponent: () => import('./features/login/login').then((m) => m.Login),
  },
  {
    path: '',
    component: Layout,
    canActivate: [autenticadoGuard],
    canActivateChild: [autenticadoGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        title: `Inicio · ${TITULO}`,
        loadComponent: () => import('./features/inicio/inicio').then((m) => m.Inicio),
      },
      {
        path: 'mi-cuenta',
        title: `Mi cuenta · ${TITULO}`,
        loadComponent: () => import('./features/mi-cuenta/mi-cuenta').then((m) => m.MiCuenta),
      },
      {
        path: 'usuarios',
        title: `Usuarios · ${TITULO}`,
        canActivate: [rolGuard('ADMIN')],
        loadComponent: () => import('./features/usuarios/usuarios').then((m) => m.Usuarios),
      },
    ],
  },
  { path: '**', redirectTo: 'inicio' },
];
