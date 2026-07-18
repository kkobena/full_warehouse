import { Component, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { CommonModule, DatePipe } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { HttpHeaders } from "@angular/common/http";
import { Router, RouterLink } from "@angular/router";
import { Button } from "primeng/button";
import { TableLazyLoadEvent, TableModule } from "primeng/table";
import { Toolbar } from "primeng/toolbar";
import { Select } from "primeng/select";
import { MultiSelect } from "primeng/multiselect";
import { DatePicker } from "primeng/datepicker";
import { InputText } from "primeng/inputtext";
import { Checkbox } from "primeng/checkbox";
import { TooltipModule } from "primeng/tooltip";
import { Menu, MenuModule } from "primeng/menu";
import { MenuItem } from "primeng/api";
import { finalize, Subject } from "rxjs";
import { debounceTime } from "rxjs/operators";
import { ITEMS_PER_PAGE } from "../../../../shared/constants/pagination.constants";
import { ISales, SalesStatut } from "../../../../shared/model";
import { IUser } from "../../../../core/user/user.model";
import { UserService } from "../../../../core/user/user.service";
import { SalesApiService } from "../../data-access/services/sales-api.service";
import { SaleToolbarService } from "../../data-access/services/sale-toolbar.service";
import { NotificationService } from "../../../../shared/services/notification.service";
import { TauriPrinterService } from "../../../../shared/services/tauri-printer.service";
import { ErrorService } from "../../../../shared/error.service";
import { handleBlobForTauri } from "../../../../shared/util/tauri-util";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";
import { FloatLabel } from "primeng/floatlabel";
import { InputGroup } from "primeng/inputgroup";
import { InputGroupAddon } from "primeng/inputgroupaddon";
import { NgxSpinnerComponent } from "ngx-spinner";
import { Toast } from "primeng/toast";
import { showCommonModal } from "../../../../entities/sales/selling-home/sale-helper";
import {
  CustomerEditModalComponent
} from "../../../../entities/sales/customer-edit-modal/customer-edit-modal.component";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import {
  SaleUpdateDateModalComponent
} from "../../../../entities/sales/sale-update-date-modal/sale-update-date-modal.component";
import { PrimeNG } from "primeng/config";
import { TranslateService } from "@ngx-translate/core";
import { TIMES } from "../../../../shared/util/times";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { AnnulationVenteMessageComponent } from "../../ui/annulation-vente-message/annulation-vente-message.component";
import { AbilityService } from "../../../../core/auth/ability.service";
import { RetourClientModalComponent } from "../../ui/retour-client-modal/retour-client-modal.component";
import { CloturerAvoirModalComponent } from "../../ui/cloturer-avoir-modal/cloturer-avoir-modal.component";
import { AvoirClientApiService } from "../../data-access/services/avoir-client-api.service";


@Component({
  selector: "app-sales-journal",
  templateUrl: "./sales-journal.component.html",
  styleUrl: "./sales-journal.component.scss",
  providers: [DatePipe],
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    Button,
    TableModule,
    Toolbar,
    Select,
    MultiSelect,
    DatePicker,
    InputText,
    Checkbox,
    TooltipModule,
    FloatLabel,
    InputGroup,
    InputGroupAddon,
    NgxSpinnerComponent,
    Toast,
    RouterLink,
    MenuModule
  ]
})
export class SalesJournalComponent implements OnInit {
  private readonly api = inject(SalesApiService);
  private readonly userService = inject(UserService);
  private readonly toolbarService = inject(SaleToolbarService);
  private readonly router = inject(Router);
  private readonly tauriPrinter = inject(TauriPrinterService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly datePipe = inject(DatePipe);
  private readonly modalService = inject(NgbModal);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly translate = inject(TranslateService);
  private readonly ability = inject(AbilityService);
  private readonly blobDownload = inject(BlobDownloadService);
  private readonly avoirApi = inject(AvoirClientApiService);

  // ── Abilities ─────────────────────────────────────────
  protected readonly canCancelVente = this.ability.canSignal("execute", "ventes.journal.cancel");
  protected readonly canEditVente = this.ability.canSignal("execute", "pr-modifier-vente");
  protected readonly canRetourClient = this.ability.canSignal("execute", "ventes.retours-client.create");
  protected readonly canCloturerAvoir = this.ability.canSignal("execute", "ventes.avoirs.cloturer");

  // ── État ──────────────────────────────────────────────
  protected loading = signal(false);
  protected exportLoading = signal(false);
  protected sales: ISales[] = [];
  protected users: IUser[] = [];
  protected totalItems = 0;
  protected page = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected useSimpleSale = false;
  // ── Filtres ───────────────────────────────────────────
  protected readonly typeVentes = [
    { label: "Comptant", value: "COMPTANT" },
    { label: "Assurance", value: "ASSURANCE" },
    { label: "Carnet", value: "CARNET" }
  ];
  protected typeVenteSelected: string[] = [];
  protected search = "";
  protected global = true;
  protected selectedUserId: number | null = null;
  protected selectedCassierId: number | null = null;
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();
  protected fromHour = "01:00";
  protected toHour = "23:59";
  protected hours = TIMES;
  // ── Permissions ───────────────────────────────────────
  protected readonly SalesStatut = SalesStatut;
  private readonly searchSubject = new Subject<void>();

  // ── Menu contextuel ───────────────────────────────────
  protected menuItems = signal<MenuItem[]>([]);
  private currentSale: ISales | null = null;

  protected openContextMenu(event: Event, sale: ISales, menu: Menu): void {
    event.stopPropagation();
    this.currentSale = sale;
    this.menuItems.set(this.buildMenuItems(sale));
    menu.toggle(event);
  }
  protected readonly totalSalesAmount = signal(0);
  private buildMenuItems(sale: ISales): MenuItem[] {
    const canceled = sale.canceled || sale.statut === SalesStatut.CANCELED;
    const isAssuranceOrCarnet = sale.natureVente === "ASSURANCE" || sale.natureVente === "CARNET";
    const items: MenuItem[] = [];

    if (!canceled) {
      items.push({
        label: "Imprimer ticket",
        icon: "pi pi-print",
        command: () => this.reprintReceipt(this.currentSale!)
      });
      if (sale.customer) {
        items.push({
          label: "Imprimer facture",
          icon: "pi pi-receipt",
          command: () => this.printInvoice(this.currentSale!)
        });
      }
      if (this.canRetourClient() || this.canCloturerAvoir()) {
        items.push({ separator: true });
      }
      if (this.canRetourClient()) {
        items.push({
          label: "Retour client",
          icon: "pi pi-undo",
          command: () => this.openRetourFromSale(this.currentSale!)
        });
      }
      if (this.canCloturerAvoir() ) {
        items.push({
          label: "Clôturer avoir",
          icon: "pi pi-ticket",
          command: () => this.openAvoirsForSale(this.currentSale!)
        });
      }
    }

    if (this.canEditVente() && !canceled) {
      items.push({ separator: true });
      if (isAssuranceOrCarnet) {
        items.push({
          label: "Éditer la vente",
          icon: "pi pi-file-edit",
          command: () => this.confirmEdit(this.currentSale!)
        });
      }
      if (isAssuranceOrCarnet) {
        items.push({
          label: "Modifier le client",
          icon: "pi pi-user-edit",
          command: () => this.onEditCustomer(this.currentSale!)
        });
      }
      items.push({
        label: "Modifier la date",
        icon: "pi pi-calendar-plus",
        command: () => this.editSaleUpdatedDate(this.currentSale!)
      });
    }

    if (this.canCancelVente() && !canceled) {
      items.push({ separator: true });
      items.push({
        label: "Annuler la vente",
        icon: "pi pi-trash",
        styleClass: "menu-item-danger",
        command: () => this.confirmCancel(this.currentSale!)
      });
    }

    return items;
  }

  protected openRetourFromSale(sale: ISales): void {
    const ref = this.modalService.open(RetourClientModalComponent, { centered: true, size: "xl", backdrop: "static" });
    ref.componentInstance.sale = sale;
  }

  protected openAvoirsForSale(sale: ISales): void {
    this.avoirApi.queryDocuments({ search: sale.numberTransaction, statut: "OUVERT", size: 1, page: 0 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          const docs = res.body ?? [];
          if (docs.length === 0) {
            this.notificationService.warning("Aucun avoir ouvert pour cette vente", "Avoirs client");
            return;
          }
          const modalRef = this.modalService.open(CloturerAvoirModalComponent, {
            centered: true,
            size: "lg",
            backdrop: "static"
          });
          modalRef.componentInstance.document = docs[0];
        }
      });
  }



  ngOnInit(): void {
    this.translate.use("fr");
    this.translate.stream("primeng").subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });

    this.loadAllUsers();
    this.restoreParams();
    this.loadPage();

    this.searchSubject.pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef)).subscribe(() => this.loadPage());
  }

  protected onTypeVenteChange(): void {
    this.searchSubject.next();
  }

  private restoreParams(): void {
    const p = this.toolbarService.params();
    this.typeVenteSelected = p.typeVente ?? [];
    this.search = p.search || "";
    this.global = p.global ?? true;
    this.selectedUserId = p.selectedUserId;
    this.selectedCassierId = p.selectedCassierId;
    this.fromDate = p.fromDate || new Date();
    this.toDate = p.toDate || new Date();
    this.fromHour = p.fromHour || "01:00";
    this.toHour = p.toHour || "23:59";
  }

  private loadAllUsers(): void {
    this.userService.query().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(res => {
      this.users = res.body || [];
    });
  }

  protected openNewSalesHome(): void {
    this.router.navigate(["/sales-home"]);
  }

  editSaleUpdatedDate(sale: ISales): void {
    if (sale) {
      showCommonModal(
        this.modalService,
        SaleUpdateDateModalComponent,
        { sale },
        (updatedSale: ISales) => {
          if (updatedSale) {
            this.loadPage();
          }
        },
        "45%"
      );
    }
  }

  protected loadPage(page?: number): void {
    const pageToLoad = page ?? this.page;
    this.fetchSales(pageToLoad, this.itemsPerPage);
    this.saveParams();
  }

  protected suggerer(sales: ISales): void {
  }
  private buildParams(): any {
    return {
      search: this.search || null,
      types: this.typeVenteSelected,
      fromDate: this.datePipe.transform(this.fromDate, "yyyy-MM-dd"),
      toDate: this.datePipe.transform(this.toDate, "yyyy-MM-dd"),
      fromHour: this.fromHour,
      toHour: this.toHour,
      global: this.global,
      userId: this.selectedUserId
    };
  }
  private fetchSales(page: number, size: number): void {
    this.loading.set(true);
    const params = this.buildParams();
    this.api
      .querySales({page, size, ...params})
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loading.set(false);
          this.onSuccess(res.body, res.headers, page);
        },
        error: () => this.loading.set(false)
      });
    this.api
      .querySalesTotalAmount(params)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: total => this.totalSalesAmount.set(total),
        error: () => {}
      });
  }

  private onSuccess(data: ISales[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get("X-Total-Count"));
    this.page = page;
    this.sales = data || [];
    this.loading.set(false);
  }

  private saveParams(): void {
    this.toolbarService.update({
      typeVente: this.typeVenteSelected,
      search: this.search || null,
      global: this.global,
      selectedUserId: this.selectedUserId,
      selectedCassierId: this.selectedCassierId,
      fromDate: this.fromDate,
      toDate: this.toDate,
      fromHour: this.fromHour,
      toHour: this.toHour
    });
  }

  protected lazyLoading(event: TableLazyLoadEvent): void {
    if (event.first != null && event.rows != null) {
      this.page = event.first / event.rows;
      this.itemsPerPage = event.rows;
      this.fetchSales(this.page, this.itemsPerPage);
    }
  }

  protected onRowExpand(event: any): void {
    if (event.data?.saleId && !event.data._loaded) {
      this.api.findSale(event.data.saleId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(detail => {
        Object.assign(event.data, detail, { _loaded: true });
      });
    }
  }

  protected onSearch(): void {
    this.searchSubject.next();
  }

  // ── Actions ───────────────────────────────────────────

  protected confirmCancel(sale: ISales): void {
    this.confirmDialog.onConfirm(
      () => this.cancelSale(sale),
      "Annulation de vente",
      "Voulez-vous vraiment annuler cette vente ?"
    );
  }

  private cancelSale(sale: ISales): void {
    if (!sale.saleId) return;
    showCommonModal(
      this.modalService,
      AnnulationVenteMessageComponent,
      {
        sale
      },
      () => {
        this.loadPage();
      },
      "xl"
    );

  }

  protected printInvoice(sale: ISales): void {
    if (!sale.saleId) return;
    this.api.printInvoice(sale.saleId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(blob => {
      if (this.tauriPrinter.isRunningInTauri()) {
        handleBlobForTauri(blob, `facture-${sale.numberTransaction}`);
      } else {
        window.open(URL.createObjectURL(blob));
      }
    });
  }

  protected reprintReceipt(sale: ISales): void {
    if (!sale.saleId) return;
    if (this.tauriPrinter.isRunningInTauri()) {
      this.api.getEscPosReceiptForTauri(sale.saleId, true).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: async escpos => {
          try {
            await this.tauriPrinter.printEscPosFromBuffer(escpos);
          } catch { /* ignore */
          }
        }
      });
    } else {
      const reprint$ = sale.categorie === "VNO"
        ? this.api.reprintReceiptComptant(sale.saleId)
        : this.api.reprintReceiptAssurance(sale.saleId);
      reprint$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
    }
  }

  protected confirmEdit(sale: ISales): void {
    this.confirmDialog.onConfirm(
      () => this.editSale(sale),
      "Modification de vente",
      "La vente sera annulée puis recréée. Voulez-vous continuer ?"
    );
  }

  protected onEditCustomer(currSale: ISales): void {
    showCommonModal(
      this.modalService,
      CustomerEditModalComponent,
      {
        sale: currSale
      },
      () => {
        this.loadPage();
      },
      "xl"
    );
  }

  private editSale(sale: ISales): void {
    this.loading.set(true);
    this.api
      .copyToEdit(sale.saleId!)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: res => {
          const saleId = res.body;
          this.router.navigate(["/sales-home"], { state: { saleInfo: { saleId, isEdit: true } } });
        },
        error: err => {
          this.notificationService.error(this.errorService.getErrorMessage(err), "Modification de vente");
        }
      });
  }

  protected exportJournal(): void {
    const fileName = `journal-ventes-${this.datePipe.transform(this.fromDate, "yyyyMMdd")}-${this.datePipe.transform(this.toDate, "yyyyMMdd")}`;
    this.blobDownload.downloadFromObservable(
      this.api.exportJournal({
        search: this.search || null,
        types: this.typeVenteSelected,
        fromDate: this.datePipe.transform(this.fromDate, "yyyy-MM-dd"),
        toDate: this.datePipe.transform(this.toDate, "yyyy-MM-dd"),
        fromHour: this.fromHour,
        toHour: this.toHour,
        global: this.global,
        userId: this.selectedUserId,
        caissierUserId: this.selectedCassierId
      }),
      fileName,
      "excel",
      () => this.exportLoading.set(true),
      () => this.exportLoading.set(false),
      () => this.notificationService.error("Erreur lors de l'export", "Export journal")
    );
  }

}
