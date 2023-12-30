import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { FormeProduitComponent } from './forme-produit.component';

const formeRoute: Routes = [
  {
    path: '',
    component: FormeProduitComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.FORME_PRODUIT],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default formeRoute;
