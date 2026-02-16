import { KeyboardShortcut } from '../../entities/sales/selling-home/racourci/keyboard-shortcuts.service';

/**
 * Interface commune pour fournir des raccourcis clavier au ShortcutsHelpDialogComponent.
 *
 * Permet de découpler le dialog d'aide du service legacy (SellingHomeShortcutsService)
 * tout en gardant la compatibilité avec celui-ci.
 *
 * Implémenté par :
 * - SellingHomeShortcutsService (legacy, implicitement compatible)
 * - createKeyboardShortcutsMixin (nouveau module features/sales)
 */
export interface ShortcutsProvider {
  getShortcutsByCategory(): Map<string, KeyboardShortcut[]>;
  isRunningInTauri(): boolean;
}
