import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { TypeEtiquetteComponent } from './type-etiquette.component';

const typeEtiquetteRoute: Routes = [
  {
    path: '',
    component: TypeEtiquetteComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.REFERENTIEL],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default typeEtiquetteRoute;
