import { Injectable, inject } from '@angular/core';
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

import { EventManager, EventWithContent } from 'app/core/util/event-manager.service';
import { EXPECTED_ERROR_STATUSES } from './expected-error-statuses';

@Injectable()
export class ErrorHandlerInterceptor implements HttpInterceptor {
  private readonly eventManager = inject(EventManager);

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      tap({
        error: (err: HttpErrorResponse) => {
          if (request.context.get(EXPECTED_ERROR_STATUSES).includes(err.status)) {
            return;
          }
          if (!(err.status === 401 && (err.message === '' || err.url.includes('api/account')))) {
            this.eventManager.broadcast(new EventWithContent('warehouseApp.httpError', err));
          }
        },
      }),
    );
  }
}
