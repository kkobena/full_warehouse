import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { GammeProduitComponent } from './gamme-produit.component';

const gammeProduitRoute: Routes = [
  {
    path: '',
    component: GammeProduitComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.GAMME_PRODUIT],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default gammeProduitRoute;
