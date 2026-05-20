import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { IBfrEvolution, IBfrSnapshot } from 'app/shared/model/report';

@Injectable({ providedIn: 'root' })
export class CashFlowBfrService {
  private readonly resourceUrl = SERVER_API_URL + 'api/cash-flow-bfr';
  private readonly http = inject(HttpClient);

  getSnapshot(): Observable<HttpResponse<IBfrSnapshot>> {
    return this.http.get<IBfrSnapshot>(`${this.resourceUrl}/snapshot`, { observe: 'response' });
  }

  getEvolution(): Observable<HttpResponse<IBfrEvolution>> {
    return this.http.get<IBfrEvolution>(`${this.resourceUrl}/evolution`, { observe: 'response' });
  }
}
