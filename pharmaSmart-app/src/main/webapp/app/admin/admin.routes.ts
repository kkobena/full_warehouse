import { Routes } from '@angular/router';

const routes: Routes = [
  {
    path: 'user-management',
    loadChildren: () => import('./user-management/user-management.route'),
    title: 'userManagement.home.title',
  },
  {
    path: 'access-management',
    loadChildren: () => import('../features/admin/access-managment-home/access-managment-home.route'),
    title: 'Rôles & Autorisations',
  },
];

export default routes;
