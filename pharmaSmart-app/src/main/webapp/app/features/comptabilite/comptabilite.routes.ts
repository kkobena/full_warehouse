import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/comptabilite-layout/comptabilite-layout.component').then(
        m => m.ComptabiliteLayoutComponent,
      ),
    data: { pageTitle: 'Comptabilité' },
  },
];

export default routes;
