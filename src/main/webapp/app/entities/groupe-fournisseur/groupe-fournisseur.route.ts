import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { GroupeFournisseurComponent } from './groupe-fournisseur.component';

const groupeFournisseurRoute: Routes = [
  {
    path: '',
    component: GroupeFournisseurComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.GROUPE_FOURNISSEUR],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default groupeFournisseurRoute;
