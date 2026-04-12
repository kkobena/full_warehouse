import { Routes } from '@angular/router';

const rayonRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./rayon.component').then(m => m.RayonComponent),
    data: { defaultSort: 'id,asc' },
  },
];

export default rayonRoute;
