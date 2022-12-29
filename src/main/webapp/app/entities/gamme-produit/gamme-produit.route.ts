import {Injectable} from '@angular/core';
import {HttpResponse} from '@angular/common/http';
import {ActivatedRouteSnapshot, Resolve, Router, Routes} from '@angular/router';
import {EMPTY, Observable, of} from 'rxjs';
import {flatMap} from 'rxjs/operators';
import {Authority} from 'app/shared/constants/authority.constants';
import {UserRouteAccessService} from 'app/core/auth/user-route-access.service';
import {GammeProduit, IGammeProduit} from '../../shared/model/gamme-produit.model';
import {GammeProduitComponent} from './gamme-produit.component';
import {GammeProduitService} from './gamme-produit.service';

@Injectable({providedIn: 'root'})
export class SalesResolve implements Resolve<IGammeProduit> {
  constructor(private service: GammeProduitService, private router: Router) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<IGammeProduit> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((sales: HttpResponse<GammeProduit>) => {
          if (sales.body) {
            return of(sales.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new GammeProduit());
  }
}

export const gammeProduitRoute: Routes = [
  {
    path: '',
    component: GammeProduitComponent,
    data: {
      authorities: [Authority.USER],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.gammeProduit.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
