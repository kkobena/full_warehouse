import { Routes } from '@angular/router';

const formeRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./parametre.component').then(m => m.ParametreComponent),
    data: { defaultSort: 'id,asc' },
  },
];

export default formeRoute;
