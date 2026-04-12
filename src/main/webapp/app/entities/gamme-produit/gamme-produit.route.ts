import { Routes } from '@angular/router';

const gammeProduitRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./gamme-produit.component').then(m => m.GammeProduitComponent),
    data: { defaultSort: 'id,asc' },
  },
];

export default gammeProduitRoute;
