import { Component, computed, effect, input, output, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { TableLazyLoadEvent, TableModule } from "primeng/table";
import { ButtonModule } from "primeng/button";
import { TooltipModule } from "primeng/tooltip";
import { TagModule } from "primeng/tag";
import { MenuModule } from "primeng/menu";
import { CheckboxModule } from "primeng/checkbox";
import { MenuItem } from "primeng/api";
import { FormsModule } from "@angular/forms";
import { IProduit } from "app/shared/model/produit.model";
import { EtaProduitComponent } from "app/shared/eta-produit/eta-produit.component";

export type ProduitMenuAction =
  | "view"
  | "edit"
  | "print-label"
  | "commander"
  | "generiques"
  | "lots"
  | "suspend"
  | "activate"
  | "archive"
  | "delete";

@Component({
  selector: "app-produit-list",
  templateUrl: "./produit-list.component.html",
  styleUrls: ["./produit-list.component.scss"],
  imports: [CommonModule, FormsModule, TableModule, ButtonModule, TooltipModule, TagModule, MenuModule, CheckboxModule, EtaProduitComponent]
})
export class ProduitListComponent {
  readonly produits = input.required<IProduit[]>();
  readonly totalItems = input<number>(0);
  readonly rows = input<number>(15);
  readonly loading = input<boolean>(false);
  readonly selectedProduit = input<IProduit | null>(null);
  readonly clearSelectionTrigger = input<number>(0);

  readonly produitSelected = output<IProduit>();
  readonly lazyLoad = output<TableLazyLoadEvent>();
  readonly editRequested = output<IProduit>();
  readonly deleteRequested = output<IProduit>();
  readonly menuAction = output<{ action: ProduitMenuAction; produit: IProduit }>();
  readonly selectionChanged = output<IProduit[]>();

  protected menuItems = signal<MenuItem[]>([]);
  protected selectedIds = signal<Set<number>>(new Set());
  protected allSelected = computed(() => {
    const ids = this.selectedIds();
    const produits = this.produits();
    return produits.length > 0 && produits.every(p => ids.has(p.id!));
  });

  private currentMenuProduit: IProduit | null = null;

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

  protected openContextMenu(event: Event, produit: IProduit, menu: any): void {
    event.stopPropagation();
    this.currentMenuProduit = produit;
    this.menuItems.set(this.buildMenuItems(produit));
    menu.toggle(event);
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

  private emit(action: ProduitMenuAction): void {
    if (this.currentMenuProduit) {
      if (action === "edit") {
        this.editRequested.emit(this.currentMenuProduit);
      } else if (action === "delete") {
        this.deleteRequested.emit(this.currentMenuProduit);
      } else {
        this.menuAction.emit({ action, produit: this.currentMenuProduit });
      }
    }
  }

  private buildMenuItems(produit: IProduit): MenuItem[] {
    return [
      {
        label: "Voir le détail",
        icon: "pi pi-eye",
        command: () => this.emit("view")
      },
      {
        label: "Éditer",
        icon: "pi pi-pencil",
        command: () => this.emit("edit")
      },
      {
        label: "Imprimer étiquette",
        icon: "pi pi-tag",
        command: () => this.emit("print-label")
      },
      { separator: true },
      {
        label: "Commander",
        icon: "pi pi-shopping-cart",
        command: () => this.emit("commander")
      },
      {
        label: "Génériques / substituts",
        icon: "pi pi-list",
        command: () => this.emit("generiques")
      },
      /* {
         label: 'Lots actifs',
         icon: 'pi pi-box',
         command: () => this.emit('lots'),
       },*/
      { separator: true },
      produit.status === 1
        ? {
          label: "Réactiver",
          icon: "pi pi-play",
          command: () => this.emit("activate")
        }
        : {
          label: "Mettre en veille",
          icon: "pi pi-pause",
          command: () => this.emit("suspend")
        },
      { separator: true },
      {
        label: "Supprimer",
        icon: "pi pi-trash",
        styleClass: "text-danger",
        disabled: (produit.totalQuantity ?? 0) > 0,
        command: () => this.emit("delete")
      }
    ];
  }
}
