import { Routes } from '@angular/router';

const familleProduitRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./famille-produit.component').then(m => m.FamilleProduitComponent),
    data: { defaultSort: 'id,asc' },
  },
];

export default familleProduitRoute;
