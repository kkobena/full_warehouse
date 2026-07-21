import { Component, inject, OnInit, viewChild, ChangeDetectionStrategy } from "@angular/core";
import { LotService } from "../../commande/lot/lot.service";
import { LotFilterParam, LotLocation, LotPerimes, LotPerimeValeurSum } from "../model/lot-perimes";
import { NGB_DATE_TO_ISO } from "../../../shared/util/warehouse-util";
import { HttpHeaders, HttpResponse } from "@angular/common/http";
import { ITEMS_PER_PAGE } from "../../../shared/constants/pagination.constants";
import { FormsModule } from "@angular/forms";
import { RayonService } from "../../rayon/rayon.service";
import { IRayon } from "../../../shared/model/rayon.model";
import { IFournisseur } from "../../../shared/model/fournisseur.model";
import { MagasinService } from "../../magasin/magasin.service";
import { IMagasin } from "../../../shared/model";
import { Storage } from "../../storage/storage.model";
import { StorageService } from "../../storage/storage.service";
import { TranslatePipe } from "@ngx-translate/core";
import { IFamilleProduit } from "../../../shared/model/famille-produit.model";
import { FamilleProduitService } from "../../famille-produit/famille-produit.service";
import { NgbDateStruct, NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { Router, RouterLink } from "@angular/router";
import { PeremptionStatut } from "../model/peremption-statut";
import { ProductToDestroyService } from "../product-to-destroy.service";
import { ProductsToDestroyPayload, ProductToDestroyPayload } from "../model/product-to-destroy";
import { PharmaDatePickerComponent } from "../../../shared/date-picker/pharma-date-picker.component";
import { saveAs } from "file-saver";
import { extractFileName2 } from "../../../shared/util/file-utils";
import { SpinnerComponent } from "../../../shared/spinner/spinner.component";
import { NgbConfirmDialogService } from "../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { NotificationService } from "../../../shared/services/notification.service";
import {
  RetourFournisseurPerimeDialogComponent
} from "../retour-fournisseur-perime-dialog/retour-fournisseur-perime-dialog.component";
import {
  RetourGroupePerimeDialogComponent
} from "../retour-groupe-perime-dialog/retour-groupe-perime-dialog.component";
import { CommonModule } from "@angular/common";
import { FournisseurSelectComponent } from "../../../features/partners/ui/fournisseur-select/fournisseur-select.component";
import {
  AppSplitButtonItem,
  AppTableLazyLoadEvent,
  BadgeComponent,
  ButtonComponent,
  DataTableComponent,
  FloatLabelComponent,
  HeaderCheckboxComponent,
  IconFieldComponent,
  KeyFilterDirective,
  KpiItemComponent,
  KpiStripComponent,
  RowCheckboxComponent,
  SelectComponent,
  SortableHeaderDirective,
  SplitButtonComponent,
  ToolbarComponent
} from "../../../shared/ui";

@Component({
  selector: "jhi-lot-perimes",
  imports: [
    CommonModule,
    ToolbarComponent,
    IconFieldComponent,
    FormsModule,
    FloatLabelComponent,
    SelectComponent,
    TranslatePipe,
    KeyFilterDirective,
    ButtonComponent,
    SplitButtonComponent,
    RouterLink,
    DataTableComponent,
    BadgeComponent,
    HeaderCheckboxComponent,
    RowCheckboxComponent,
    SortableHeaderDirective,
    PharmaDatePickerComponent,
    SpinnerComponent,
    NgbTooltip,
    FournisseurSelectComponent,
    KpiStripComponent,
    KpiItemComponent
  ],
  templateUrl: "./lot-perimes.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./lot-perimes.component.scss"
})
export class LotPerimesComponent implements OnInit {
  protected payload: ProductsToDestroyPayload = null;
  protected exportMenus: AppSplitButtonItem[];
  protected selectedLotPerimes: LotPerimes[] = [];
  protected lotPerimeValeurSum: LotPerimeValeurSum = null;
  protected storages: Storage[] = [];
  protected rayons: IRayon[] = [];
  protected magasins: IMagasin[] = [];
  protected famillesProduit: IFamilleProduit[] = [];
  protected selectedMagasin: IMagasin = null;
  protected selectedStorage: Storage = null;
  protected selectedFournisseur: IFournisseur = null;
  protected selectedFamilleProduit: IFamilleProduit = null;
  protected selectedRayon: IRayon = null;
  protected data: LotPerimes[] = [];
  protected dayCount: number;
  protected searchTerm: string;
  protected fromDate: NgbDateStruct = null;
  protected toDate: NgbDateStruct = null;
  protected showAdvancedFilters = false;
  /**
   * Stocke le storageId sélectionné par l'utilisateur pour chaque lot multi-site.
   * Clé = lot.id, Valeur = storageId choisi.
   */
  protected selectedLocationMap = new Map<number, number>();

  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected page!: number;
  protected loading!: boolean;
  protected totalItems = 0;
  protected types: any[] = [
    {
      label: "Déjà périmé",
      value: "PERIME"
    },
    {
      label: "En cours",
      value: "EN_COURS"
    },
    {
      label: "Tout",
      value: "ALL"
    }
  ];
  protected selectedType: any = null;
  private readonly rayonService = inject(RayonService);
  private readonly familleProduitService = inject(FamilleProduitService);
  private readonly magasinSrevice = inject(MagasinService);
  private readonly storageService = inject(StorageService);
  private readonly productToDestroyService = inject(ProductToDestroyService);
  private readonly spinner = viewChild.required<SpinnerComponent>("spinner");
  private readonly lotService = inject(LotService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly modalService = inject(NgbModal);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.selectedType = this.types[2];
    this.findConfigStock();
    this.fetchFamillesProduit();
    this.getSum();
    this.exportMenus = [
      {
        label: "PDF",
        icon: "pi pi-file-pdf",
        command: () => this.exportPdf()
      },
      {
        label: "Excel",
        icon: "pi pi-file-excel",
        command: () => this.onExport("excel")
      },
      {
        label: "Csv",
        icon: "pi pi-file-export",
        command: () => this.onExport("csv")
      }
    ];
    this.onSearch();
  }

  protected onMagasinChange(): void {
    this.fetchStorages();
    this.onSearch();
  }

  protected onSearch(): void {
    this.getSum();
    this.loadPage();
  }

  protected onStorageChange(): void {
    this.fetchRayon();
    this.onSearch();
  }

  protected onFilterChange(): void {
    this.onSearch();
  }

  /** Toggle panneau filtres avancés */
  protected toggleAdvancedFilters(): void {
    this.showAdvancedFilters = !this.showAdvancedFilters;
  }

  /** KPI card 1 — Lots déjà périmés */
  protected filterByPerimes(): void {
    this.selectedType = this.types.find(t => t.value === "PERIME");
    this.dayCount = null;
    this.fromDate = null;
    this.toDate = null;
    this.onSearch();
  }

  /** KPI card 3 — Prochaines péremptions dans 30j */
  protected filterByUpcoming30(): void {
    this.selectedType = this.types.find(t => t.value === "EN_COURS");
    this.dayCount = 30;
    this.fromDate = null;
    this.toDate = null;
    this.onSearch();
  }

  /** Réinitialise tous les filtres */
  protected onFournisseurSelected(f: IFournisseur | null): void {
    this.selectedFournisseur = f;
    this.onSearch();
  }

  protected resetFilters(): void {
    this.selectedType = this.types[2];
    this.dayCount = null;
    this.searchTerm = null;
    this.fromDate = null;
    this.toDate = null;
    this.selectedFournisseur = null;
    this.selectedRayon = null;
    this.selectedFamilleProduit = null;
    this.selectedMagasin = null;
    this.selectedStorage = null;
    this.selectedLocationMap.clear();
    this.onSearch();
  }

  protected lazyLoading(event: AppTableLazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.lotService
        .fetchLotPerimes({
          page: this.page,
          size: event.rows,
          ...this.buidParams()
        })
        .subscribe({
          next: (res: HttpResponse<LotPerimes[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError()
        });
    }
  }

  protected getSum(): void {
    this.lotService.getSum(this.buidParams()).subscribe({
      next: (res: HttpResponse<LotPerimeValeurSum>) => {
        this.lotPerimeValeurSum = res.body || null;
      },
      error: () => {
        this.lotPerimeValeurSum = null;
      }
    });
  }

  protected getSeverity(status: PeremptionStatut): "danger" | "warn" | "info" {
    if (!status) return "info";
    if (status.days < 0) return "danger";
    if (status.days === 0) return "warn";
    return "info";
  }

  /** Retourne true si le lot est dans plusieurs emplacements */
  protected isMultiLocation(lot: LotPerimes): boolean {
    return lot.locations?.length > 1;
  }

  /** Retourne la localisation unique du lot, ou undefined si multi/aucune */
  protected getSingleLocation(lot: LotPerimes): LotLocation | undefined {
    return lot.locations?.length === 1 ? lot.locations[0] : undefined;
  }

  /** Enregistre le choix d'emplacement de l'utilisateur pour un lot multi-site */
  protected onLocationSelect(lot: LotPerimes, storageId: number): void {
    this.selectedLocationMap.set(lot.id, storageId);
  }

  /** Résout le storageId à utiliser pour un lot donné */
  protected resolveStorageId(lot: LotPerimes): number | undefined {
    if (lot.locations?.length === 1) {
      return lot.locations[0].storageId;
    }
    return this.selectedLocationMap.get(lot.id) ?? this.selectedStorage?.id;
  }

  /** Options du sélecteur d'emplacement, libellé pré-formaté (remplace le `#item` custom de `p-select`) */
  protected locationOptions(lot: LotPerimes): { storageId: number; label: string }[] {
    return (lot.locations ?? []).map(loc => ({
      storageId: loc.storageId,
      label: `${loc.storageName} (${loc.qty})`
    }));
  }

  protected confirmRetirerDialog(lot: LotPerimes): void {
    if (this.isMultiLocation(lot) && !this.resolveStorageId(lot)) {
      this.notificationService.error(
        "Ce lot est présent dans plusieurs emplacements. Veuillez sélectionner un emplacement avant de retirer.",
        "Emplacement requis"
      );
      return;
    }
    this.confirmDialog.onConfirm(
      () => this.retirerStock(lot),
      "Confirmation",
      "Voulez-vous retirer la quantité du stock ?"
    );
  }

  protected confirmRetourFournisseurDialog(lot: LotPerimes): void {
    if (this.isMultiLocation(lot) && !this.resolveStorageId(lot)) {
      this.notificationService.error(
        "Ce lot est présent dans plusieurs emplacements. Veuillez sélectionner un emplacement avant de retourner.",
        "Emplacement requis"
      );
      return;
    }

    const modalRef = this.modalService.open(RetourFournisseurPerimeDialogComponent, {
      size: "lg",
      backdrop: "static",
      centered: true
    });
    modalRef.componentInstance.lot = lot;

    modalRef.closed.subscribe(() => {
      this.notificationService.success(
        `Retour fournisseur créé avec succès pour le lot "${lot.numLot}".`,
        "Succès"
      );
      this.loadPage();
    });
  }

  protected confirmAll(): void {
    const count = this.selectedLotPerimes.length;
    const totalQty = this.selectedLotPerimes.reduce((s, l) => s + (l.quantity ?? 0), 0);
    const totalValeur = this.selectedLotPerimes.reduce((s, l) => s + (l.prixAchat ?? 0) * (l.quantity ?? 0), 0);
    const message =
      `Retirer ${count} lot(s) du stock ?\n` +
      `Quantité totale : ${totalQty.toLocaleString("fr-FR")} unités\n` +
      `Valeur achat estimée : ${totalValeur.toLocaleString("fr-FR")} FCFA\n\n` +
      `⚠️ Cette action est irréversible.`;
    this.confirmDialog.onConfirm(
      () => this.retirerStock(),
      "Confirmer le retrait groupé",
      message
    );
  }

  protected confirmRetourGroupeDialog(): void {
    if (this.selectedLotPerimes.length === 0) return;

    const modalRef = this.modalService.open(RetourGroupePerimeDialogComponent, {
      size: "xl",
      backdrop: "static",
      centered: true
    });
    modalRef.componentInstance.lots = [...this.selectedLotPerimes];

    modalRef.closed.subscribe((result: any) => {
      if (result?.totalCreated > 0) {
        this.notificationService.success(
          `${result.totalCreated} retour(s) fournisseur créé(s) avec succès.`,
          "Succès"
        );
      }
      this.selectedLotPerimes = [];
      this.loadPage();
    });
  }

  protected navigateToRetours(): void {
    void this.router.navigate(["/commande/retour-fournisseur"]);
  }

  private exportPdf(): void {
    this.spinner().show();
    this.lotService.exportToPdf(this.buidParams()).subscribe({
      next: blod => {
        this.spinner().hide();
        window.open(URL.createObjectURL(blod));
      },
      error: () => this.spinner().hide()
    });
  }

  private onExport(format: string): void {
    this.spinner().show();
    this.lotService.export(format, this.buidParams()).subscribe({
      next: resp => {
        this.spinner().hide();
        const blob = resp.body;
        saveAs(blob, extractFileName2(resp.headers.get("Content-disposition"), format, "liste_de_produit_perimes"));
      },
      error: () => {
        this.spinner().hide();
        this.notificationService.error("Une erreur est survenue", "Erreur");
      },
      complete: () => {
        this.spinner().hide();
      }
    });
  }

  private fetchStorages(): void {
    this.storageService.fetchStorages({ magasinId: this.selectedMagasin?.id }).subscribe((res: HttpResponse<Storage[]>) => {
      this.storages = res.body || [];
    });
  }

  private findConfigStock(): void {
    this.fetchMagasin();
    this.fetchRayon();
  }

  private buidParams(): LotFilterParam {
    return {
      dayCount: this.dayCount,
      searchTerm: this.searchTerm,
      fromDate: NGB_DATE_TO_ISO(this.fromDate),
      toDate: NGB_DATE_TO_ISO(this.toDate),   // ✅ BUG CORRIGÉ (était fromDate)
      fournisseurId: this.selectedFournisseur?.id,
      rayonId: this.selectedRayon?.id,
      familleProduitId: this.selectedFamilleProduit?.id,
      magasinId: this.selectedMagasin?.id,
      storageId: this.selectedStorage?.id,
      type: this.selectedType?.value
    };
  }

  private onSuccess(data: LotPerimes[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get("X-Total-Count"));
    this.page = page;
    this.data = data || [];
    this.loading = false;
  }

  private onError(): void {
    this.loading = false;
    this.spinner().hide();
  }

  private loadPage(page?: number): void {
    const pageToLoad: number = page || this.page || 1;
    this.lotService
      .fetchLotPerimes({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        ...this.buidParams()
      })
      .subscribe({
        next: (res: HttpResponse<LotPerimes[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError()
      });
  }

  private fetchRayon(): void {
    this.rayonService
      .query({
        page: 0,
        storageId: this.selectedStorage?.id,
        size: 9999
      })
      .subscribe((res: HttpResponse<IRayon[]>) => {
        this.rayons = res.body || [];
      });
  }

  private fetchFamillesProduit(): void {
    this.familleProduitService
      .query({ page: 0, size: 9999 })
      .subscribe((res: HttpResponse<IFamilleProduit[]>) => {
        this.famillesProduit = res.body || [];
      });
  }

  private fetchMagasin(): void {
    this.magasinSrevice.fetchAll().subscribe((res: HttpResponse<IMagasin[]>) => {
      this.magasins = res.body || [];
    });
  }

  private buildPayload(lot: LotPerimes): ProductToDestroyPayload {
    return {
      lotId: lot.id,
      quantity: lot.quantity,
      produitId: lot.produitId,
      fournisseurId: this.selectedFournisseur?.id,
      // Résolution de l'emplacement :
      // 1. Lot dans 1 seul emplacement → automatique
      // 2. Lot multi-site → utiliser la sélection utilisateur ou le filtre storage
      // 3. null → cascade automatique backend (PRINCIPAL en premier)
      storageId: this.resolveStorageId(lot)
    };
  }

  private buildPayloads(lot?: LotPerimes): ProductsToDestroyPayload {
    return {
      magasinId: this.selectedMagasin?.id,
      products: lot
        ? [this.buildPayload(lot)]
        : this.selectedLotPerimes.map(d => this.buildPayload(d))
    };
  }

  private retirerStock(lot?: LotPerimes): void {
    this.spinner().show();
    this.productToDestroyService.addProductQuantity(this.buildPayloads(lot)).subscribe({
      next: () => {
        this.spinner().hide();
        this.loadPage();
      },
      error: () => {
        this.spinner().hide();
        this.notificationService.error("Une erreur est survenue", "Erreur");
      }
    });
  }
}
