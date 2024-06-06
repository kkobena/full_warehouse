import { Component, OnInit } from '@angular/core';
import { ToolbarModule } from 'primeng/toolbar';
import { FormsModule } from '@angular/forms';
import { RippleModule } from 'primeng/ripple';
import { ButtonModule } from 'primeng/button';
import { ITEMS_PER_PAGE } from '../../shared/constants/pagination.constants';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { MvtCaisse, TypeFinancialTransaction } from '../cash-register/model/cash-register.model';
import { MvtCaisseServiceService } from './mvt-caisse-service.service';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ConfirmationService, MessageService } from 'primeng/api';
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
  ],
  templateUrl: './visualisation-mvt-caisse.component.html',
  styleUrl: './visualisation-mvt-caisse.component.scss',
})
export class VisualisationMvtCaisseComponent implements OnInit {
  protected search = '';
  protected mvtCaisses: MvtCaisse[] = [];
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

  constructor(
    protected mvtCaisseService: MvtCaisseServiceService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modeService: ModePaymentService,
    private dialogService: DialogService,
  ) {}

  ngOnInit(): void {
    this.onSearch();
  }

  onSearch(): void {
    this.loadPage();
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.mvtCaisseService
      .findAllMvts({
        page: pageToLoad,
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
      })
      .subscribe({
        next: (res: HttpResponse<MvtCaisse[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
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

  protected addNew(): void {
    this.ref = this.dialogService.open(FormTransactionComponent, {
      data: { entity: null, type: 'CARNET' },
      header: "FORMULAIRE D'AJOUT DE MOUVEMENT DE CAISSE",
      width: '500',
    });
    this.ref.onClose.subscribe(() => {
      this.loadPage();
    });
  }
}
