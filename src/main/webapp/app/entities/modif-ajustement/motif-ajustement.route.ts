import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ModifAjustementComponent } from './modif-ajustement.component';

const motifAjustementRoute: Routes = [
  {
    path: '',
    component: ModifAjustementComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.REFERENTIEL, Authority.MOTIF_AJUSTEMENT],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default motifAjustementRoute;
