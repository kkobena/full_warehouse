import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { SuggestionService } from './suggestion.service';
import { Suggestion } from './model/suggestion.model';

const SuggestionResolver = (route: ActivatedRouteSnapshot): Observable<null | Suggestion> => {
  const id = route.params['id'];
  if (id) {
    return inject(SuggestionService)
      .find(id)
      .pipe(
        mergeMap((res: HttpResponse<Suggestion>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(new Suggestion());
};

export default SuggestionResolver;
