import { Routes } from '@angular/router';

const fournisseurRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./fournisseur-home.component').then(m => m.FournisseurHomeComponent),
    data: { defaultSort: 'id,asc' },
  },
];

export default fournisseurRoute;
