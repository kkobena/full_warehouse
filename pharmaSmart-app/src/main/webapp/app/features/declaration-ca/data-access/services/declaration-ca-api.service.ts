import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApplicationConfigService } from 'app/core/config/application-config.service';

/** Un rayon ou un tiers-payant, candidat à l'exclusion du CA à déclarer. */
export interface ExclusionItem {
  id: number;
  code: string | null;
  libelle: string;
  exclu: boolean;
}

/** Les deux référentiels partagent le même écran : le type ne change que l'URL appelée. */
export type ReferentielExclusion = 'rayons' | 'tiers-payants';

/** Ce que le serveur attend pour découper et filtrer une page du référentiel. */
export interface ExclusionPageRequest {
  page: number;
  size: number;
  /** `null` ou absent pour ne pas filtrer sur l'état d'exclusion. */
  exclus?: boolean | null;
  /** Recherche sur le libellé ou le code. */
  search?: string;
}

@Injectable({ providedIn: 'root' })
export class DeclarationCaApiService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(ApplicationConfigService);

  private readonly base = this.config.getEndpointFor('api/declaration-ca');

  /**
   * Une page du référentiel. Filtre et recherche partent au serveur : la page reçue est déjà
   * la bonne, et l'en-tête `X-Total-Count` compte l'ensemble filtré, pas ce qui est affiché.
   */
  lister(referentiel: ReferentielExclusion, requete: ExclusionPageRequest): Observable<HttpResponse<ExclusionItem[]>> {
    let params = new HttpParams().set('page', requete.page).set('size', requete.size);
    if (requete.exclus !== undefined && requete.exclus !== null) {
      params = params.set('exclus', requete.exclus);
    }
    if (requete.search) {
      params = params.set('search', requete.search);
    }
    return this.http.get<ExclusionItem[]>(`${this.base}/${referentiel}`, { params, observe: 'response' });
  }

  /**
   * Envoie l'état cible et non une bascule : rejouer la requête donne le même résultat, ce qu'un
   * double-clic ou une reprise réseau rendent parfaitement possible.
   */
  majExclusion(
    referentiel: ReferentielExclusion,
    ids: number[],
    exclure: boolean,
  ): Observable<{ modifies: number }> {
    return this.http.put<{ modifies: number }>(`${this.base}/${referentiel}/exclusion`, { ids, exclure });
  }

  lireParametres(): Observable<{ excludeFreeUnit: boolean }> {
    return this.http.get<{ excludeFreeUnit: boolean }>(`${this.base}/parametres`);
  }

  majParametres(excludeFreeUnit: boolean): Observable<{ excludeFreeUnit: boolean }> {
    return this.http.put<{ excludeFreeUnit: boolean }>(`${this.base}/parametres`, { excludeFreeUnit });
  }

  // ===== Ponction =====

  /** Les valeurs d'officine affichées avant toute saisie. */
  parametresPonction(): Observable<PonctionParametres> {
    return this.http.get<PonctionParametres>(`${this.base}/ponctions/parametres`);
  }

  /** L'assiette d'une période, sans objectif : ce qu'on peut prélever avant de décider combien. */
  assietteePonction(dateDebut: string, dateFin: string, plafondParVente?: number | null): Observable<PonctionAssiette> {
    let params = new HttpParams().set('dateDebut', dateDebut).set('dateFin', dateFin);
    if (plafondParVente !== null && plafondParVente !== undefined) {
      params = params.set('plafondParVente', plafondParVente);
    }
    return this.http.get<PonctionAssiette>(`${this.base}/ponctions/assiette`, { params });
  }

  simuler(param: PonctionParam): Observable<PonctionSimulation> {
    return this.http.post<PonctionSimulation>(`${this.base}/ponctions/simulation`, param);
  }

  validerPonction(param: PonctionParam): Observable<Ponction> {
    return this.http.post<Ponction>(`${this.base}/ponctions`, param);
  }

  annulerPonction(id: number): Observable<Ponction> {
    return this.http.delete<Ponction>(`${this.base}/ponctions/${id}`);
  }

  /**
   * L'historique, éventuellement borné à une période. Les deux bornes vont de pair : le serveur
   * ignore un filtre à moitié renseigné plutôt que d'ouvrir un côté au hasard.
   */
  historiquePonctions(dateDebut?: string, dateFin?: string): Observable<Ponction[]> {
    let params = new HttpParams();
    if (dateDebut && dateFin) {
      params = params.set('dateDebut', dateDebut).set('dateFin', dateFin);
    }
    return this.http.get<Ponction[]>(`${this.base}/ponctions`, { params });
  }

  detailPonction(id: number): Observable<PonctionLigne[]> {
    return this.http.get<PonctionLigne[]>(`${this.base}/ponctions/${id}/detail`);
  }

  justificatifPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/ponctions/${id}/pdf`, { responseType: 'blob' });
  }

  // ===== Journaux d'exclusion =====

  /**
   * Le contenu d'un journal : les indicateurs et les lignes, en un seul appel.
   *
   * <p>Un appel unique et non deux : le bandeau et le tableau décrivent le même ensemble, et deux
   * requêtes distinctes pourraient les calculer sur des instants différents.
   */
  journal(type: TypeJournal, filtres: JournalFiltres): Observable<JournalExclusion> {
    let params = new HttpParams().set('dateDebut', filtres.dateDebut).set('dateFin', filtres.dateFin);
    if (filtres.recherche) {
      params = params.set('recherche', filtres.recherche);
    }
    if (filtres.tiersPayantId != null) {
      params = params.set('tiersPayantId', filtres.tiersPayantId);
    }
    return this.http.get<JournalExclusion>(`${this.base}/journaux/${type}`, { params });
  }

  /** Le détail d'une vente tiers-payant exclue. La date fait partie de la clé : `sales` est partitionnée. */
  lignesDeLaVente(saleId: number, saleDate: string): Observable<JournalLigne[]> {
    const params = new HttpParams().set('saleId', saleId).set('saleDate', saleDate);
    return this.http.get<JournalLigne[]>(`${this.base}/journaux/tiers-payants/lignes`, { params });
  }

  // ===== Audit =====

  auditer(fromDate?: string, toDate?: string): Observable<Anomalie[]> {
    let params = new HttpParams();
    if (fromDate && toDate) {
      params = params.set('fromDate', fromDate).set('toDate', toDate);
    }
    return this.http.get<Anomalie[]>(`${this.base}/audit`, { params });
  }
}


/** Comment l'objectif de ponction est exprimé. */
export type ModeCalculPonction = 'MONTANT_FIXE' | 'POURCENTAGE';

export type StatutPonction = 'SIMULATION' | 'VALIDEE' | 'ANNULEE';

export interface PonctionParametres {
  /** Plafond appliqué faute de surcharge, en pourcentage du montant d'une vente. */
  plafondDefaut: number;
  delaiAnnulationJours: number;
}

export interface PonctionParam {
  dateDebut: string;
  dateFin: string;
  modeCalcul: ModeCalculPonction;
  valeur: number;
  plafondParVente?: number;
  commentaire?: string;
}

/** Ce qui est prélevable sur une période, avant toute saisie d'objectif. */
export interface PonctionAssiette {
  dateDebut: string;
  dateFin: string;
  caReel: number;
  /** Encaissé en espèces sur les ventes éligibles — ce que la ponction peut mordre. */
  caEspece: number;
  caAssietteTva0: number;
  montantPonctionnable: number;
  nombreVentesEligibles: number;
  plafondApplique: number;
}

export interface PonctionLigne {
  saleId: number;
  saleDate: string;
  numeroTransaction: string;
  montantVente: number;
  montantBase: number;
  montantPonctionne: number;
  rang: number;
}

export interface PonctionSimulation {
  dateDebut: string;
  dateFin: string;
  caReel: number;
  caApresExclusions: number;
  caAssietteTva0: number;
  montantObjectif: number;
  montantPonctionnable: number;
  montantPonctionne: number;
  caDeclare: number;
  nombreVentesEligibles: number;
  nombreVentesImpactees: number;
  tauxMoyenApplique: number;
  tauxMaxApplique: number;
  objectifAtteignable: boolean;
  /** Plafond par vente réellement retenu par le serveur : celui saisi, ou celui de l'officine. */
  plafondApplique: number;
  delaiAnnulationJours: number;
  apercu: PonctionLigne[];
  avertissements: string[];
}

export interface Ponction {
  id: number;
  dateDebut: string;
  dateFin: string;
  modeCalcul: ModeCalculPonction;
  valeurSaisie: number;
  plafondParVente: number;
  caReel: number;
  caApresExclusions: number;
  caDeclare: number;
  montantObjectif: number;
  montantPonctionne: number;
  nombreVentes: number;
  statut: StatutPonction;
  commentaire: string | null;
  auteur: string;
  creeLe: string;
  valideLe: string | null;
  annuleLe: string | null;
}

/** Les trois journaux, un par mécanisme d'exclusion. Le type ne change que l'URL appelée. */
export type TypeJournal = 'unites-gratuites' | 'rayons' | 'tiers-payants';

export interface JournalFiltres {
  dateDebut: string;
  dateFin: string;
  recherche?: string;
  /** Ventes tiers-payant uniquement. */
  tiersPayantId?: number | null;
}

/** Une ligne de vente sortie, en tout ou partie, du CA à déclarer. */
export interface JournalLigne {
  saleId: number;
  saleDate: string;
  numeroTransaction: string;
  codeProduit: string | null;
  libelleProduit: string;
  /** Renseigné pour le journal des rayons exclus. */
  rayon: string | null;
  quantite: number;
  quantiteUg: number;
  valeurTtc: number;
  montantExclu: number;
  marge: number;
}

/** Une vente tiers-payant exclue — le « maître » de l'écran tiers-payant. */
export interface JournalVente {
  saleId: number;
  saleDate: string;
  numeroTransaction: string;
  tiersPayants: string | null;
  client: string | null;
  valeurTtc: number;
  montantExclu: number;
  marge: number;
  nombreLignes: number;
}

/**
 * Les indicateurs du bandeau, calculés sur la totalité de la période et non sur les lignes
 * renvoyées : celles-ci sont plafonnées.
 */
export interface JournalKpi {
  nombreVentes: number;
  nombreLignes: number;
  quantite: number;
  quantiteUg: number;
  valeurTtc: number;
  montantExclu: number;
  marge: number;
  tauxMarge: number;
}

export interface JournalExclusion {
  kpi: JournalKpi;
  ventes: JournalVente[];
  lignes: JournalLigne[];
  /** Le plafond de lignes a été atteint ; les indicateurs, eux, restent complets. */
  tronque: boolean;
}

/** Résultat du contrôle d'un invariant du chiffre d'affaires déclaré. */
export interface Anomalie {
  code: string;
  libelle: string;
  consequence: string;
  nombreAnomalies: number;
  exemples: string[];
}
