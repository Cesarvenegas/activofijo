import { Routes } from '@angular/router';
import { Layout } from './core/layout/layout';

export const routes: Routes = [
  {
    path: '',
    component: Layout,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        title: 'Inicio · Kromas Inventa Gov',
        loadComponent: () => import('./features/inicio/inicio').then((m) => m.Inicio),
      },
    ],
  },
  { path: '**', redirectTo: 'inicio' },
];
