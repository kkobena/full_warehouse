import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./feature/exports-page/exports-page.component').then(m => m.ExportsPageComponent),
    data: { pageTitle: 'Exports de données' },
  },
];

export default routes;
