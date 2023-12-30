import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { LaboratoireProduitComponent } from './laboratoire-produit.component';

const laboratoireProduitRoute: Routes = [
  {
    path: '',
    component: LaboratoireProduitComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.LABORATOIRE],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default laboratoireProduitRoute;
