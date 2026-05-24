import { Routes } from '@angular/router';

const laboratoireProduitRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./laboratoire-produit.component').then(m => m.LaboratoireProduitComponent),
    data: { defaultSort: 'id,asc' },
  },
];

export default laboratoireProduitRoute;
