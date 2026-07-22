import { Component, computed, effect, input, output, signal, ChangeDetectionStrategy } from "@angular/core";
import { injectTableRows } from "app/shared/utils";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import {
  NgbDropdown,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle,
  NgbTooltip,
} from "@ng-bootstrap/ng-bootstrap";
import { AppTableLazyLoadEvent, BadgeComponent, CheckboxComponent, DataTableComponent } from "app/shared/ui";
import { IProduit } from "app/shared/model/produit.model";
import { EtaProduitComponent } from "app/shared/eta-produit/eta-produit.component";

export type ProduitMenuAction =
  | "view"
  | "edit"
  | "print-label"
  | "commander"
  | "generiques"
  | "add-detail"
  | "decondition"
  | "prix-reference"
  | "saisir-lots"
  | "suspend"
  | "activate"
  | "archive"
  | "delete"
  | "clone-rayon";

interface MenuEntry {
  label: string;
  icon: string;
  action: ProduitMenuAction;
  disabled?: boolean;
  danger?: boolean;
  separatorBefore?: boolean;
}

@Component({
  selector: "app-produit-list",
  templateUrl: "./produit-list.component.html",
  styleUrls: ["./produit-list.component.scss"],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    DataTableComponent,
    BadgeComponent,
    CheckboxComponent,
    NgbDropdown,
    NgbDropdownToggle,
    NgbDropdownMenu,
    NgbDropdownItem,
    NgbTooltip,
    EtaProduitComponent,
  ]
})
export class ProduitListComponent {
  readonly produits = input.required<IProduit[]>();
  readonly totalItems = input<number>(0);
  /** Pass a positive number to override; 0 (default) = auto-compute from viewport. */
  readonly rows = input<number>(0);
  readonly loading = input<boolean>(false);
  readonly selectedProduit = input<IProduit | null>(null);
  readonly clearSelectionTrigger = input<number>(0);
  readonly canCreate = input<boolean>(true);
  readonly canEdit = input<boolean>(true);
  readonly canDelete = input<boolean>(true);

  readonly produitSelected = output<IProduit>();
  readonly lazyLoad = output<AppTableLazyLoadEvent>();
  readonly editRequested = output<IProduit>();
  readonly deleteRequested = output<IProduit>();
  readonly menuAction = output<{ action: ProduitMenuAction; produit: IProduit }>();
  readonly selectionChanged = output<IProduit[]>();

  private readonly autoRows = injectTableRows({ overhead: 220 });
  protected readonly effectiveRows = computed(() => {
    const r = this.rows();
    return r > 0 ? r : this.autoRows();
  });

  protected selectedIds = signal<Set<number>>(new Set());
  protected allSelected = computed(() => {
    const ids = this.selectedIds();
    const produits = this.produits();
    return produits.length > 0 && produits.every(p => ids.has(p.id!));
  });

  constructor() {
    effect(() => {
      // Reset selection when parent triggers it (page change, filter, explicit clear)
      void this.clearSelectionTrigger();
      this.selectedIds.set(new Set());
    });
  }

  protected onRowClick(produit: IProduit): void {
    this.produitSelected.emit(produit);
  }

  protected toggleCheckbox(produit: IProduit): void {
    const id = produit.id!;
    this.selectedIds.update(ids => {
      const next = new Set(ids);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
    this.emitSelection();
  }

  protected toggleAll(): void {
    if (this.allSelected()) {
      this.selectedIds.set(new Set());
    } else {
      this.selectedIds.set(new Set(this.produits().map(p => p.id!)));
    }
    this.emitSelection();
  }


  protected isChecked(produit: IProduit): boolean {
    return this.selectedIds().has(produit.id!);
  }

  private emitSelection(): void {
    const ids = this.selectedIds();
    this.selectionChanged.emit(this.produits().filter(p => ids.has(p.id!)));
  }

  protected onMenuAction(produit: IProduit, action: ProduitMenuAction): void {
    if (action === "edit") {
      this.editRequested.emit(produit);
    } else if (action === "delete") {
      this.deleteRequested.emit(produit);
    } else {
      this.menuAction.emit({ action, produit });
    }
  }

  protected stockSeverity(produit: IProduit): "success" | "warn" | "danger" | "secondary" {
    const qty = produit.totalQuantity ?? 0;
    const seuil = produit.seuilMini ?? 0;
    if (qty <= 0) return "danger";
    if (seuil > 0 && qty < seuil) return "warn";
    return "success";
  }

  protected tauxMarge(produit: IProduit): number | null {
    const pv = produit.regularUnitPrice;
    const pa = produit.costAmount;
    if (!pv || pv === 0 || pa == null) return null;
    return Math.round(((pv - pa) / pv) * 100);
  }

  protected joursStock(produit: IProduit): number | null {
    const v = produit.couvertureStockJours;
    if (v == null || v < 0) return null;
    return v;
  }

  protected classeLabel(classe?: string): string {
    if (!classe) return "—";
    return classe.replace("_", "+");
  }

  protected menuItemsFor(produit: IProduit): MenuEntry[] {
    const hasRealRayon = produit.rayonProduits?.some(rp => rp.codeRayon !== 'SANS') ?? false;
    const items: MenuEntry[] = [
      { label: "Voir le détail", icon: "pi pi-eye", action: "view" },
    ];

    if (this.canEdit()) {
      items.push({ label: "Éditer", icon: "pi pi-pencil", action: "edit" });
    }

    items.push({ label: "Imprimer étiquette", icon: "pi pi-tag", action: "print-label", separatorBefore: true });
    items.push({ label: "Tarifs assurance", icon: "pi pi-euro", action: "prix-reference", separatorBefore: true });
    items.push({
      label: hasRealRayon ? "Cloner vers un autre stockage" : "Assigner un emplacement",
      icon: hasRealRayon ? "pi pi-copy" : "pi pi-map-marker",
      action: "clone-rayon",
      separatorBefore: true,
    });

    if (this.canEdit()) {
      items.push({
        label: "Saisir un lot",
        icon: "pi pi-tag",
        action: "saisir-lots",
        disabled: (produit.totalQuantity ?? 0) <= 0,
        separatorBefore: true,
      });

      if (produit.deconditionnable) {
        items.push({ label: "Configurer le détail", icon: "pi pi-sliders-h", action: "add-detail", separatorBefore: true });
        items.push({
          label: "Déconditionner",
          icon: "pi pi-box",
          action: "decondition",
          disabled: (produit.totalQuantity ?? 0) <= 0 || produit.produits.length === 0,
        });
      }

      items.push(
        produit.status === "DISABLE"
          ? { label: "Réactiver", icon: "pi pi-play", action: "activate", separatorBefore: true }
          : { label: "Mettre en veille", icon: "pi pi-pause", action: "suspend", separatorBefore: true }
      );
    }

    if (this.canDelete()) {
      items.push({
        label: "Supprimer",
        icon: "pi pi-trash",
        action: "delete",
        danger: true,
        disabled: (produit.totalQuantity ?? 0) > 0,
        separatorBefore: true,
      });
    }

    return items;
  }
}
