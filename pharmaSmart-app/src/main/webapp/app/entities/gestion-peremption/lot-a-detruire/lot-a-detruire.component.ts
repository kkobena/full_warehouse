import { Component, inject, OnInit, viewChild, ChangeDetectionStrategy } from "@angular/core";
import { ProductToDestroyService } from "../product-to-destroy.service";
import { ITEMS_PER_PAGE } from "../../../shared/constants/pagination.constants";
import { HttpHeaders, HttpResponse } from "@angular/common/http";
import { NGB_DATE_TO_ISO } from "../../../shared/util/warehouse-util";
import { ProductToDestroy, ProductToDestroyFilter, ProductToDestroySum } from "../model/product-to-destroy";
import { TranslatePipe } from "@ngx-translate/core";
import { IMagasin } from "../../../shared/model";
import { Storage } from "../../storage/storage.model";
import { IFournisseur } from "../../../shared/model/fournisseur.model";
import { IRayon } from "../../../shared/model/rayon.model";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { NgbDateStruct, NgbTooltip } from "@ng-bootstrap/ng-bootstrap";
import { Params } from "../../../shared/model/enumerations/params.model";
import { ConfigurationService } from "../../../shared/configuration.service";
import { RayonService } from "../../rayon/rayon.service";
import { MagasinService } from "../../magasin/magasin.service";
import { StorageService } from "../../storage/storage.service";
import { RouterLink } from "@angular/router";
import { PeremptionStatut } from "../model/peremption-statut";
import { PharmaDatePickerComponent } from "../../../shared/date-picker/pharma-date-picker.component";
import { SpinnerComponent } from "../../../shared/spinner/spinner.component";
import { NgbConfirmDialogService } from "../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NotificationService } from "../../../shared/services/notification.service";
import { CommonModule } from "@angular/common";
import { BlobDownloadService } from "../../../shared/services/blob-download.service";
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
  KpiItemComponent,
  KpiStripComponent,
  RowCheckboxComponent,
  SelectComponent,
  SortableHeaderDirective,
  SplitButtonComponent,
  ToolbarComponent
} from "../../../shared/ui";

@Component({
  selector: "jhi-lot-a-detruire",
  imports: [
    ButtonComponent,
    FloatLabelComponent,
    IconFieldComponent,
    ReactiveFormsModule,
    SelectComponent,
    SplitButtonComponent,
    ToolbarComponent,
    FormsModule,
    TranslatePipe,
    CommonModule,
    DataTableComponent,
    BadgeComponent,
    HeaderCheckboxComponent,
    RowCheckboxComponent,
    SortableHeaderDirective,
    KpiStripComponent,
    KpiItemComponent,
    NgbTooltip,
    PharmaDatePickerComponent,
    SpinnerComponent,
    RouterLink,
    FournisseurSelectComponent
  ],
  templateUrl: "./lot-a-detruire.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./lot-a-detruire.component.scss"
})
export class LotADetruireComponent implements OnInit {
  protected isMono = true;
  protected productToDestroySum: ProductToDestroySum = null;
  protected data: ProductToDestroy[] = [];
  protected selectedItems: ProductToDestroy[] = [];
  protected selectedMagasin: IMagasin = null;
  protected selectedStorage: Storage = null;
  protected selectedFournisseur: IFournisseur = null;
  protected selectedRayon: IRayon = null;
  protected produitId: number;
  protected numLot: string;
  protected searchTerm: string;
  protected fromDate: NgbDateStruct = null;
  protected toDate: NgbDateStruct = null;
  protected storages: Storage[] = [];
  protected rayons: IRayon[] = [];
  protected magasins: IMagasin[] = [];
  protected showAdvancedFilters = false;
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected page!: number;
  protected loading!: boolean;
  protected ngbPaginationPage = 1;
  protected totalItems = 0;
  protected exportMenus: AppSplitButtonItem[];
  protected types: any[] = [
    {
      label: "Déjà détruits",
      value: true
    },
    {
      label: "A détruire",
      value: false
    },
    {
      label: "Tout",
      value: null
    }
  ];
  protected selectedType: any = null;
  private readonly productToDestroyService = inject(ProductToDestroyService);
  private readonly configurationService = inject(ConfigurationService);
  private readonly rayonService = inject(RayonService);
  private readonly magasinSrevice = inject(MagasinService);
  private readonly storageService = inject(StorageService);
  private readonly spinner = viewChild.required<SpinnerComponent>("spinner");
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly downloadDocumentService = inject(BlobDownloadService);

  ngOnInit(): void {
    this.selectedType = this.types[2];
    this.findConfigStock();

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
    this.getSum();
  }

  protected getSeverity(status: PeremptionStatut): "danger" | "warn" | "info" {
    if (!status) return "info";
    if (status.days < 0) return "danger";
    if (status.days === 0) return "warn";
    return "info";
  }

  protected confirmDestroyDialog(id: number): void {
    this.confirmDialog.onConfirm(
      () => this.destroy(id),
      "Confirmation",
      "Êtes-vous sûr de vouloir détruire ce stock ?"
    );
  }

  protected onStorageChange(): void {
    this.fetchRayon();
    this.onSearch();
  }

  protected onFilterChange(): void {
    this.onSearch();
  }

  protected toggleAdvancedFilters(): void {
    this.showAdvancedFilters = !this.showAdvancedFilters;
  }

  protected onFournisseurSelected(f: IFournisseur | null): void {
    this.selectedFournisseur = f;
    this.onSearch();
  }

  protected resetFilters(): void {
    this.selectedType = this.types[2];
    this.searchTerm = null;
    this.fromDate = null;
    this.toDate = null;
    this.selectedFournisseur = null;
    this.selectedRayon = null;
    this.selectedMagasin = null;
    this.selectedStorage = null;
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

  protected lazyLoading(event: AppTableLazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.productToDestroyService
        .query({
          page: this.page,
          size: event.rows,
          ...this.buidParams()
        })
        .subscribe({
          next: (res: HttpResponse<ProductToDestroy[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError()
        });
    }
  }

  protected getSum(): void {
    this.productToDestroyService.getSum(this.buidParams()).subscribe(res => {
      this.productToDestroySum = res.body;
    });
  }

  protected onDestroyAll(): void {
    const count = this.selectedItems?.length ?? 0;
    const totalQty = this.selectedItems?.reduce((s, i) => s + (i.quantity ?? 0), 0) ?? 0;
    const totalValeur = this.selectedItems?.reduce((s, i) => s + (i.prixAchat ?? 0) * (i.quantity ?? 0), 0) ?? 0;
    const message =
      `Détruire définitivement ${count} lot(s) ?\n` +
      `Quantité totale : ${totalQty.toLocaleString("fr-FR")} unités\n` +
      `Valeur achat estimée : ${totalValeur.toLocaleString("fr-FR")} FCFA\n\n` +
      `⚠️ Cette action est irréversible.`;
    this.confirmDialog.onConfirm(
      () => this.destroyAll(),
      "Confirmer la destruction groupée",
      message
    );
  }

  private fetchStorages(): void {
    this.storageService.fetchStorages({ magasinId: this.selectedMagasin?.id }).subscribe((res: HttpResponse<Storage[]>) => {
      this.storages = res.body || [];
    });
  }

  private destroy(id: number): void {
    this.spinner().show();
    this.productToDestroyService
      .destroy({
        ids: [id],
        all: false
      })
      .subscribe({
        next: () => this.loadPage(),
        error: () => this.onError()
      });
  }

  private findConfigStock(): void {
    this.configurationService.getParamByKey(Params.APP_GESTION_STOCK).subscribe(
      {
        next: res => {
          if (res.body) {
            this.isMono = Number(res.body.value) === 0;
            if (!this.isMono) {
              this.fetchMagasin();
            }
            this.fetchRayon();

          }

        }
      }
    );

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

  private fetchMagasin(): void {
    this.magasinSrevice.fetchAll().subscribe((res: HttpResponse<IMagasin[]>) => {
      this.magasins = res.body || [];
    });
  }

  private exportPdf(): void {
    this.spinner().show();
    this.productToDestroyService.exportToPdf(this.buidParams()).subscribe({
      next: blod => {
        this.spinner().hide();
        this.downloadDocumentService.downloadPdf(blod, "lot_a_detruire");
      },
      error: () => this.spinner().hide()
    });
  }

  private onExport(format: string): void {
    this.spinner().show();
    this.productToDestroyService.export(format, this.buidParams()).subscribe({
      next: resp => {
        this.spinner().hide();
        this.downloadDocumentService.download(resp.body, "lot_a_detruire", format === "csv" ? "csv" : "excel");

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

  /** Reflète l'état « tout sélectionné » de la table, sans dépendre de son ordre d'initialisation dans le template. */
  private isAllSelected(): boolean {
    if (!this.data.length) {
      return false;
    }
    const selectedIds = new Set(this.selectedItems.map(item => item.id));
    return this.data.every(item => selectedIds.has(item.id));
  }

  private destroyAll(): void {
    this.productToDestroyService
      .destroy({
        ids: this.selectedItems?.map(item => item.id) || [],
        all: this.isAllSelected()
      })
      .subscribe({
        next: () => this.loadPage(),
        error: () => this.onError()
      });
  }

  private buidParams(): ProductToDestroyFilter {
    return {
      searchTerm: this.searchTerm,
      fromDate: this.fromDate ? NGB_DATE_TO_ISO(this.fromDate) : undefined,
      toDate: this.toDate ? NGB_DATE_TO_ISO(this.toDate) : undefined,
      fournisseurId: this.selectedFournisseur?.id,
      rayonId: this.selectedRayon?.id,
      magasinId: this.selectedMagasin?.id,
      destroyed: this.selectedType?.value,
      storageId: this.selectedStorage?.id,
      editing: false
    };
  }

  private onSuccess(data: ProductToDestroy[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get("X-Total-Count"));
    this.page = page;
    this.data = data || [];
    this.ngbPaginationPage = this.page;
    this.loading = false;
  }

  private onError(): void {
    this.spinner().hide();
    this.ngbPaginationPage = this.page ?? 1;
    this.loading = false;
    this.notificationService.error("Une erreur est survenue", "Erreur");
  }

  private loadPage(page?: number): void {
    // spinner affiché au début, masqué dans onSuccess/onError
    this.loading = true;
    const pageToLoad: number = page || this.page || 1;
    this.productToDestroyService
      .query({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        ...this.buidParams()
      })
      .subscribe({
        next: (res: HttpResponse<ProductToDestroy[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError()
      });
  }
}
