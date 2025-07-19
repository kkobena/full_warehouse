import { AfterViewInit, Component, inject, OnInit, viewChild } from '@angular/core';
import { LotService } from '../../commande/lot/lot.service';
import { LotFilterParam, LotPerimes, LotPerimeValeurSum } from '../model/lot-perimes';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { ConfirmationService, LazyLoadEvent, MenuItem, MessageService } from 'primeng/api';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { ToolbarModule } from 'primeng/toolbar';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { FormsModule } from '@angular/forms';
import { InputText } from 'primeng/inputtext';
import { Params } from '../../../shared/model/enumerations/params.model';
import { ConfigurationService } from '../../../shared/configuration.service';
import { RayonService } from '../../rayon/rayon.service';
import { IRayon } from '../../../shared/model/rayon.model';
import { IFournisseur } from '../../../shared/model/fournisseur.model';
import { FournisseurService } from '../../fournisseur/fournisseur.service';
import { MagasinService } from '../../magasin/magasin.service';
import { IMagasin } from '../../../shared/model/magasin.model';
import { Storage } from '../../storage/storage.model';
import { StorageService } from '../../storage/storage.service';
import { DatePicker } from 'primeng/datepicker';
import { FloatLabel } from 'primeng/floatlabel';
import { SelectModule } from 'primeng/select';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { IFamilleProduit } from '../../../shared/model/famille-produit.model';
import { KeyFilter } from 'primeng/keyfilter';
import { Button } from 'primeng/button';
import { ButtonGroup } from 'primeng/buttongroup';
import { SplitButton } from 'primeng/splitbutton';
import { PrimeNG } from 'primeng/config';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { TableHeaderCheckbox, TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { PeremptionStatut } from '../model/peremption-statut';
import { ProductToDestroyService } from '../product-to-destroy.service';
import { ProductsToDestroyPayload, ProductToDestroyPayload } from '../model/product-to-destroy';
import { acceptButtonProps, rejectButtonProps } from '../../../shared/util/modal-button-props';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { Divider } from 'primeng/divider';
import {RemoveButtonTextComponent} from "../../../shared/cta/remove-button-text.component";
import {CtaComponent} from "../../../shared/cta/cta.component";
import {DatePickerComponent} from "../../../shared/date-picker/date-picker.component";

@Component({
  selector: 'jhi-lot-perimes',
  providers: [ConfirmationService, MessageService],
  imports: [
    ToolbarModule,
    ConfirmDialogModule,
    IconField,
    InputIcon,
    FormsModule,
    InputText,
    DatePicker,
    FloatLabel,
    SelectModule,
    TranslatePipe,
    KeyFilter,
    Button,
    ButtonGroup,
    SplitButton,
    RouterLink,
    DecimalPipe,
    TableModule,
    Tag,
    ToastModule,
    NgxSpinnerModule,
    Divider,
    RemoveButtonTextComponent,
    CtaComponent,
    DatePickerComponent,
  ],
  templateUrl: './lot-perimes.component.html',
})
export class LotPerimesComponent implements OnInit, AfterViewInit {
  protected checkbox = viewChild<TableHeaderCheckbox>('checkbox');
  protected payload: ProductsToDestroyPayload = null;
  protected exportMenus: MenuItem[];
  protected selectedLotPerimes: LotPerimes[] = [];
  protected lotPerimeValeurSum: LotPerimeValeurSum = null;
  protected storages: Storage[] = [];
  protected rayons: IRayon[] = [];
  protected magasins: IMagasin[] = [];
  protected fournisseurs: IFournisseur[] = [];
  protected selectedMagasin: IMagasin = null;
  protected selectedStorage: Storage = null;
  protected selectedFournisseur: IFournisseur = null;
  protected selectedFamilleProduit: IFamilleProduit = null;
  protected selectedRayon: IRayon = null;
  protected isMono = true;
  protected data: LotPerimes[] = [];
  protected dayCount: number;
  protected searchTerm: string;
  protected fromDate: Date = null;
  protected toDate: Date = null;

  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected page!: number;
  protected loading!: boolean;
  protected ngbPaginationPage = 1;
  protected totalItems = 0;
  protected types: any[] = [
    {
      label: 'Déjà périmé',
      value: 'PERIME',
    },
    {
      label: 'En cours',
      value: 'EN_COURS',
    },
    {
      label: 'Tout',
      value: 'ALL',
    },
  ];
  protected selectedType: any = null;
  private readonly configurationService = inject(ConfigurationService);
  private readonly rayonService = inject(RayonService);
  private readonly fournisseurService = inject(FournisseurService);
  private readonly magasinSrevice = inject(MagasinService);
  private readonly storageService = inject(StorageService);
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly translate = inject(TranslateService);
  private readonly productToDestroyService = inject(ProductToDestroyService);
  private readonly spinner = inject(NgxSpinnerService);
  private readonly messageService = inject(MessageService);
  private readonly modalService = inject(ConfirmationService);
  private readonly lotService = inject(LotService);

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
    this.getSum();
    this.exportMenus = [
      {
        label: 'PDF',
        icon: 'pi pi-file-pdf',
        command: () => this.onPrint(),
      },
      {
        label: 'Excel',
        icon: 'pi pi-file-excel',
        command: () => this.onExcel(),
      },
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

  protected lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.lotService
        .fetchLotPerimes({
          page: this.page,
          size: event.rows,
          ...this.buidParams(),
        })
        .subscribe({
          next: (res: HttpResponse<LotPerimes[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError(),
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

      },
    });
  }

  protected getSeverity(status: PeremptionStatut) {
    if (status.days < 0) {
      return 'danger';
    } else if (status.days === 0) {
      return 'warn';
    }
    return 'info';
  }

  protected confirmRetirerDialog(lot: LotPerimes): void {
    this.modalService.confirm({
      message: 'Voulez-vous retirer la quantité du  stock ?',
      header: 'Confirmation',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.retirerStock(lot);
      },
    });
  }

  protected confirmAll(): void {
    this.modalService.confirm({
      message: 'Voulez-vous tout retirer du  stock ?',
      header: 'Confirmation',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.retirerStock();
      },
    });
  }

  private fetchStorages(): void {
    this.storageService.fetchStorages({ magasinId: this.selectedMagasin?.id }).subscribe((res: HttpResponse<Storage[]>) => {
      this.storages = res.body || [];
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

  private buidParams(): LotFilterParam {
    return {
      dayCount: this.dayCount,
      searchTerm: this.searchTerm,
      fromDate: DATE_FORMAT_ISO_DATE(this.fromDate),
      toDate: DATE_FORMAT_ISO_DATE(this.fromDate),
      fournisseurId: this.selectedFournisseur?.id,
      rayonId: this.selectedRayon?.id,
      familleProduitId: this.selectedFamilleProduit?.id,
      magasinId: this.selectedMagasin?.id,
      storageId: this.selectedStorage?.id,
      type: this.selectedType?.value,
    };
  }

  private onSuccess(data: LotPerimes[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));

    this.page = page;
    this.data = data || [];
    this.ngbPaginationPage = this.page;
    this.loading = false;
  }

  private onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
    this.loading = false;
    this.spinner.hide();
  }

  private loadPage(page?: number): void {
    const pageToLoad: number = page || this.page || 1;
    this.lotService
      .fetchLotPerimes({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        ...this.buidParams(),
      })
      .subscribe({
        next: (res: HttpResponse<LotPerimes[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  private fetchRayon(): void {
    this.rayonService
      .query({
        page: 0,
        storageId: this.selectedStorage?.id,
        size: 9999,
      })
      .subscribe((res: HttpResponse<IRayon[]>) => {
        this.rayons = res.body || [];
      });
  }

  private fetchFournisseur(): void {
    this.fournisseurService
      .query({
        page: 0,
        size: 9999,
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

  private onPrint(): void {}

  private onExcel(): void {}

  private buildPayload(lot: LotPerimes): ProductToDestroyPayload {
    return {
      lotId: lot.id,
      quantity: lot.quantity,
      produitId: lot.produitId,
      fournisseurId: this.selectedFournisseur?.id,
    };
  }

  private buildPayloads(lot?: LotPerimes): ProductsToDestroyPayload {
    return {
      magasinId: this.selectedMagasin?.id,
      products: lot
        ? [this.buildPayload(lot)]
        : this.selectedLotPerimes.map(d => {
            return this.buildPayload(d);
          }),
    };
  }

  private retirerStock(lot?: LotPerimes): void {
    this.spinner.show();
    this.productToDestroyService.addProductQuantity(this.buildPayloads(lot)).subscribe({
      next: () => {
        this.spinner.hide();
        this.loadPage();
      },
      error: () => {
        this.spinner.hide();
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: 'Une erreur est survenue',
        });
      },
    });
  }
}
