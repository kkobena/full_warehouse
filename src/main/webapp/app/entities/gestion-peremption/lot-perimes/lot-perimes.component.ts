import { AfterViewInit, Component, inject, OnInit, viewChild } from "@angular/core";
import { LotService } from "../../commande/lot/lot.service";
import { LotFilterParam, LotLocation, LotPerimes, LotPerimeValeurSum } from '../model/lot-perimes';
import { DATE_FORMAT_ISO_DATE } from "../../../shared/util/warehouse-util";
import { MenuItem } from "primeng/api";
import { HttpHeaders, HttpResponse } from "@angular/common/http";
import { ITEMS_PER_PAGE } from "../../../shared/constants/pagination.constants";
import { ToolbarModule } from "primeng/toolbar";
import { IconField } from "primeng/iconfield";
import { InputIcon } from "primeng/inputicon";
import { FormsModule } from "@angular/forms";
import { InputText } from "primeng/inputtext";
import { RayonService } from "../../rayon/rayon.service";
import { IRayon } from "../../../shared/model/rayon.model";
import { IFournisseur } from "../../../shared/model/fournisseur.model";
import { FournisseurService } from "../../fournisseur/fournisseur.service";
import { MagasinService } from "../../magasin/magasin.service";
import { IMagasin } from "../../../shared/model";
import { Storage } from "../../storage/storage.model";
import { StorageService } from "../../storage/storage.service";
import { FloatLabel } from "primeng/floatlabel";
import { SelectModule } from "primeng/select";
import { TranslatePipe } from "@ngx-translate/core";
import { IFamilleProduit } from "../../../shared/model/famille-produit.model";
import { FamilleProduitService } from "../../famille-produit/famille-produit.service";
import { KeyFilter } from "primeng/keyfilter";
import { Button } from "primeng/button";
import { SplitButton } from "primeng/splitbutton";
import { RouterLink } from "@angular/router";
import { DatePipe, DecimalPipe } from "@angular/common";
import { TableHeaderCheckbox, TableLazyLoadEvent, TableModule } from "primeng/table";
import { Tag } from "primeng/tag";
import { PeremptionStatut } from "../model/peremption-statut";
import { ProductToDestroyService } from "../product-to-destroy.service";
import { ProductsToDestroyPayload, ProductToDestroyPayload } from "../model/product-to-destroy";
import { Divider } from "primeng/divider";
import { DatePickerComponent } from "../../../shared/date-picker/date-picker.component";
import { saveAs } from "file-saver";
import { extractFileName2 } from "../../../shared/util/file-utils";
import { SpinnerComponent } from "../../../shared/spinner/spinner.component";
import { NgbConfirmDialogService } from "../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NotificationService } from "../../../shared/services/notification.service";
import { ButtonGroup } from "primeng/buttongroup";
import { Tooltip } from "primeng/tooltip";

@Component({
  selector: "jhi-lot-perimes",
  imports: [
    ToolbarModule,
    IconField,
    InputIcon,
    FormsModule,
    InputText,
    FloatLabel,
    SelectModule,
    TranslatePipe,
    KeyFilter,
    Button,
    SplitButton,
    RouterLink,
    DatePipe,
    DecimalPipe,
    TableModule,
    Tag,
    Divider,
    DatePickerComponent,
    SpinnerComponent,
    ButtonGroup,
    Tooltip
  ],
  templateUrl: "./lot-perimes.component.html",
  styleUrl: "./lot-perimes.component.scss"
})
export class LotPerimesComponent implements OnInit, AfterViewInit {
  protected checkbox = viewChild<TableHeaderCheckbox>("checkbox");
  protected payload: ProductsToDestroyPayload = null;
  protected exportMenus: MenuItem[];
  protected selectedLotPerimes: LotPerimes[] = [];
  protected lotPerimeValeurSum: LotPerimeValeurSum = null;
  protected storages: Storage[] = [];
  protected rayons: IRayon[] = [];
  protected magasins: IMagasin[] = [];
  protected fournisseurs: IFournisseur[] = [];
  protected famillesProduit: IFamilleProduit[] = [];
  protected selectedMagasin: IMagasin = null;
  protected selectedStorage: Storage = null;
  protected selectedFournisseur: IFournisseur = null;
  protected selectedFamilleProduit: IFamilleProduit = null;
  protected selectedRayon: IRayon = null;
  protected data: LotPerimes[] = [];
  protected dayCount: number;
  protected searchTerm: string;
  protected fromDate: Date = null;
  protected toDate: Date = null;
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
  private readonly fournisseurService = inject(FournisseurService);
  private readonly familleProduitService = inject(FamilleProduitService);
  private readonly magasinSrevice = inject(MagasinService);
  private readonly storageService = inject(StorageService);
  private readonly productToDestroyService = inject(ProductToDestroyService);
  private readonly spinner = viewChild.required<SpinnerComponent>("spinner");
  private readonly lotService = inject(LotService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);

  ngAfterViewInit(): void {
  }

  ngOnInit(): void {
    this.selectedType = this.types[2];
    this.findConfigStock();
    this.fetchFournisseur();
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

  protected lazyLoading(event: TableLazyLoadEvent): void {
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

  protected confirmRetirerDialog(lot: LotPerimes): void {
    if (this.isMultiLocation(lot) && !this.resolveStorageId(lot)) {
      this.notificationService.error(
        'Ce lot est présent dans plusieurs emplacements. Veuillez sélectionner un emplacement avant de retirer.',
        'Emplacement requis',
      );
      return;
    }
    this.confirmDialog.onConfirm(
      () => this.retirerStock(lot),
      'Confirmation',
      'Voulez-vous retirer la quantité du stock ?',
    );
  }

  protected confirmRetourFournisseurDialog(lot: LotPerimes): void {
    this.confirmDialog.onConfirm(
      () => this.retirerStock(lot),
      "Retour fournisseur",
      `Voulez-vous initier un retour fournisseur pour le lot "${lot.numLot}" (${lot.produitName}) ?`
    );
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
      fromDate: DATE_FORMAT_ISO_DATE(this.fromDate),
      toDate: DATE_FORMAT_ISO_DATE(this.toDate),   // ✅ BUG CORRIGÉ (était fromDate)
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

  private fetchFournisseur(): void {
    this.fournisseurService
      .query({
        page: 0,
        size: 9999
      })
      .subscribe((res: HttpResponse<IFournisseur[]>) => {
        this.fournisseurs = res.body || [];
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
      storageId: this.resolveStorageId(lot),
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
