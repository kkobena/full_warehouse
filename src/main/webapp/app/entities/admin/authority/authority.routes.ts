import { Routes } from '@angular/router';
import AuthorityResolve from './route/authority-routing-resolve.service';

const authorityRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/authority.component').then(m => m.AuthorityComponent),
  },
  {
    path: ':name/view',
    loadComponent: () => import('./detail/authority-detail.component').then(m => m.AuthorityDetailComponent),
    resolve: { authority: AuthorityResolve },
  },
  {
    path: 'new',
    loadComponent: () => import('./update/authority-update.component').then(m => m.AuthorityUpdateComponent),
    resolve: { authority: AuthorityResolve },
  },
];

export default authorityRoute;
