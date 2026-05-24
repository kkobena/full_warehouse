import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./nav-manager.component').then(m => m.NavManagerComponent),
  },
];

export default routes;
