import { Injectable, signal } from '@angular/core';

export interface TabCrumb {
  label: string;
  url?: string;
}

/**
 * Service permettant aux composants à onglets (sales-home, commande, facturation…)
 * de pousser dynamiquement un crumb supplémentaire dans le fil d'Ariane.
 *
 * Usage dans un composant :
 *   inject(BreadcrumbService).setTabCrumb('Suggestions de réappro');
 *
 *   // Nettoyer au destroy (ou utiliser takeUntilDestroyed)
 *   inject(BreadcrumbService).clearTabCrumb();
 */
@Injectable({ providedIn: 'root' })
export class BreadcrumbService {
  private readonly _tabCrumb = signal<TabCrumb | null>(null);

  /** Signal en lecture seule : onglet actif courant */
  readonly tabCrumb = this._tabCrumb.asReadonly();

  /** Définir l'onglet actif (appelé par le composant hôte) */
  setTabCrumb(label: string, url?: string): void {
    this._tabCrumb.set({ label, url });
  }

  /** Effacer l'onglet actif (appelé à la destruction du composant) */
  clearTabCrumb(): void {
    this._tabCrumb.set(null);
  }
}

