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
  {
    path: 'ecarts',
    loadComponent: () =>
      import('./feature/ecarts-a-regulariser/ecarts-a-regulariser.component').then(
        m => m.EcartsARegulariserComponent,
      ),
    data: { pageTitle: 'Écarts à régulariser' },
  },
];

export default AJUSTEMENT_ROUTES;
