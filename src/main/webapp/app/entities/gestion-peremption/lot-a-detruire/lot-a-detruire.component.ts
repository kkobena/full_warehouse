import { AfterViewInit, Component, inject, OnInit, viewChild } from '@angular/core';
import { ProductToDestroyService } from '../product-to-destroy.service';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { MenuItem } from 'primeng/api';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { ProductToDestroy, ProductToDestroyFilter, ProductToDestroySum } from '../model/product-to-destroy';
import { TableHeaderCheckbox, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { PrimeNG } from 'primeng/config';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { IMagasin } from '../../../shared/model/magasin.model';
import { Storage } from '../../storage/storage.model';
import { IFournisseur } from '../../../shared/model/fournisseur.model';
import { IRayon } from '../../../shared/model/rayon.model';
import { Button } from 'primeng/button';
import { FloatLabel } from 'primeng/floatlabel';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Select } from 'primeng/select';
import { SplitButton } from 'primeng/splitbutton';
import { Toolbar } from 'primeng/toolbar';
import { Params } from '../../../shared/model/enumerations/params.model';
import { ConfigurationService } from '../../../shared/configuration.service';
import { FournisseurService } from '../../fournisseur/fournisseur.service';
import { RayonService } from '../../rayon/rayon.service';
import { MagasinService } from '../../magasin/magasin.service';
import { StorageService } from '../../storage/storage.service';
import { DecimalPipe } from '@angular/common';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';
import { PeremptionStatut } from '../model/peremption-statut';
import { DatePickerComponent } from '../../../shared/date-picker/date-picker.component';
import { saveAs } from 'file-saver';
import { extractFileName2 } from '../../../shared/util/file-utils';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { SpinnerComponent } from '../../../shared/spinner/spinner.component';

@Component({
  selector: 'jhi-lot-a-detruire',
  imports: [
    Button,
    FloatLabel,
    IconField,
    InputIcon,
    InputText,
    ReactiveFormsModule,
    Select,
    SplitButton,
    Toolbar,
    FormsModule,
    TranslatePipe,
    DecimalPipe,
    TableModule,
    Tag,
    Tooltip,
    DatePickerComponent,
    ConfirmDialogComponent,
    ToastAlertComponent,
    SpinnerComponent
  ],
  templateUrl: './lot-a-detruire.component.html',
  styleUrl: './lot-a-detruire.component.scss'
})
export class LotADetruireComponent implements OnInit, AfterViewInit {
  protected checkbox = viewChild<TableHeaderCheckbox>('checkbox');

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
  protected fromDate: Date = null;
  protected toDate: Date = null;
  protected storages: Storage[] = [];
  protected rayons: IRayon[] = [];
  protected magasins: IMagasin[] = [];
  protected fournisseurs: IFournisseur[] = [];
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected page!: number;
  protected loading!: boolean;
  protected ngbPaginationPage = 1;
  protected totalItems = 0;
  protected exportMenus: MenuItem[];
  protected types: any[] = [
    {
      label: 'Déjà détruits',
      value: true
    },
    {
      label: 'A détruire',
      value: false
    },
    {
      label: 'Tout',
      value: null
    }
  ];
  protected selectedType: any = null;
  private readonly productToDestroyService = inject(ProductToDestroyService);
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly translate = inject(TranslateService);
  private readonly configurationService = inject(ConfigurationService);
  private readonly fournisseurService = inject(FournisseurService);
  private readonly rayonService = inject(RayonService);
  private readonly magasinSrevice = inject(MagasinService);
  private readonly storageService = inject(StorageService);
   private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');

  ngAfterViewInit(): void {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
  }

  ngOnInit(): void {
    this.selectedType = this.types[2];
    this.findConfigStock();
    this.fetchFournisseur();

    this.exportMenus = [
      {
        label: 'PDF',
        icon: 'pi pi-file-pdf',
        command: () => this.exportPdf()
      },
      {
        label: 'Excel',
        icon: 'pi pi-file-excel',
        command: () => this.onExport('EXCEL')
      },
      {
        label: 'Csv',
        icon: 'pi pi-file-export',
        command: () => this.onExport('CSV')
      }
    ];
    this.onSearch();
    this.getSum();
  }

  protected getSeverity(status: PeremptionStatut) {
    if (status.days < 0) {
      return 'danger';
    } else if (status.days === 0) {
      return 'warn';
    }
    return 'info';
  }

  protected confirmDestroyDialog(id: number): void {
    this.confimDialog().onConfirm(
      () => {
        this.destroy(id);
      },
      'Confirmation',
      'Êtes-vous sûr de vouloir détruire ce stock ?',
      null,
      () => {
      }
    );
  }

  protected onStorageChange(): void {
    this.fetchRayon();
    this.onSearch();
  }

  protected onFilterChange(): void {
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

  protected lazyLoading(event: TableLazyLoadEvent): void {
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
    this.confimDialog().onConfirm(
      () => {
        this.destroyAll();
      },
      'Confirmation',
      'Voulez-vous detruire tous les stocks de ces produits ?',
      null,
      () => {
      }
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
    const stockParam = this.configurationService.getParamByKey(Params.APP_GESTION_STOCK);
    if (stockParam) {
      this.isMono = Number(stockParam.value) === 0;
      if (!this.isMono) {
        this.fetchMagasin();
      }
      this.fetchRayon();
    }
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
        window.open(URL.createObjectURL(blod));
      },
      error: () => this.spinner().hide()
    });
  }

  private onExport(format: string): void {
    this.spinner().show();
    this.productToDestroyService.export(format, this.buidParams()).subscribe({
      next: resp => {
        this.spinner().hide();
        const blob = resp.body;
        saveAs(blob, extractFileName2(resp.headers.get('Content-disposition'), format, 'produits_a_detruire'));
      },
      error: () => {
        this.spinner().hide();
        this.alert().showError('Une erreur est survenue');
      },
      complete: () => {
        this.spinner().hide();
      }
    });
  }

  private destroyAll(): void {
    this.productToDestroyService
      .destroy({
        ids: this.selectedItems?.map(item => item.id) || [],
        all: this.checkbox()?.checked
      })
      .subscribe({
        next: () => this.loadPage(),
        error: () => this.onError()
      });
  }

  private buidParams(): ProductToDestroyFilter {
    return {
      searchTerm: this.searchTerm,
      fromDate: this.fromDate ? DATE_FORMAT_ISO_DATE(this.fromDate) : undefined,
      toDate: this.toDate ? DATE_FORMAT_ISO_DATE(this.toDate) : undefined,
      fournisseurId: this.selectedFournisseur?.id,
      rayonId: this.selectedRayon?.id,
      magasinId: this.selectedMagasin?.id,
      destroyed: this.selectedType?.value,
      storageId: this.selectedStorage?.id,
      editing: false
    };
  }

  private onSuccess(data: ProductToDestroy[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.data = data || [];
    this.ngbPaginationPage = this.page;
    this.loading = false;
  }

  private onError(): void {
    this.spinner().hide();
    this.ngbPaginationPage = this.page ?? 1;
    this.loading = false;
    this.alert().showError('Une erreur est survenue');
  }

  private loadPage(page?: number): void {
    this.spinner().hide();
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
