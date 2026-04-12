import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/differes-layout/differes-layout.component').then(m => m.DifferesLayoutComponent),
    data: { pageTitle: 'facturation.differes' },
  },
  {
    path: 'reglements',
    redirectTo: '',
    pathMatch: 'full',
  },
];

export default routes;
