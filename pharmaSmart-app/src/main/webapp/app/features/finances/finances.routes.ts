import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/finances-layout/finances-layout.component').then(
        m => m.FinancesLayoutComponent,
      ),
    data: { pageTitle: 'Finances' },
  },
];

export default routes;
