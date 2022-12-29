import {Injectable} from '@angular/core';
import {HttpResponse} from '@angular/common/http';
import {ActivatedRouteSnapshot, Resolve, Router, Routes} from '@angular/router';
import {EMPTY, Observable, of} from 'rxjs';
import {flatMap} from 'rxjs/operators';
import {Authority} from 'app/shared/constants/authority.constants';
import {UserRouteAccessService} from 'app/core/auth/user-route-access.service';
import {GroupeFournisseur, IGroupeFournisseur} from '../../shared/model/groupe-fournisseur.model';
import {GroupeFournisseurService} from './groupe-fournisseur.service';
import {GroupeFournisseurComponent} from './groupe-fournisseur.component';

@Injectable({providedIn: 'root'})
export class SalesResolve implements Resolve<IGroupeFournisseur> {
  constructor(private service: GroupeFournisseurService, private router: Router) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<IGroupeFournisseur> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((sales: HttpResponse<GroupeFournisseur>) => {
          if (sales.body) {
            return of(sales.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new GroupeFournisseur());
  }
}

export const groupeFournisseurRoute: Routes = [
  {
    path: '',
    component: GroupeFournisseurComponent,
    data: {
      authorities: [Authority.USER],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.groupeFournisseur.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
