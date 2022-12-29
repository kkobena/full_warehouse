import {Injectable} from '@angular/core';
import {HttpResponse} from '@angular/common/http';
import {ActivatedRouteSnapshot, Resolve, Router, Routes} from '@angular/router';
import {EMPTY, Observable, of} from 'rxjs';
import {flatMap} from 'rxjs/operators';

import {Authority} from 'app/shared/constants/authority.constants';
import {UserRouteAccessService} from 'app/core/auth/user-route-access.service';
import {Decondition, IDecondition} from 'app/shared/model/decondition.model';
import {DeconditionService} from './decondition.service';
import {DeconditionComponent} from './decondition.component';

@Injectable({providedIn: 'root'})
export class DeconditionResolve implements Resolve<IDecondition> {
  constructor(private service: DeconditionService, private router: Router) {
  }

  resolve(route: ActivatedRouteSnapshot): Observable<IDecondition> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((decondition: HttpResponse<Decondition>) => {
          if (decondition.body) {
            return of(decondition.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new Decondition());
  }
}

export const deconditionRoute: Routes = [
  {
    path: '',
    component: DeconditionComponent,
    data: {
      authorities: [Authority.USER],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.decondition.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
