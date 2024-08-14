import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ParametreComponent } from './parametre.component';

const formeRoute: Routes = [
  {
    path: '',
    component: ParametreComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.PARAMETRE],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default formeRoute;
