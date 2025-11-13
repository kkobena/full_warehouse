import { Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { Authority } from 'app/shared/constants/authority.constants';

const motifRetourProduitRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./motif-retour-produit.component').then(m => m.MotifRetourProduitComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Motifs de Retour Produit',
    },
    canActivate: [UserRouteAccessService],
  },
];

export default motifRetourProduitRoutes;
