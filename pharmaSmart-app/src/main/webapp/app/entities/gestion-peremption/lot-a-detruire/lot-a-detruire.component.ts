import { Component, inject, OnInit, viewChild, ChangeDetectionStrategy, input, signal } from "@angular/core";
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
import { currencySymbol } from 'app/shared/utils/format-utils';
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
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: "./lot-a-detruire.component.scss"
})
export class LotADetruireComponent implements OnInit {
  /**
   * Code de l'entrée de navigation dont cet écran est le contenu.
   *
   * <p>Fourni par le layout : le titre de la barre suit le libellé du menu, ou son `titre_long`
   * quand la barre nomme plus longuement.
   */
  readonly navCode = input<string>('');

  protected readonly isMono = signal(true);
  protected readonly productToDestroySum = signal<ProductToDestroySum>(null);
  protected readonly data = signal<ProductToDestroy[]>([]);
  protected selectedItems: ProductToDestroy[] = [];
  protected readonly selectedMagasin = signal<IMagasin>(null);
  protected readonly selectedStorage = signal<Storage>(null);
  protected readonly selectedFournisseur = signal<IFournisseur>(null);
  protected readonly selectedRayon = signal<IRayon>(null);
  protected produitId: number;
  protected numLot: string;
  protected readonly searchTerm = signal<string | undefined>(undefined);
  protected readonly fromDate = signal<NgbDateStruct>(null);
  protected readonly toDate = signal<NgbDateStruct>(null);
  protected readonly storages = signal<Storage[]>([]);
  protected readonly rayons = signal<IRayon[]>([]);
  protected readonly magasins = signal<IMagasin[]>([]);
  protected readonly showAdvancedFilters = signal(false);
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected readonly page = signal<number | undefined>(undefined);
  protected readonly loading = signal<boolean | undefined>(undefined);
  protected readonly ngbPaginationPage = signal(1);
  protected readonly totalItems = signal(0);
  protected readonly exportMenus = signal<AppSplitButtonItem[] | undefined>(undefined);
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
  protected readonly selectedType = signal<any>(null);
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
    this.selectedType.set(this.types[2]);
    this.findConfigStock();

    this.exportMenus.set([
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
    ]);
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
    this.showAdvancedFilters.set(!this.showAdvancedFilters());
  }

  protected onFournisseurSelected(f: IFournisseur | null): void {
    this.selectedFournisseur.set(f);
    this.onSearch();
  }

  protected resetFilters(): void {
    this.selectedType.set(this.types[2]);
    this.searchTerm.set(null);
    this.fromDate.set(null);
    this.toDate.set(null);
    this.selectedFournisseur.set(null);
    this.selectedRayon.set(null);
    this.selectedMagasin.set(null);
    this.selectedStorage.set(null);
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
      this.page.set(event.first / event.rows);
      this.loading.set(true);
      this.productToDestroyService
        .query({
          page: this.page(),
          size: event.rows,
          ...this.buidParams()
        })
        .subscribe({
          next: (res: HttpResponse<ProductToDestroy[]>) => this.onSuccess(res.body, res.headers, this.page()),
          error: () => this.onError()
        });
    }
  }

  protected getSum(): void {
    this.productToDestroyService.getSum(this.buidParams()).subscribe(res => {
      this.productToDestroySum.set(res.body);
    });
  }

  protected onDestroyAll(): void {
    const count = this.selectedItems?.length ?? 0;
    const totalQty = this.selectedItems?.reduce((s, i) => s + (i.quantity ?? 0), 0) ?? 0;
    const totalValeur = this.selectedItems?.reduce((s, i) => s + (i.prixAchat ?? 0) * (i.quantity ?? 0), 0) ?? 0;
    const message =
      `Détruire définitivement ${count} lot(s) ?\n` +
      `Quantité totale : ${totalQty.toLocaleString("fr-FR")} unités\n` +
      `Valeur achat estimée : ${totalValeur.toLocaleString("fr-FR")} ${currencySymbol()}\n\n` +
      `⚠️ Cette action est irréversible.`;
    this.confirmDialog.onConfirm(
      () => this.destroyAll(),
      "Confirmer la destruction groupée",
      message
    );
  }

  private fetchStorages(): void {
    this.storageService.fetchStorages({ magasinId: this.selectedMagasin()?.id }).subscribe((res: HttpResponse<Storage[]>) => {
      this.storages.set(res.body || []);
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
            this.isMono.set(Number(res.body.value) === 0);
            if (!this.isMono()) {
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
        storageId: this.selectedStorage()?.id,
        size: 9999
      })
      .subscribe((res: HttpResponse<IRayon[]>) => {
        this.rayons.set(res.body || []);
      });
  }

  private fetchMagasin(): void {
    this.magasinSrevice.fetchAll().subscribe((res: HttpResponse<IMagasin[]>) => {
      this.magasins.set(res.body || []);
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
    if (!this.data().length) {
      return false;
    }
    const selectedIds = new Set(this.selectedItems.map(item => item.id));
    return this.data().every(item => selectedIds.has(item.id));
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
      searchTerm: this.searchTerm(),
      fromDate: this.fromDate() ? NGB_DATE_TO_ISO(this.fromDate()) : undefined,
      toDate: this.toDate() ? NGB_DATE_TO_ISO(this.toDate()) : undefined,
      fournisseurId: this.selectedFournisseur()?.id,
      rayonId: this.selectedRayon()?.id,
      magasinId: this.selectedMagasin()?.id,
      destroyed: this.selectedType()?.value,
      storageId: this.selectedStorage()?.id,
      editing: false
    };
  }

  private onSuccess(data: ProductToDestroy[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems.set(Number(headers.get("X-Total-Count")));
    this.page.set(page);
    this.data.set(data || []);
    this.ngbPaginationPage.set(this.page());
    this.loading.set(false);
  }

  private onError(): void {
    this.spinner().hide();
    this.ngbPaginationPage.set(this.page() ?? 1);
    this.loading.set(false);
    this.notificationService.error("Une erreur est survenue", "Erreur");
  }

  private loadPage(page?: number): void {
    // spinner affiché au début, masqué dans onSuccess/onError
    this.loading.set(true);
    const pageToLoad: number = page || this.page() || 1;
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
