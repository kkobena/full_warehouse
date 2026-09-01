import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, of, shareReplay } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { CaptureEcran } from './cahier-recette.model';

/** Une capture telle que servie au guide affiché : le rang de l'étape et le chemin de l'image. */
export type CaptureServie = Pick<CaptureEcran, 'ordre' | 'fichier'>;

/** Captures indexées par identifiant de scénario, telles que produites à la génération. */
export type IndexCaptures = Record<string, CaptureServie[]>;

/** Chemin d'accès complet d'une entrée de menu, indexé par son code `nav_item`. */
export type CheminsMenu = Record<string, string>;

@Injectable({ providedIn: 'root' })
export class CahierRecetteService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/cahier-recette';

  /**
   * Index des écrans, écrit par `generate-cahier-recette-json.ts` à côté des images.
   *
   * Il n'est pas servi par une API : ce sont des actifs statiques, versionnés avec le build du
   * front. Son absence — aucune campagne de captures n'a tourné — est un cas normal, pas une
   * erreur : le guide reste alors purement textuel.
   */
  private readonly captures$ = this.http.get<IndexCaptures>('content/captures/index.json').pipe(
    catchError(() => of({} as IndexCaptures)),
    shareReplay({ bufferSize: 1, refCount: false }),
  );

  loadCaptures(): Observable<IndexCaptures> {
    return this.captures$;
  }

  /**
   * Chemin d'accès complet de chaque entrée de menu, indexé par code
   * (« ventes.devis » → « Barre de navigation ▸ Gestion Courante ▸ Ventes ▸ Proformas »).
   *
   * Le guide ne mémorise que le code : les libellés se renomment depuis l'administration et
   * les entrées se déplacent, un chemin recopié dans le modèle deviendrait faux en silence.
   *
   * Une erreur réseau rend un index vide plutôt que de faire échouer l'écran : le guide perd
   * ses indications « Où le trouver », il ne perd pas sa documentation.
   */
  private readonly navPaths$ = this.http.get<CheminsMenu>(`${SERVER_API_URL}api/nav/paths`).pipe(
    catchError(() => of({} as CheminsMenu)),
    shareReplay({ bufferSize: 1, refCount: false }),
  );

  loadNavPaths(): Observable<CheminsMenu> {
    return this.navPaths$;
  }

  downloadPdf(moduleIds: readonly string[] = [], scenarioIds: readonly string[] = []): Observable<Blob> {
    let params = new HttpParams();
    if (moduleIds.length > 0) {
      params = params.set('modules', moduleIds.join(','));
    }
    if (scenarioIds.length > 0) {
      params = params.set('scenarios', scenarioIds.join(','));
    }
    return this.http.get(`${this.resourceUrl}/pdf`, { params, responseType: 'blob' });
  }
}
