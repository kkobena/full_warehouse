import { Component, inject, OnDestroy, OnInit } from "@angular/core";
import { ToolbarModule } from "primeng/toolbar";
import { FormsModule } from "@angular/forms";
import { ButtonModule } from "primeng/button";
import { TableLazyLoadEvent, TableModule } from "primeng/table";
import { TooltipModule } from "primeng/tooltip";
import { MvtCaisse, MvtCaisseWrapper, TypeFinancialTransaction } from "../cash-register/model/cash-register.model";
import { MvtCaisseServiceService } from "./mvt-caisse-service.service";
import { HttpHeaders, HttpResponse } from "@angular/common/http";
import { InputTextModule } from "primeng/inputtext";
import { IPaymentMode } from "../../shared/model/payment-mode.model";
import { IUser } from "../../core/user/user.model";
import { ModePaymentService } from "../mode-payments/mode-payment.service";
import { MultiSelectModule } from "primeng/multiselect";
import { FormTransactionComponent } from "./form-transaction/form-transaction.component";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { DATE_FORMAT_ISO_DATE } from "../../shared/util/warehouse-util";
import { getTypeName, MvtCaisseParams } from "./mvt-caisse-util";
import { ButtonGroupModule } from "primeng/buttongroup";
import { DividerModule } from "primeng/divider";
import { UserService } from "../../core/user/user.service";
import { CardModule } from "primeng/card";
import { MvtParamServiceService } from "./mvt-param-service.service";
import { DatePicker } from "primeng/datepicker";
import { Select } from "primeng/select";
import { FloatLabel } from "primeng/floatlabel";
import { showCommonModal } from "../sales/selling-home/sale-helper";
import { TauriPrinterService } from "../../shared/services/tauri-printer.service";
import { PaymentId } from "../reglement/model/reglement.model";
import { takeUntil } from "rxjs/operators";
import { Subject } from "rxjs";
import { BlobDownloadService } from "../../shared/services/blob-download.service";
import { Toast } from "primeng/toast";
import { CommonModule } from "@angular/common";
import { NotificationService } from "../../shared/services/notification.service";
import { NgbConfirmDialogService } from "../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";

@Component({
  selector: "jhi-visualisation-mvt-caisse",
  imports: [
    CommonModule,
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
    Toast
  ],
  templateUrl: "./visualisation-mvt-caisse.component.html",
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
  protected fromDate: Date | undefined;
  protected toDate: Date | undefined;
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
      { header: "FORMULAIRE D'AJOUT DE MOUVEMENT DE CAISSE" },
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
}
