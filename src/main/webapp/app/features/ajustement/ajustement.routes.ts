import { Routes } from '@angular/router';

export const AJUSTEMENT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/ajustement-home/ajustement-home.component').then(
        m => m.AjustementHomeComponent,
      ),
    data: { pageTitle: 'Ajustements de stock' },
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./feature/ajustement-form/ajustement-form.component').then(
        m => m.AjustementFormComponent,
      ),
    data: { pageTitle: 'Nouvel ajustement' },
  },
];

export default AJUSTEMENT_ROUTES;
