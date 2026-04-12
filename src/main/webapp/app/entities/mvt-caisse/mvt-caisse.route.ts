import { Routes } from '@angular/router';

const mvtCaisseRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./mvt-caisse.component').then(m => m.MvtCaisseComponent),
    data: { defaultSort: 'id,asc' },
  },
];

export default mvtCaisseRoute;
