import { Signal } from '@angular/core';

/** Colonne décrite en TypeScript, pour les tableaux qui n'écrivent pas leur `<thead>` à la main. */
export interface AppTableColumn<T = Record<string, unknown>> {
  field: keyof T & string;
  header: string;
  sortable?: boolean;
  width?: string;
  align?: 'left' | 'center' | 'right';
}

/**
 * Charge utile de `(onLazyLoad)` — volontairement identique à celle de `p-table`,
 * pour que les ~41 gestionnaires existants n'aient pas à être réécrits.
 */
export interface AppTableLazyLoadEvent {
  first: number;
  rows: number;
  sortField?: string;
  sortOrder?: 1 | -1;
  filters?: Record<string, unknown>;
}

/**
 * Contrat exposé par la table à ses directives compagnons (`[appSortableHeader]`,
 * `[appRowToggler]`).
 *
 * Ces directives sont posées dans des templates **projetés** (`#header`, `#body`) : elles
 * ne peuvent donc pas importer la table sans créer un cycle. Le jeton abstrait rompt ce
 * cycle — la table le fournit via `useExisting`, les directives ne connaissent que lui.
 */
export abstract class AppTableHost {
  abstract readonly sortField: Signal<string>;
  abstract readonly sortOrder: Signal<1 | -1>;
  abstract toggleSort(field: string): void;
  abstract isExpanded(row: unknown): boolean;
  abstract toggleRow(row: unknown): void;
  abstract isSelected(row: unknown): boolean;
  abstract selectRow(row: unknown, event: Event): void;

  /** Bascule la sélection d'une ligne sans passer par un clic sur celle-ci. */
  abstract toggleSelection(row: unknown): void;

  /** Vrai si toutes les lignes de la page courante sont sélectionnées. */
  abstract isAllSelected(): boolean;

  /**
   * Vrai si une partie seulement des lignes est sélectionnée — alimente l'état
   * indéterminé de la case d'en-tête.
   */
  abstract isPartiallySelected(): boolean;

  /** Sélectionne ou désélectionne toutes les lignes de la page courante. */
  abstract toggleAll(): void;
}
