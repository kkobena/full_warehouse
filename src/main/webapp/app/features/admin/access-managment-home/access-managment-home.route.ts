import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./access-managment-home.component').then(m => m.AccessManagmentHomeComponent),
  },
];

export default routes;
