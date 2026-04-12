import { Routes } from '@angular/router';

const motifRetourProduitRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./motif-retour-produit.component').then(m => m.MotifRetourProduitComponent),
    data: { pageTitle: 'Motifs de Retour Produit' },
  },
];

export default motifRetourProduitRoutes;
