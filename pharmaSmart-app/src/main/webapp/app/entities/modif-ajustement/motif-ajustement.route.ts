import { Routes } from '@angular/router';

const motifAjustementRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./modif-ajustement.component').then(m => m.ModifAjustementComponent),
    data: { defaultSort: 'id,asc' },
  },
];

export default motifAjustementRoute;
