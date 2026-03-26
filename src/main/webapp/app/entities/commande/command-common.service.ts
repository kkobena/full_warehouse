import { Injectable, signal, WritableSignal } from '@angular/core';
import { ICommande } from '../../shared/model/commande.model';

export type SuggestionsSource = 'FOURNISSEURS' | 'SEMOIS';

@Injectable({
  providedIn: 'root',
})
export class CommandCommonService {
  currentCommand: WritableSignal<ICommande> = signal<ICommande>(null);
  commandPreviousActiveNav: WritableSignal<string> = signal<string>('DASHBOARD');
  /** Source active dans le composant suggestions-unified */
  suggestionsActiveSource: WritableSignal<SuggestionsSource> = signal<SuggestionsSource>('FOURNISSEURS');

  constructor() {}

  updateCommand(commande: ICommande): void {
    this.currentCommand.set(commande);
  }

  updateCommandPreviousActiveNav(nav: string): void {
    this.commandPreviousActiveNav.set(nav);
  }

  /** Navigue vers l'onglet Suggestions en forçant la source SEMOIS */
  navigateToSemoisSuggestions(): void {
    this.suggestionsActiveSource.set('SEMOIS');
    this.commandPreviousActiveNav.set('SUGGESTIONS');
  }

  /** Navigue vers l'onglet Suggestions avec la source fournisseurs */
  navigateToFournisseursSuggestions(): void {
    this.suggestionsActiveSource.set('FOURNISSEURS');
    this.commandPreviousActiveNav.set('SUGGESTIONS');
  }
}
