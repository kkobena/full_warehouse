import { Injectable, signal, WritableSignal } from '@angular/core';
import { ICommande } from '../../shared/model/commande.model';
import { CommandeId } from '../../shared/model/abstract-commande.model';

/**
 * Source active dans les onglets de suggestions (v12).
 * - REAPPRO              → suggestions GENEREE (paniers à valider)
 * - COMMANDES_A_PASSER   → suggestions VALIDEE (en attente de commande)
 * - BONS_DE_LIVRAISON    → historique des bons de livraison (réception)
 * - ANALYSE              → vue analytique SEMOIS (lecture seule)
 */
export type SuggestionsSource = 'REAPPRO' | 'COMMANDES_A_PASSER' | 'BONS_DE_LIVRAISON' | 'ANALYSE';

@Injectable({
  providedIn: 'root',
})
export class CommandCommonService {
  currentCommand: WritableSignal<ICommande> = signal<ICommande>(null);
  commandPreviousActiveNav: WritableSignal<string> = signal<string>('DASHBOARD');
  /** Source active dans le composant suggestions-unified */
  suggestionsActiveSource: WritableSignal<SuggestionsSource> = signal<SuggestionsSource>('REAPPRO');

  /** Commande à ouvrir en mode édition après navigation vers "Commandes à passer" */
  pendingOpenCommandeId = signal<CommandeId | null>(null);

  /** Bon de livraison (commandeId) à ouvrir en mode édition après navigation vers "Bons de livraison" */
  pendingOpenDeliveryId = signal<CommandeId | null>(null);

  /** Déclenche l'ouverture du formulaire de nouvelle commande après navigation vers "Commandes à passer" */
  pendingNewCommande = signal<boolean>(false);

  constructor() {}



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

  /** Navigue vers l'onglet Bons de livraison (réceptions en attente) */
  navigateToBonsLivraison(): void {
    this.suggestionsActiveSource.set('BONS_DE_LIVRAISON');
    this.commandPreviousActiveNav.set('SUGGESTIONS');
  }


}
