import { HttpClient, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from '../../../app.constants';
import { FormatExport, IFiltreJournal, ILigneJournal, ISyntheseJournal, NatureJournal, parametres } from '../models/journal-remises.model';

/** Le journal des remises accordées et des UG vendues, dans le magasin de l'utilisateur. */
@Injectable({ providedIn: 'root' })
export class JournalRemisesApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/journal-remises';
  private readonly http = inject(HttpClient);

  synthese(filtre: IFiltreJournal): Observable<ISyntheseJournal> {
    return this.http.get<ISyntheseJournal>(`${this.resourceUrl}/synthese`, { params: parametres(filtre) });
  }

  /** Une page du journal ; le total des lignes est dans l'en-tête `X-Total-Count`. */
  lignes(nature: NatureJournal, filtre: IFiltreJournal, page: number, size: number): Observable<HttpResponse<ILigneJournal[]>> {
    return this.http.get<ILigneJournal[]>(`${this.resourceUrl}/lignes`, {
      params: { ...parametres(filtre), nature, page, size },
      observe: 'response',
    });
  }

  /** Sans nature : la synthèse et les trois journaux. */
  exporter(format: FormatExport, nature: NatureJournal | null, filtre: IFiltreJournal): Observable<Blob> {
    const params: Record<string, string | string[]> = { ...parametres(filtre), format };
    if (nature) {
      params['nature'] = nature;
    }
    return this.http.get(`${this.resourceUrl}/export`, { params, responseType: 'blob' });
  }
}
