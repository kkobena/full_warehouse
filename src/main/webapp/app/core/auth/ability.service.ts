import { computed, Injectable, Signal, signal } from '@angular/core';
import { IAbility, INavNode, NavAbilityAction } from 'app/shared/model/nav-item.model';

/**
 * Service de contrôle d'accès fin (CASL-like).
 * Alimenté par l'arbre de navigation dynamique (NavStore).
 *
 * Usage :
 *   ability.can('create', 'commande')       → boolean
 *   ability.canSignal('export', 'facturation') → Signal<boolean>
 */
@Injectable({ providedIn: 'root' })
export class AbilityService {
  private readonly abilities = signal<IAbility[]>([]);

  /** Initialise les abilities depuis l'arbre de navigation. */
  setFromNavTree(tree: INavNode[]): void {
    const flat = this.flatten(tree);
    const abilities: IAbility[] = [];

    for (const node of flat) {
      const p = node.permissions;
      if (!p) continue;
      const subject = node.code;
      if (p.canDisplay) abilities.push({ action: 'display', subject });
      if (p.canAccess) abilities.push({ action: 'access', subject });
      if (p.canCreate) abilities.push({ action: 'create', subject });
      if (p.canEdit) abilities.push({ action: 'edit', subject });
      if (p.canDelete) abilities.push({ action: 'delete', subject });
      if (p.canExport) abilities.push({ action: 'export', subject });
      if (p.canExecute) abilities.push({ action: 'execute', subject });
    }

    this.abilities.set(abilities);
  }

  /** Réinitialise les abilities (lors de la déconnexion). */
  reset(): void {
    this.abilities.set([]);
  }

  /** Vérifie si l'utilisateur peut effectuer une action sur un sujet. */
  can(action: NavAbilityAction, subject: string): boolean {
    return this.abilities().some(a => a.subject === subject && a.action === action);
  }

  /** Retourne un Signal réactif pour une vérification de permission. */
  canSignal(action: NavAbilityAction, subject: string): Signal<boolean> {
    return computed(() => this.abilities().some(a => a.subject === subject && a.action === action));
  }

  /** Vérifie si au moins une des abilities correspond. */
  canAny(checks: Array<{ action: NavAbilityAction; subject: string }>): boolean {
    return checks.some(c => this.can(c.action, c.subject));
  }

  private flatten(nodes: INavNode[]): INavNode[] {
    return nodes.flatMap(n => [n, ...this.flatten(n.children ?? [])]);
  }
}

