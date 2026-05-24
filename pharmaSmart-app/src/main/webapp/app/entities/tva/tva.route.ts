import { Routes } from '@angular/router';

const tvaRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./tva.component').then(m => m.TvaComponent),
    data: { defaultSort: 'id,asc' },
  },
];

export default tvaRoute;
