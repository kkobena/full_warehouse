import { AfterViewInit, Component, inject, OnInit } from '@angular/core';
import { ToolbarModule } from 'primeng/toolbar';
import { FormsModule } from '@angular/forms';
import { RippleModule } from 'primeng/ripple';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { MvtCaisse, MvtCaisseWrapper, TypeFinancialTransaction } from '../cash-register/model/cash-register.model';
import { MvtCaisseServiceService } from './mvt-caisse-service.service';
import { Router } from '@angular/router';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ConfirmationService, LazyLoadEvent, MessageService } from 'primeng/api';
import { InputTextModule } from 'primeng/inputtext';
import { IPaymentMode } from '../../shared/model/payment-mode.model';
import { IUser } from '../../core/user/user.model';
import { ModePaymentService } from '../mode-payments/mode-payment.service';
import { MultiSelectModule } from 'primeng/multiselect';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FormTransactionComponent } from './form-transaction/form-transaction.component';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { DATE_FORMAT_ISO_DATE } from '../../shared/util/warehouse-util';
import { getTypeName, MvtCaisseParams } from './mvt-caisse-util';
import { ButtonGroupModule } from 'primeng/buttongroup';
import { DividerModule } from 'primeng/divider';
import { UserService } from '../../core/user/user.service';
import { TranslateService } from '@ngx-translate/core';
import { CardModule } from 'primeng/card';
import { MvtParamServiceService } from './mvt-param-service.service';
import { ToastModule } from 'primeng/toast';
import { PrimeNG } from 'primeng/config';
import { DatePicker } from 'primeng/datepicker';
import { Select } from 'primeng/select';
import { FloatLabel } from 'primeng/floatlabel';

@Component({
  selector: 'jhi-visualisation-mvt-caisse',
  providers: [MessageService, DialogService, ConfirmationService, NgbActiveModal],
  imports: [
    WarehouseCommonModule,
    ToolbarModule,
    FormsModule,
    RippleModule,
    ButtonModule,
    TableModule,
    TooltipModule,
    InputTextModule,
    MultiSelectModule,
    ButtonGroupModule,
    DividerModule,
    CardModule,
    ToastModule,
    DatePicker,
    Select,
    FloatLabel
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
  protected ref!: DynamicDialogRef;
  private readonly colors: string[] = ['bg-primary', 'bg-info', 'bg-success', 'bg-warning', 'bg-danger', 'bg-secondary'];
  private readonly colorsByTypes: string[] = ['bg-primary', 'bg-info', 'bg-success', 'bg-warning', 'bg-secondary'];
  private userService = inject(UserService);
  private mvtCaisseService = inject(MvtCaisseServiceService);
  private router = inject(Router);
  private modeService = inject(ModePaymentService);
  private dialogService = inject(DialogService);
  private primeNGConfig = inject(PrimeNG);
  private translate = inject(TranslateService);
  private mvtParamServiceService = inject(MvtParamServiceService);
  private messageService = inject(MessageService);

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

  protected lazyLoading(event: LazyLoadEvent): void {
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
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Une erreur est survenue'
        });
      },
      complete: () => {
        this.btnLoading = false;
      }
    });
  }

  protected addNew(): void {
    this.ref = this.dialogService.open(FormTransactionComponent, {
      data: { entity: null },
      header: 'FORMULAIRE D\'AJOUT DE MOUVEMENT DE CAISSE',
      width: '50%'
    });
    this.ref.onClose.subscribe(() => {
      this.onSearch();
    });
  }

  private onSuccess(data: MvtCaisse[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    /*     this.router.navigate(['/mvt-caisse'], {
          queryParams: {
            page: this.page,
            size: this.itemsPerPage,
            search: this.search,
          },
        }); */

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
