import { Routes } from '@angular/router';

const posteRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./poste.component').then(m => m.PosteComponent),
    data: { defaultSort: 'id,asc' },
  },
];

export default posteRoute;
