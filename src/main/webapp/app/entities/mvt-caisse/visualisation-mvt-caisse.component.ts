import { AfterViewInit, Component, inject, OnInit, viewChild } from '@angular/core';
import { ToolbarModule } from 'primeng/toolbar';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { MvtCaisse, MvtCaisseWrapper, TypeFinancialTransaction } from '../cash-register/model/cash-register.model';
import { MvtCaisseServiceService } from './mvt-caisse-service.service';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { InputTextModule } from 'primeng/inputtext';
import { IPaymentMode } from '../../shared/model/payment-mode.model';
import { IUser } from '../../core/user/user.model';
import { ModePaymentService } from '../mode-payments/mode-payment.service';
import { MultiSelectModule } from 'primeng/multiselect';
import { FormTransactionComponent } from './form-transaction/form-transaction.component';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { DATE_FORMAT_ISO_DATE } from '../../shared/util/warehouse-util';
import { getTypeName, MvtCaisseParams } from './mvt-caisse-util';
import { ButtonGroupModule } from 'primeng/buttongroup';
import { DividerModule } from 'primeng/divider';
import { UserService } from '../../core/user/user.service';
import { TranslateService } from '@ngx-translate/core';
import { CardModule } from 'primeng/card';
import { MvtParamServiceService } from './mvt-param-service.service';
import { PrimeNG } from 'primeng/config';
import { DatePicker } from 'primeng/datepicker';
import { Select } from 'primeng/select';
import { FloatLabel } from 'primeng/floatlabel';
import { ToastAlertComponent } from '../../shared/toast-alert/toast-alert.component';
import { showCommonModal } from '../sales/selling-home/sale-helper';

@Component({
  selector: 'jhi-visualisation-mvt-caisse',
  imports: [
    WarehouseCommonModule,
    ToolbarModule,
    FormsModule,
    ButtonModule,
    TableModule,
    TooltipModule,
    InputTextModule,
    MultiSelectModule,
    ButtonGroupModule,
    DividerModule,
    CardModule,
    DatePicker,
    Select,
    FloatLabel,
    ToastAlertComponent
  ],
  templateUrl: './visualisation-mvt-caisse.component.html'
})
export class VisualisationMvtCaisseComponent implements OnInit, AfterViewInit {
  protected mvtCaisses: MvtCaisse[] = [];
  protected mvtCaisseSum: MvtCaisseWrapper | null = null;
  protected totalItems = 0;
  protected loading!: boolean;
  protected btnLoading = false;
  protected page = 0;
  protected predicate!: string;
  protected ascending!: boolean;
  protected ngbPaginationPage = 1;
  protected readonly itemsPerPage = 10;
  protected fromDate: Date | undefined;
  protected toDate: Date | undefined;
  protected fromTime: Date | undefined;
  protected toTime: Date | undefined;
  protected order = 'ASC';
  protected selectedUser: IUser | null = null;
  protected selectedModes: IPaymentMode[] = [];
  protected users: IUser[];
  protected types: TypeFinancialTransaction[] = [
    TypeFinancialTransaction.ENTREE_CAISSE,
    TypeFinancialTransaction.SORTIE_CAISSE,
    TypeFinancialTransaction.REGLEMENT_DIFFERE,
    TypeFinancialTransaction.REGLEMENT_TIERS_PAYANT,
    TypeFinancialTransaction.CASH_SALE,
    TypeFinancialTransaction.CREDIT_SALE
  ];
  protected selectedTypes: TypeFinancialTransaction[] = [];
  protected paymentModes: IPaymentMode[] = [];
  private readonly colors: string[] = ['bg-primary', 'bg-info', 'bg-success', 'bg-warning', 'bg-danger', 'bg-secondary'];
  private readonly colorsByTypes: string[] = ['bg-primary', 'bg-info', 'bg-success', 'bg-warning', 'bg-secondary'];
  private readonly userService = inject(UserService);
  private readonly mvtCaisseService = inject(MvtCaisseServiceService);
  private readonly modeService = inject(ModePaymentService);
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly translate = inject(TranslateService);
  private readonly mvtParamServiceService = inject(MvtParamServiceService);
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly modalService = inject(NgbModal);

  ngOnInit(): void {
    if (this.mvtParamServiceService.mvtCaisseParam()) {
      this.fromDate = this.mvtParamServiceService.mvtCaisseParam().fromDate;
      this.toDate = this.mvtParamServiceService.mvtCaisseParam().toDate;
      this.selectedTypes = this.mvtParamServiceService.mvtCaisseParam().selectedTypes;
      this.selectedModes = this.mvtParamServiceService.mvtCaisseParam().paymentModes;
      this.selectedUser = this.mvtParamServiceService.mvtCaisseParam().selectedUser;
    }
    this.loadModes();
    this.loadUsers();
    this.onSearch();
  }

  ngAfterViewInit(): void {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
  }

  onSearch(): void {
    this.btnLoading = true;
    this.loadPage();
    this.loadSum();
    this.updateParam();
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.mvtCaisseService
      .findAllMvts({
        page: pageToLoad,
        size: this.itemsPerPage,
        ...this.buildParams()
      })
      .subscribe({
        next: (res: HttpResponse<MvtCaisse[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
        complete: () => {
          this.btnLoading = false;
        }
      });
  }

  protected getColorClass(index: number): string {
    return this.colors[index % this.colors.length];
  }

  protected getColorClassForTypeMvts(index: number): string {
    return this.colorsByTypes[index % this.colors.length];
  }

  protected lazyLoading(event: TableLazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.mvtCaisseService
        .findAllMvts({
          page: this.page,
          size: event.rows,
          ...this.buildParams()
        })
        .subscribe({
          next: (res: HttpResponse<MvtCaisse[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError()
        });
    }
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
    this.btnLoading = false;
  }

  protected onPrint(): void {
    this.btnLoading = true;
    this.updateParam();
    this.mvtCaisseService.exportToPdf(this.buildParams()).subscribe({
      next: blod => {
        this.btnLoading = false;
        const blobUrl = URL.createObjectURL(blod);
        window.open(blobUrl);
      },
      error: () => {
        this.btnLoading = false;

        this.alert().showError('Erreur', 'Une erreur est survenue lors de l\'exportation');
      },
      complete: () => {
        this.btnLoading = false;
      }
    });
  }

  protected addNew(): void {
    showCommonModal(
      this.modalService,
      FormTransactionComponent,
      { header: 'FORMULAIRE D\'AJOUT DE MOUVEMENT DE CAISSE' },
      () => this.onSearch(), 'lg'
    );
  }

  private onSuccess(data: MvtCaisse[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;


    this.mvtCaisses = data || [];
    this.loading = false;
  }

  private buildParams(): any {
    return {
      fromDate: DATE_FORMAT_ISO_DATE(this.fromDate),
      toDate: DATE_FORMAT_ISO_DATE(this.toDate),
      fromTime: this.fromTime,
      toTime: this.toTime,
      typeFinancialTransactions: this.selectedTypes?.map(type => getTypeName(type)),
      paymentModes: this.selectedModes?.map(mode => mode.code),
      userId: this.selectedUser?.id,
      order: this.order
    };
  }

  private loadModes(): void {
    this.modeService.query().subscribe((res: HttpResponse<IPaymentMode[]>) => {
      this.paymentModes = res.body || [];
    });
  }

  private loadUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<IUser[]>) => {
      this.users = res.body || [];
    });
  }

  private loadSum(): void {
    this.mvtCaisseService.findAllMvtsSum(this.buildParams()).subscribe((res: HttpResponse<MvtCaisseWrapper>) => {
      this.mvtCaisseSum = res.body || null;
      this.loading = false;
    });
  }

  private setParam(): void {
    const param: MvtCaisseParams = {
      fromDate: this.fromDate,
      toDate: this.toDate,
      selectedTypes: this.selectedTypes,
      paymentModes: this.selectedModes,
      selectedUser: this.selectedUser
    };
    this.mvtParamServiceService.setMvtCaisseParam(param);
  }

  private updateParam(): void {
    const params = this.mvtParamServiceService.mvtCaisseParam();
    if (params) {
      params.fromDate = this.fromDate;
      params.toDate = this.toDate;
      params.selectedTypes = this.selectedTypes;
      params.paymentModes = this.selectedModes;
      params.selectedUser = this.selectedUser;
      this.mvtParamServiceService.setMvtCaisseParam(params);
    } else {
      this.setParam();
    }
  }
}
