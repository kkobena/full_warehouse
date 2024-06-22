import { AfterViewInit, Component, inject, OnInit } from '@angular/core';
import { ToolbarModule } from 'primeng/toolbar';
import { FormsModule } from '@angular/forms';
import { RippleModule } from 'primeng/ripple';
import { ButtonModule } from 'primeng/button';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { MvtCaisse, MvtCaisseWrapper, TypeFinancialTransaction } from '../cash-register/model/cash-register.model';
import { MvtCaisseServiceService } from './mvt-caisse-service.service';
import { Router } from '@angular/router';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ConfirmationService, MessageService, PrimeNGConfig } from 'primeng/api';
import { InputTextModule } from 'primeng/inputtext';
import { CalendarModule } from 'primeng/calendar';
import { DropdownModule } from 'primeng/dropdown';
import { IPaymentMode } from '../../shared/model/payment-mode.model';
import { IUser } from '../../core/user/user.model';
import { ModePaymentService } from '../mode-payments/mode-payment.service';
import { MultiSelectModule } from 'primeng/multiselect';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FormTransactionComponent } from './form-transaction/form-transaction.component';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { DATE_FORMAT_ISO_DATE } from '../../shared/util/warehouse-util';
import { getTypeName } from './mvt-caisse-util';
import { ButtonGroupModule } from 'primeng/buttongroup';
import { DividerModule } from 'primeng/divider';
import { UserService } from '../../core/user/user.service';
import { TranslateService } from '@ngx-translate/core';
import { CardModule } from 'primeng/card';
import { Tuple } from '../../shared/model/tuple.model';

@Component({
  selector: 'jhi-visualisation-mvt-caisse',
  standalone: true,
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
    CalendarModule,
    DropdownModule,
    MultiSelectModule,
    ButtonGroupModule,
    DividerModule,
    CardModule,
  ],
  templateUrl: './visualisation-mvt-caisse.component.html',
})
export class VisualisationMvtCaisseComponent implements OnInit, AfterViewInit {
  protected search = '';
  protected mvtCaisses: MvtCaisse[] = [];
  protected mvtCaisseSum: MvtCaisseWrapper | null = null;
  protected totalItems = 0;
  protected loading!: boolean;
  protected page = 0;
  protected predicate!: string;
  protected ascending!: boolean;
  protected ngbPaginationPage = 1;
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
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
    TypeFinancialTransaction.CREDIT_SALE,
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
  private primeNGConfig = inject(PrimeNGConfig);
  private translate = inject(TranslateService);

  ngOnInit(): void {
    this.loadModes();
    this.loadUsers();
    this.onSearch();
    this.loadSum();
  }

  ngAfterViewInit(): void {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
  }

  onSearch(): void {
    this.loadPage();
    this.loadSum();
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.mvtCaisseService
      .findAllMvts({
        page: pageToLoad,
        ...this.buildParams(),
      })
      .subscribe({
        next: (res: HttpResponse<MvtCaisse[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  trackModeId(index: number, item: Tuple): string {
    return item.key;
  }

  protected getColorClass(index: number): string {
    return this.colors[index % this.colors.length];
  }

  protected getColorClassForTypeMvts(index: number): string {
    return this.colorsByTypes[index % this.colors.length];
  }

  protected onSuccess(data: MvtCaisse[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.router.navigate(['/mvt-caisse'], {
      queryParams: {
        page: this.page,
        size: this.itemsPerPage,
        search: this.search,
      },
    });

    this.mvtCaisses = data || [];
    this.loading = false;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }

  protected onPrint(): void {}

  protected addNew(): void {
    this.ref = this.dialogService.open(FormTransactionComponent, {
      data: { entity: null, type: 'CARNET' },
      header: "FORMULAIRE D'AJOUT DE MOUVEMENT DE CAISSE",
      width: '500',
    });
    this.ref.onClose.subscribe(() => {
      this.onSearch();
    });
  }

  private buildParams(): any {
    return {
      size: this.itemsPerPage,
      search: this.search,
      fromDate: DATE_FORMAT_ISO_DATE(this.fromDate),
      toDate: DATE_FORMAT_ISO_DATE(this.toDate),
      fromTime: this.fromTime,
      toTime: this.toTime,
      typeFinancialTransactions: this.selectedTypes?.map(type => getTypeName(type)),
      paymentModes: this.selectedModes.map(mode => mode.code),
      userId: this.selectedUser?.id,
      order: this.order,
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
    });
  }
}
