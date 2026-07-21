import { Component, computed, effect, inject, input, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { NgbNavModule } from "@ng-bootstrap/ng-bootstrap";
import { IAuthority, INavRole, NavApiService } from "app/core/data-access/nav-api.service";
import { NotificationService } from "app/shared/services/notification.service";
import { INavNode, NavItemAssignment } from "app/shared/model/nav-item.model";
import { NavReorderComponent } from "./nav-reorder.component";
import { CheckboxComponent, DataTableComponent, SelectSearchComponent } from "app/shared/ui";

interface FlatNavNode extends INavNode {
  depth: number;
  assignment: NavItemAssignment;
}


@Component({
  selector: "app-nav-manager",
  templateUrl: "./nav-manager.component.html",
  styleUrl: "./nav-manager.component.scss",
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule, FormsModule,
    DataTableComponent, CheckboxComponent, SelectSearchComponent,
    NgbNavModule, NavReorderComponent
  ]
})
export class NavManagerComponent implements OnInit {
  private readonly navApi = inject(NavApiService);
  private readonly notificationService = inject(NotificationService);

  /** Rôle pré-sélectionné par le composant parent (ex: depuis la liste des rôles). */
  readonly triggerRole = input<string | null>(null);

  protected activeTab = signal<string>("permissions");
  protected readonly loadingRoles = signal(false);
  protected readonly loading = signal(false);
  protected readonly flatItems = signal<FlatNavNode[]>([]);
  /** Groupes repliés (Set des IDs) */
  protected readonly collapsedGroups = signal<Set<number>>(new Set());
  /** Liste visible dans le tableau (mise à jour explicite pour fiabilité avec p-table) */
  protected readonly visibleItems = signal<FlatNavNode[]>([]);
  /** Lignes en cours de sauvegarde (Set des IDs) */
  protected readonly savingRows = signal<Set<number>>(new Set());
  /** ID de l'item dont on édite le libellé */
  protected readonly editingItemId = signal<number | null>(null);
  protected editingLibelle = "";
  /** Terme de recherche — filtre par libellé, bypasse le collapse */
  protected readonly searchTerm = signal("");
  /** Arbre brut conservé pour la prévisualisation */
  private readonly rawTree = signal<INavNode[]>([]);
  protected readonly availableRoles = signal<IAuthority[]>([]);
  protected selectedRole: IAuthority | null = null;
  //La fonctionnalité pas totalement OK
  protected showTabReorder=false;

  /**
   * Arbre de prévisualisation : reflète en temps réel les cases cochées
   * dans la matrice des permissions. Exclut les items de type ACTION.
   */
  protected readonly previewTree = computed<INavNode[]>(() => {
    const assignments = new Map(
      this.flatItems().map(item => [item.id, item.assignment])
    );
    return this.filterTreeForPreview(this.rawTree(), assignments);
  });

  constructor() {
    // Quand le rôle déclenché ET la liste des rôles sont disponibles → auto-sélection
    effect(() => {
      const roleName = this.triggerRole();
      const roles = this.availableRoles();
      if (!roleName || roles.length === 0) return;
      const found = roles.find(r => r.name === roleName);
      if (found && found.name !== this.selectedRole?.name) {
        this.selectedRole = found;
        this.loadRoleItems();
      }
    });
  }

  ngOnInit(): void {
    this.loadRoles();
  }

  onTabChange(tabId: string): void {
    this.activeTab.set(tabId);
  }

  private loadRoles(): void {
    this.loadingRoles.set(true);
    this.navApi.getAllRoles().subscribe({
      next: roles => {
        this.availableRoles.set(roles.filter(r => r.name !== "ROLE_SUPER_USER"));
        this.loadingRoles.set(false);
      },
      error: () => {
        this.notificationService.error("Impossible de charger la liste des rôles.");
        this.loadingRoles.set(false);
      }
    });
  }

  toggleGroup(id: number): void {
    const next = new Set(this.collapsedGroups());
    next.has(id) ? next.delete(id) : next.add(id);
    this.collapsedGroups.set(next);
    this.refreshVisible();
  }

  onSearchChange(): void {
    this.refreshVisible();
  }

  clearSearch(): void {
    this.searchTerm.set("");
    this.refreshVisible();
  }

  private refreshVisible(): void {
    const term = this.searchTerm().trim().toLowerCase();
    if (term) {
      // Recherche active : on bypasse le collapse et on montre tous les matches
      this.visibleItems.set(
        this.flatItems().filter(item => item.libelle.toLowerCase().includes(term))
      );
    } else {
      this.visibleItems.set(this.applyCollapse(this.flatItems(), this.collapsedGroups()));
    }
  }

  /** Retourne l'item lui-même + tous ses descendants dans la liste plate. */
  private getSubtree(item: FlatNavNode): FlatNavNode[] {
    const items = this.flatItems();
    const idx = items.findIndex(i => i.id === item.id);
    if (idx === -1) return [];
    const result: FlatNavNode[] = [items[idx]];
    for (let i = idx + 1; i < items.length; i++) {
      if (items[i].depth <= item.depth) break;
      result.push(items[i]);
    }
    return result;
  }

  /** Accorde ou révoque toutes les permissions sur un menu et ses enfants, puis sauvegarde. */
  grantSubtree(item: FlatNavNode, grant: boolean): void {
    if (!this.selectedRole) return;
    const subtree = this.getSubtree(item);
    subtree.forEach(n => {
      n.assignment = grant
        ? { ...n.assignment, canDisplay: true,  canAccess: true,  canCreate: true,  canEdit: true,  canDelete: true,  canExport: true,  canExecute: true  }
        : { ...n.assignment, canDisplay: false, canAccess: false, canCreate: false, canEdit: false, canDelete: false, canExport: false, canExecute: false };
    });
    // Force la détection du changement sur les signaux
    this.flatItems.update(f => [...f]);
    this.refreshVisible();
    // Sauvegarde en séquence (chaque item est indépendant)
    subtree.forEach(n => this.onPermissionChange(n));
  }

  // ── Sauvegarde auto d'une permission ────────────────────────────────────
  onPermissionChange(item: FlatNavNode): void {
    if (!this.selectedRole) return;
    const rowId = item.id;
    this.savingRows.update(s => new Set(s).add(rowId));
    this.navApi.updateSinglePermission(this.selectedRole.name, item.assignment).subscribe({
      next: () => {
        this.savingRows.update(s => {
          const n = new Set(s);
          n.delete(rowId);
          return n;
        });
      },
      error: () => {
        this.savingRows.update(s => {
          const n = new Set(s);
          n.delete(rowId);
          return n;
        });
        this.notificationService.error("Échec de la mise à jour de la permission.");
      }
    });
  }

  // ── Édition inline du libellé ────────────────────────────────────────────
  startEdit(item: FlatNavNode): void {
    this.editingItemId.set(item.id);
    this.editingLibelle = item.libelle;
  }

  saveEdit(item: FlatNavNode): void {
    const libelle = this.editingLibelle.trim();
    if (!libelle || libelle === item.libelle) {
      this.cancelEdit();
      return;
    }
    this.navApi.updateNavItemLibelle(item.id, libelle).subscribe({
      next: () => {
        const patch = (list: FlatNavNode[]) =>
          list.map(i => i.id === item.id ? { ...i, libelle } : i);
        this.flatItems.update(patch);
        this.visibleItems.update(patch);
        this.cancelEdit();
        this.notificationService.success("Libellé mis à jour.");
      },
      error: () => this.notificationService.error("Échec de la mise à jour du libellé.")
    });
  }

  cancelEdit(): void {
    this.editingItemId.set(null);
    this.editingLibelle = "";
  }

  /** Indentation progressive : grand écart entre GROUP (depth=0) et ses enfants. */
  protected indentRem(depth: number): string {
    const rem = depth === 0 ? 0.5 : depth * 1.75 + 0.75;
    return `${rem}rem`;
  }

  private applyCollapse(items: FlatNavNode[], collapsed: Set<number>): FlatNavNode[] {
    const result: FlatNavNode[] = [];
    let skipBelowDepth: number | null = null;
    for (const item of items) {
      if (skipBelowDepth !== null) {
        if (item.depth > skipBelowDepth) continue;
        skipBelowDepth = null;
      }
      result.push(item);
      // GROUP et ROUTE peuvent être repliés (ROUTE pour cacher ses enfants SECTION/ACTION)
      if ((item.targetType === "GROUP" || item.targetType === "ROUTE") && collapsed.has(item.id)) {
        skipBelowDepth = item.depth;
      }
    }
    return result;
  }

  loadRoleItems(): void {
    if (!this.selectedRole) {
      this.flatItems.set([]);
      this.rawTree.set([]);
      this.visibleItems.set([]);
      this.searchTerm.set('');
      this.collapsedGroups.set(new Set());
      return;
    }
    this.loading.set(true);
    this.collapsedGroups.set(new Set());
    this.navApi.getAllNavItemsForRole(this.selectedRole.name).subscribe({
      next: items => {
        this.rawTree.set(items);
        const flat = this.flattenWithDepth(items, 0);
        this.flatItems.set(flat);
        this.searchTerm.set(""); // réinitialise la recherche au changement de rôle
        this.refreshVisible();
        this.loading.set(false);
      },
      error: () => {
        this.notificationService.error("Impossible de charger les items de navigation.");
        this.loading.set(false);
      }
    });
  }

  private flattenWithDepth(nodes: INavNode[], depth: number): FlatNavNode[] {
    return nodes.flatMap(node => [
      {
        ...node,
        depth,
        assignment: {
          navItemId: node.id,
          canDisplay: node.permissions?.canDisplay ?? true,
          canAccess: node.permissions?.canAccess ?? true,
          canCreate: node.permissions?.canCreate ?? false,
          canEdit: node.permissions?.canEdit ?? false,
          canDelete: node.permissions?.canDelete ?? false,
          canExport: node.permissions?.canExport ?? false,
          canExecute: node.permissions?.canExecute ?? false
        }
      },
      ...this.flattenWithDepth(node.children ?? [], depth + 1)
    ]);
  }

  /** Filtre récursivement l'arbre selon les assignments courants (pour la prévisualisation).
   *  ACTION et SECTION sont exclus : ils n'apparaissent pas dans la top navbar. */
  private filterTreeForPreview(
    nodes: INavNode[],
    assignments: Map<number, NavItemAssignment>
  ): INavNode[] {
    const result: INavNode[] = [];
    for (const node of nodes) {
      if (node.targetType === "ACTION" || node.targetType === "SECTION") continue;
      const a = assignments.get(node.id);
      if (a && !a.canDisplay) continue;
      const children = this.filterTreeForPreview(node.children ?? [], assignments);
      if (node.targetType === "GROUP" && children.length === 0) continue;
      result.push({ ...node, children });
    }
    return result;
  }

  /** Compte récursivement tous les enfants (pour le résumé de prévisualisation). */
  protected countAllChildren(nodes: INavNode[]): number {
    return nodes.reduce((sum, n) => sum + (n.children?.length ?? 0) + this.countAllChildren(n.children ?? []), 0);
  }
}
