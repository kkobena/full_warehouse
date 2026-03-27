import { Injectable, signal, WritableSignal } from '@angular/core';
import { ICommande } from '../../shared/model/commande.model';

/**
 * Source active dans les onglets de suggestions (v12).
 * - REAPPRO   → suggestions GENEREE (paniers à valider)
 * - COMMANDES_A_PASSER → suggestions VALIDEE (en attente de commande)
 * - ANALYSE   → vue analytique SEMOIS (lecture seule)
 */
export type SuggestionsSource = 'REAPPRO' | 'COMMANDES_A_PASSER' | 'ANALYSE';

@Injectable({
  providedIn: 'root',
})
export class CommandCommonService {
  currentCommand: WritableSignal<ICommande> = signal<ICommande>(null);
  commandPreviousActiveNav: WritableSignal<string> = signal<string>('DASHBOARD');
  /** Source active dans le composant suggestions-unified */
  suggestionsActiveSource: WritableSignal<SuggestionsSource> = signal<SuggestionsSource>('REAPPRO');

  constructor() {}

  updateCommand(commande: ICommande): void {
    this.currentCommand.set(commande);
  }

  updateCommandPreviousActiveNav(nav: string): void {
    this.commandPreviousActiveNav.set(nav);
  }

  /** Navigue vers l'onglet Réapprovisionnement (suggestions GENEREE) */
  navigateToReappro(): void {
    this.suggestionsActiveSource.set('REAPPRO');
    this.commandPreviousActiveNav.set('SUGGESTIONS');
  }

  /** Navigue vers l'onglet Commandes à passer (suggestions VALIDEE) */
  navigateToCommandesAPasser(): void {
    this.suggestionsActiveSource.set('COMMANDES_A_PASSER');
    this.commandPreviousActiveNav.set('SUGGESTIONS');
  }

  /** Navigue vers l'onglet Analyse des stocks (SEMOIS analytique) */
  navigateToAnalyse(): void {
    this.suggestionsActiveSource.set('ANALYSE');
    this.commandPreviousActiveNav.set('SUGGESTIONS');
  }

  /** @deprecated Utiliser navigateToAnalyse() */
  navigateToSemoisSuggestions(): void {
    this.navigateToAnalyse();
  }

  /** @deprecated Utiliser navigateToReappro() */
  navigateToFournisseursSuggestions(): void {
    this.navigateToReappro();
  }
}
