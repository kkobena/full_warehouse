import { Component, computed, inject, OnInit, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { Router, RouterModule } from "@angular/router";
import { FormsModule } from "@angular/forms";
import { HttpHeaders } from "@angular/common/http";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { ButtonModule } from "primeng/button";
import { InputTextModule } from "primeng/inputtext";
import { SelectModule } from "primeng/select";
import { IconField } from "primeng/iconfield";
import { InputIcon } from "primeng/inputicon";
import { SplitButtonModule } from "primeng/splitbutton";
import { ToolbarModule } from "primeng/toolbar";
import { TableLazyLoadEvent } from "primeng/table";
import { MenuItem, SelectItem } from "primeng/api";
import { TooltipModule } from "primeng/tooltip";
import { Authority } from "app/shared/constants/authority.constants";
import { IProduit } from "app/shared/model/produit.model";
import { IFamilleProduit } from "app/shared/model/famille-produit.model";
import { IRayon } from "app/shared/model/rayon.model";
import { FamilleProduitService } from "app/entities/famille-produit/famille-produit.service";
import { RayonService } from "app/entities/rayon/rayon.service";
import { NgbConfirmDialogService } from "app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { WarehouseCommonModule } from "app/shared/warehouse-common/warehouse-common.module";
import { ProductsApiService } from "../../data-access/services/products-api.service";
import { ProduitListComponent, ProduitMenuAction } from "../../ui/produit-list/produit-list.component";
import { ProduitDetailPanelComponent } from "../../ui/produit-detail-panel/produit-detail-panel.component";
import {
  ImportProduitModalComponent
} from "../../../../entities/produit/import-produit-modal/import-produit-modal.component";
import { IResponseDto } from "../../../../shared/util/response-dto";
import { showCommonModal } from "../../../../entities/sales/selling-home/sale-helper";
import {
  ImportProduitReponseModalComponent
} from "../../../../entities/produit/import-produit-reponse-modal/import-produit-reponse-modal.component";
import { CommandeRapideModalComponent } from "../../ui/commande-rapide-modal/commande-rapide-modal.component";
import { ProduitGeneriquesModalComponent } from "../../ui/generiques-modal/produit-generiques-modal.component";
import { ProduitEtiquetteModalComponent } from "../../ui/etiquette-modal/produit-etiquette-modal.component";
import { ProduitDetailFormModalComponent } from "../../ui/detail-form-modal/produit-detail-form-modal.component";
import { ProduitDeconditionModalComponent } from "../../ui/decondition-modal/produit-decondition-modal.component";
import { NotificationService } from "app/shared/services/notification.service";
import { ListPrixReferenceComponent } from "../../ui/prix-reference/list-prix-reference/list-prix-reference.component";

@Component({
  selector: "app-produit-home",
  templateUrl: "./produit-home.component.html",
  styleUrls: ["./produit-home.component.scss"],
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ButtonModule,
    InputTextModule,
    SelectModule,
    SplitButtonModule,
    ToolbarModule,
    IconField,
    InputIcon,
    TooltipModule,
    WarehouseCommonModule,
    ProduitListComponent,
    ProduitDetailPanelComponent
  ]
})
export class ProduitHomeComponent implements OnInit {
  protected readonly Authority = Authority;

  protected produits = signal<IProduit[]>([]);
  protected totalItems = signal(0);
  protected loading = signal(false);
  protected selectedProduit = signal<IProduit | null>(null);
  protected panelOpen = computed(() => this.selectedProduit() !== null);
  protected showHint = signal<boolean>(localStorage.getItem("produit-list-hint-dismissed") !== "1");
  protected selectedProduits = signal<IProduit[]>([]);
  protected hasSelection = computed(() => this.selectedProduits().length > 0);
  protected clearSelectionTrigger = signal(0);

  protected familles = signal<IFamilleProduit[]>([]);
  protected rayons = signal<IRayon[]>([]);
  protected filterOptions: SelectItem[] = [
    { label: "Produits actifs", value: "ENABLE" },
    { label: "Produits désactivés", value: "DISABLE" },
    { label: "Déconditionnables", value: "DECONDITIONNABLE" },
    { label: "Déconditionnés", value: "DECONDITIONNE" },
    { label: "Tous", value: "ALL" }
  ];

  protected search = "";
  protected selectedFilter = "ENABLE";
  protected selectedFamilleId: number | null = null;
  protected selectedRayonId: number | null = null;
  protected page = 0;
  protected rows = 10;
  protected sortField = "libelle";
  protected sortOrder = 1;

  protected importMenuItems: MenuItem[] = [
    { label: "Nouvelle installation", icon: "pi pi-file-excel", command: () => this.onImport("NOUVELLE_INSTALLATION") },
    { label: "Basculement", icon: "pi pi-filter", command: () => this.onImport("BASCULEMENT") },
    { label: "Basculement prestige", icon: "pi pi-file", command: () => this.onImport("BASCULEMENT_PRESTIGE") }
  ];

  private readonly api = inject(ProductsApiService);
  private readonly familleService = inject(FamilleProduitService);
  private readonly rayonService = inject(RayonService);
  private readonly router = inject(Router);
  private readonly modalService = inject(NgbModal);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);


  ngOnInit(): void {
    this.loadReferentiels();
    this.loadPage();
  }

  protected onLazyLoad(event: TableLazyLoadEvent): void {
    this.page = Math.floor((event.first ?? 0) / (event.rows ?? this.rows));
    this.rows = event.rows ?? this.rows;
    if (event.sortField) {
      this.sortField = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
      this.sortOrder = event.sortOrder ?? 1;
    }
    this.loadPage();
  }

  protected onSearch(): void {
    this.page = 0;
    this.loadPage();
  }

  protected onFilterChange(): void {
    this.page = 0;
    this.loadPage();
  }

  protected onProduitSelected(produit: IProduit): void {
    this.selectedProduit.set(produit);
  }

  protected onClosePanel(): void {
    this.selectedProduit.set(null);
  }

  protected dismissHint(): void {
    localStorage.setItem("produit-list-hint-dismissed", "1");
    this.showHint.set(false);
  }

  protected onEditRequested(produit: IProduit): void {
    if (produit.typeProduit === "DETAIL") {
      const ref = this.modalService.open(ProduitDetailFormModalComponent, {
        size: "lg",
        centered: true,
        backdrop: "static"
      });

      const decon = ref.componentInstance as ProduitDetailFormModalComponent;
      decon.entity = produit;
      decon.produit = produit.parent;
      ref.closed.subscribe(() => this.refreshProduit(produit.id!));
    } else {
      this.router.navigate(["/produits", produit.id, "edit"]);
    }

  }

  protected onDeleteRequested(produit: IProduit): void {
    this.confirmDialog.onConfirm(
      () => this.deleteProduit(produit),
      "Suppression",
      `Voulez-vous supprimer le produit "${produit.libelle}" ?`
    );
  }

  protected onNewProduit(): void {
    this.router.navigate(["/produits/new"]);
  }

  protected onSelectionChanged(produits: IProduit[]): void {
    this.selectedProduits.set(produits);
  }

  protected onBulkDisable(): void {
    const list = this.selectedProduits();
    const count = list.length;
    this.confirmDialog.onConfirm(
      () => this.executeBulk(list, "DISABLE"),
      "Mettre en veille",
      `Mettre en veille ${count} produit(s) sélectionné(s) ?`
    );
  }

  protected onBulkEnable(): void {
    const list = this.selectedProduits();
    const count = list.length;
    this.confirmDialog.onConfirm(
      () => this.executeBulk(list, "ENABLE"),
      "Réactiver",
      `Réactiver ${count} produit(s) sélectionné(s) ?`
    );
  }

  protected onClearSelection(): void {
    this.selectedProduits.set([]);
    this.clearSelectionTrigger.update(v => v + 1);
  }

  private executeBulk(list: IProduit[], status: "ENABLE" | "DISABLE"): void {
    let completed = 0;
    for (const produit of list) {
      this.api.patchStatus(produit.id!, status).subscribe({
        next: () => {
          completed++;
          this.produits.update(all =>
            all.map(p => p.id === produit.id ? { ...p, status: status === "ENABLE" ? 0 : 1 } : p)
          );
          if (completed === list.length) {
            this.selectedProduits.set([]);
            this.clearSelectionTrigger.update(v => v + 1);
          }
        }
      });
    }
  }

  protected onMenuAction(event: { action: ProduitMenuAction; produit: IProduit }): void {
    const { action, produit } = event;
    switch (action) {
      case "view":
        this.selectedProduit.set(produit);
        break;
      case "commander":
        this.openCommandeRapide(produit);
        break;
      case "generiques":
        this.openGeneriques(produit);
        break;
      case "print-label":
        this.openEtiquette(produit);
        break;
      case "add-detail":
        this.openDetailForm(produit);
        break;
      case "decondition":
        this.openDecondition(produit);
        break;
      case "suspend":
        this.confirmDialog.onConfirm(
          () => this.changeStatus(produit, "DISABLE"),
          "Mettre en veille",
          `Mettre en veille "${produit.libelle}" ? Le produit sera masqué des ventes et des commandes.`
        );
        break;
      case "activate":
        this.confirmDialog.onConfirm(
          () => this.changeStatus(produit, "ENABLE"),
          "Réactiver",
          `Réactiver "${produit.libelle}" ?`
        );
        break;
      case "archive":
        this.confirmDialog.onConfirm(
          () => this.changeStatus(produit, "DISABLE"),
          "Archiver",
          `Archiver "${produit.libelle}" ? Cette action désactivera le produit.`
        );
        break;
      case "prix-reference":
        this.openPrixReference(produit);
        break;
    }
  }

  private changeStatus(produit: IProduit, status: "ENABLE" | "DISABLE"): void {
    this.api.patchStatus(produit.id!, status).subscribe({
      next: () => {
        // Mise à jour locale immédiate sans rechargement complet
        this.produits.update(list =>
          list.map(p => p.id === produit.id ? { ...p, status: status === "ENABLE" ? 0 : 1 } : p)
        );
        if (this.selectedProduit()?.id === produit.id) {
          this.selectedProduit.update(p => p ? { ...p, status: status === "ENABLE" ? 0 : 1 } : null);
        }
      }
    });
  }

  private loadPage(): void {
    this.loading.set(true);
    const req: any = {
      page: this.page,
      size: this.rows,
      search: this.search || "",
      sort: [`${this.sortField},${this.sortOrder === 1 ? "asc" : "desc"}`]
    };

    if (this.selectedFilter === "DECONDITIONNABLE") {
      req.deconditionnable = true;
      req.status = "ENABLE";
    } else if (this.selectedFilter === "DECONDITIONNE") {
      req.deconditionne = true;
      req.status = "ENABLE";
    } else if (this.selectedFilter !== "ALL") {
      req.status = this.selectedFilter;
    }

    if (this.selectedFamilleId) req.familleId = this.selectedFamilleId;
    if (this.selectedRayonId) req.rayonId = this.selectedRayonId;

    this.api.query(req).subscribe({
      next: (res) => this.onSuccess(res.body ?? [], res.headers),
      error: () => this.loading.set(false)
    });
  }

  private onSuccess(data: IProduit[], headers: HttpHeaders): void {
    this.totalItems.set(Number(headers.get("X-Total-Count") ?? 0));
    this.produits.set(data);
    this.loading.set(false);
    this.selectedProduits.set([]);
    this.clearSelectionTrigger.update(v => v + 1);
  }

  private deleteProduit(produit: IProduit): void {
    this.router.navigate(["/produits", produit.id, "edit"]);
  }

  private loadReferentiels(): void {
    this.familleService.query({ search: "" }).subscribe({
      next: res => this.familles.set(res.body ?? [])
    });
    this.rayonService.query({ search: "", page: 0, size: 9999 }).subscribe({
      next: res => this.rayons.set(res.body ?? [])
    });
  }

  private openGeneriques(produit: IProduit): void {
    const ref = this.modalService.open(ProduitGeneriquesModalComponent, { size: "lg", centered: true });
    ref.componentInstance.produit = produit;
  }

  private openEtiquette(produit: IProduit): void {
    const ref = this.modalService.open(ProduitEtiquetteModalComponent, { size: "lg", centered: true });
    ref.componentInstance.produit = produit;
  }

  private openCommandeRapide(produit: IProduit): void {
    const ref = this.modalService.open(CommandeRapideModalComponent, {
      size: "lg",
      centered: true
    });
    ref.componentInstance.produit = produit;
  }

  private openDetailForm(produit: IProduit): void {
    this.api.getById(produit.id!).subscribe(full => {
      const ref = this.modalService.open(ProduitDetailFormModalComponent, {
        size: "lg",
        centered: true,
        backdrop: "static"
      });
      const inst = ref.componentInstance as ProduitDetailFormModalComponent;
      inst.produit = full;
      if (full.produits?.length) {
        inst.entity = full.produits[0];
      }
      ref.closed.subscribe(() => this.refreshProduit(produit.id!));
    });
  }

  private openPrixReference(produit: IProduit): void {
    const ref = this.modalService.open(ListPrixReferenceComponent, {
      size: "lg",
      centered: true,
      backdrop: "static"
    });
    const inst = ref.componentInstance as ListPrixReferenceComponent;
    inst.produit = produit;
    inst.isFromProduit = true;
  }

  private openDecondition(produit: IProduit): void {
    this.api.getById(produit.id!).subscribe(full => {
      if (!full.produits?.length) {
        this.notificationService.warning(
          "Le produit n'a pas de détail configuré. Configurez-le d'abord.",
          "Déconditionnement impossible"
        );
        return;
      }
      const ref = this.modalService.open(ProduitDeconditionModalComponent, {
        size: "lg",
        centered: true,
        backdrop: "static"
      });
      (ref.componentInstance as ProduitDeconditionModalComponent).produit = full;
      ref.closed.subscribe(() => this.refreshProduit(produit.id!));
    });
  }

  /**
   * Rafraîchit le produit dans la liste et dans le panneau de détail
   * sans recharger toute la page, en préservant l'onglet actif.
   */
  private refreshProduit(id: number): void {
    this.api.getById(id).subscribe(fresh => {
      // Mise à jour en place dans la liste
      this.produits.update(list => list.map(p => p.id === id ? { ...fresh } : p));
      // Mise à jour du produit sélectionné (même id → le panel ne réinitialise pas l'onglet actif)
      if (this.selectedProduit()?.id === id) {
        this.selectedProduit.set({ ...fresh });
      }
    });
  }

  private onImport(type: string): void {
    const modalRef = this.modalService.open(ImportProduitModalComponent, {
      backdrop: "static",
      size: "lg",
      centered: true
    });
    modalRef.componentInstance.type = type;
    modalRef.closed.subscribe(reason => {
      if (reason) {
        this.showResponse(reason);
        this.loadPage();
      }
    });
  }

  private showResponse(responsedto: IResponseDto): void {
    showCommonModal(this.modalService, ImportProduitReponseModalComponent, { responsedto }, () => {
    }, "lg");
  }

}
