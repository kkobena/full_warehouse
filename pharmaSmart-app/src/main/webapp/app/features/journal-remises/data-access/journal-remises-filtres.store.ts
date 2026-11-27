import { computed, Injectable, signal } from '@angular/core';
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { NGB_DATE_TO_ISO } from 'app/shared/util/warehouse-util';
import { IFiltreJournal, NatureVente, OrigineRemise } from '../models/journal-remises.model';

/**
 * La période et les filtres du journal, partagés par ses onglets : passer de la synthèse aux UG garde
 * la période choisie. Fourni par l'écran qui porte les onglets (sous-menu ou module Promotions), pas
 * à la racine : deux écrans ouverts l'un après l'autre ne se transmettent pas leurs filtres.
 *
 * <p>Ce qui est saisi ne s'applique qu'à « Afficher » ({@link appliquer}) : les onglets lisent
 * {@link demande}, jamais la saisie en cours.
 */
@Injectable()
export class JournalRemisesFiltresStore {
  readonly du = signal<NgbDateStruct>(debutDuMois());
  readonly au = signal<NgbDateStruct>(aujourdHui());
  /** Nulle : toutes les natures de vente. */
  readonly natureVente = signal<NatureVente | null>(null);
  /** Nulle : toutes les origines. Ne filtre que les journaux des promotions et des remises. */
  readonly origine = signal<OrigineRemise | null>(null);

  /** Le filtre appliqué : les onglets se rechargent quand il change. */
  readonly demande = computed(() => this.applique());

  private readonly saisie = computed<IFiltreJournal>(() => {
    const nature = this.natureVente();
    const origine = this.origine();
    return {
      du: NGB_DATE_TO_ISO(this.du()) ?? '',
      au: NGB_DATE_TO_ISO(this.au()) ?? '',
      naturesVente: nature ? [nature] : [],
      origines: origine ? [origine] : [],
    };
  });

  private readonly applique = signal<IFiltreJournal>(this.saisie());

  appliquer(): void {
    this.applique.set(this.saisie());
  }
}

function aujourdHui(): NgbDateStruct {
  const maintenant = new Date();
  return { year: maintenant.getFullYear(), month: maintenant.getMonth() + 1, day: maintenant.getDate() };
}

/** Le mois en cours : l'écran s'ouvre sur ce qui a déjà été accordé ce mois-ci. */
function debutDuMois(): NgbDateStruct {
  const maintenant = new Date();
  return { year: maintenant.getFullYear(), month: maintenant.getMonth() + 1, day: 1 };
}
