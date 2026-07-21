import {ChangeDetectionStrategy, Component, inject, OnDestroy, OnInit} from "@angular/core";
import {FormsModule} from "@angular/forms";
import {
  MvtCaisse,
  MvtCaisseWrapper,
  TypeFinancialTransaction
} from "../cash-register/model/cash-register.model";
import {MvtCaisseServiceService} from "./mvt-caisse-service.service";
import {HttpHeaders, HttpResponse} from "@angular/common/http";
import {IPaymentMode} from "../../shared/model/payment-mode.model";
import {IUser} from "../../core/user/user.model";
import {ModePaymentService} from "../mode-payments/mode-payment.service";
import {FormTransactionComponent} from "./form-transaction/form-transaction.component";
import {NgbDateStruct, NgbModal, NgbTooltip} from "@ng-bootstrap/ng-bootstrap";
import {NGB_DATE_TO_ISO} from "../../shared/util/warehouse-util";
import {getTypeName, MvtCaisseParams} from "./mvt-caisse-util";
import {UserService} from "../../core/user/user.service";
import {MvtParamServiceService} from "./mvt-param-service.service";
import {showCommonModal} from "../sales/selling-home/sale-helper";
import {TauriPrinterService} from "../../shared/services/tauri-printer.service";
import {PaymentId} from "../reglement/model/reglement.model";
import {takeUntil} from "rxjs/operators";
import {Subject} from "rxjs";
import {BlobDownloadService} from "../../shared/services/blob-download.service";
import {CommonModule} from "@angular/common";
import {NotificationService} from "../../shared/services/notification.service";
import {
  NgbConfirmDialogService
} from "../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import {
  AppTableLazyLoadEvent,
  ButtonComponent,
  DataTableComponent,
  MultiSelectComponent,
  SelectComponent,
  ToolbarComponent
} from "../../shared/ui";
import {PharmaDatePickerComponent} from "../../shared/date-picker/pharma-date-picker.component";

@Component({
  selector: "app-visualisation-mvt-caisse",
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    ToolbarComponent,
    DataTableComponent,
    MultiSelectComponent,
    SelectComponent,
    PharmaDatePickerComponent,
    NgbTooltip
  ],
  templateUrl: "./visualisation-mvt-caisse.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ["./visualisation-mvt-caisse.scss"]
})
export class VisualisationMvtCaisseComponent implements OnInit, OnDestroy {
  protected mvtCaisses: MvtCaisse[] = [];
  protected mvtCaisseSum: MvtCaisseWrapper | null = null;
  protected totalItems = 0;
  protected loading!: boolean;
  protected btnLoading = false;
  protected page = 0;
  protected predicate!: string;
  protected ngbPaginationPage = 1;
  protected readonly itemsPerPage = 10;
  protected fromDate: NgbDateStruct | null = null;
  protected toDate: NgbDateStruct | null = null;
  protected fromTime: Date | undefined;
  protected toTime: Date | undefined;
  protected order = "ASC";
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
  protected typeOptions = this.types.map(type => ({label: type, value: type}));
  protected selectedTypes: TypeFinancialTransaction[] = [];
  protected paymentModes: IPaymentMode[] = [];

  private readonly userService = inject(UserService);
  private readonly mvtCaisseService = inject(MvtCaisseServiceService);
  private readonly modeService = inject(ModePaymentService);
  private readonly mvtParamServiceService = inject(MvtParamServiceService);
  private readonly modalService = inject(NgbModal);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private destroy$ = new Subject<void>();
  private readonly blobDownloadService = inject(BlobDownloadService);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);

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


  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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

  printReceiptForTauri(paymentId: PaymentId): void {
    this.mvtCaisseService.getEscPosReceiptForTauri(paymentId).subscribe({
      next: async (escposData: ArrayBuffer) => {
        try {
          await this.tauriPrinterService.printEscPosFromBuffer(escposData);
        } catch (error) {
        }
      },
      error() {
      }
    });
  }

  protected lazyLoading(event: AppTableLazyLoadEvent): void {
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
      next: blob => {
        this.btnLoading = false;
        this.blobDownloadService.downloadPdf(blob, "visualisation-mouvements-caisse");

      },
      error: () => {
        this.btnLoading = false;
        this.notificationService.error("Erreur", "Une erreur est survenue lors de l'exportation");
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
      {header: "FORMULAIRE D'AJOUT DE MOUVEMENT DE CAISSE"},
      (paymentId: PaymentId) => {
        if (paymentId) {
          this.onPrintReceipt(paymentId);
          this.onSearch();
        }
      },
      "lg"
    );
  }

  private onSuccess(data: MvtCaisse[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get("X-Total-Count"));
    this.page = page;

    this.mvtCaisses = data || [];
    this.loading = false;
  }

  private buildParams(): any {
    return {
      fromDate: this.fromDate ? NGB_DATE_TO_ISO(this.fromDate) : null,
      toDate: this.toDate ? NGB_DATE_TO_ISO(this.toDate) : null,
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

  private onPrintReceipt(paymentId: PaymentId): void {

    this.confirmDialog.onConfirm(
      () => {
        if (this.tauriPrinterService.isRunningInTauri()) {
          this.printReceiptForTauri(paymentId);
        } else {
          this.mvtCaisseService.printReceipt(paymentId).pipe(takeUntil(this.destroy$)).subscribe();
        }
      },
      "TICKET REGLEMENT",
      "Voullez-vous imprimer le ticket ?"
    );

  }
}
