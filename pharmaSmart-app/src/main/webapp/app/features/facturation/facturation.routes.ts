import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/facturation-layout/facturation-layout.component').then(m => m.FacturationLayoutComponent),
    data: { pageTitle: 'facturation.factures' },
  },
  {
    path: 'reglements',
    redirectTo: '',
    pathMatch: 'full',
  },
];

export default routes;
