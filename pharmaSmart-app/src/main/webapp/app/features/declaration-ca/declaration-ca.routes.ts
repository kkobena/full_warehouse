import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/declaration-ca-layout/declaration-ca-layout.component').then(
        m => m.DeclarationCaLayoutComponent,
      ),
    data: { pageTitle: 'Retraitement du CA' },
  },
];

export default routes;
