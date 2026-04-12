import { Routes } from '@angular/router';

const formeRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./forme-produit.component').then(m => m.FormeProduitComponent),
    data: { defaultSort: 'id,asc' },
  },
];

export default formeRoute;
