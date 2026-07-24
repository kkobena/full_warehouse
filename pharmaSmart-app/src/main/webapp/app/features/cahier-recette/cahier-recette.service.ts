import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';

@Injectable({ providedIn: 'root' })
export class CahierRecetteService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/cahier-recette';

  downloadPdf(): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/pdf`, { responseType: 'blob' });
  }
}
