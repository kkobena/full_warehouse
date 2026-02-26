import {Component, DestroyRef, inject, OnInit, signal, viewChild} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {CommonModule, DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {HttpHeaders} from '@angular/common/http';
import {Router, RouterLink} from '@angular/router';
import {Button} from 'primeng/button';
import {TableLazyLoadEvent, TableModule} from 'primeng/table';
import {Toolbar} from 'primeng/toolbar';
import {Select} from 'primeng/select';
import {DatePicker} from 'primeng/datepicker';
import {InputText} from 'primeng/inputtext';
import {Checkbox} from 'primeng/checkbox';
import {TooltipModule} from 'primeng/tooltip';
import {finalize, Subject} from 'rxjs';
import {debounceTime} from 'rxjs/operators';

import {ITEMS_PER_PAGE} from '../../../../shared/constants/pagination.constants';
import {ISales} from '../../../../shared/model';
import {IUser} from '../../../../core/user/user.model';
import {Authority} from '../../../../shared/constants/authority.constants';
import {UserService} from '../../../../core/user/user.service';
import {SalesApiService} from '../../data-access/services/sales-api.service';
import {SaleToolbarService} from '../../data-access/services/sale-toolbar.service';
import {NotificationService} from '../../../../shared/services/notification.service';
import {ConfirmDialogComponent} from '../../../../shared/dialog/confirm-dialog/confirm-dialog.component';
import {HasAuthorityService} from "../../../../entities/sales/service/has-authority.service";
import {TauriPrinterService} from "../../../../shared/services/tauri-printer.service";
import {ErrorService} from "../../../../shared/error.service";
import {handleBlobForTauri} from "../../../../shared/util/tauri-util";
import {ButtonGroup} from "primeng/buttongroup";
import {FloatLabel} from "primeng/floatlabel";
import {InputGroup} from "primeng/inputgroup";
import {InputGroupAddon} from "primeng/inputgroupaddon";
import {NgxSpinnerComponent} from "ngx-spinner";
import {Toast} from "primeng/toast";
import {SalesStatut} from "../../models/enumerations/sales-statut.enum";
import {showCommonModal} from "../../../../entities/sales/selling-home/sale-helper";
import {CustomerEditModalComponent} from "../../../../entities/sales/customer-edit-modal/customer-edit-modal.component";
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {
  SaleUpdateDateModalComponent
} from "../../../../entities/sales/sale-update-date-modal/sale-update-date-modal.component";
import {PrimeNG} from "primeng/config";
import {TranslateService} from "@ngx-translate/core";
import {TIMES} from "../../../../shared/util/times";


@Component({
  selector: 'app-sales-journal',
  templateUrl: './sales-journal.component.html',
  styleUrl: './sales-journal.component.scss',
  providers: [DatePipe],
  imports: [
    CommonModule,
    FormsModule,
    Button,
    TableModule,
    Toolbar,
    Select,
    DatePicker,
    InputText,
    Checkbox,
    TooltipModule,
    ButtonGroup,
    ConfirmDialogComponent,
    FloatLabel,
    InputGroup,
    InputGroupAddon,
    NgxSpinnerComponent,
    Toast,
    RouterLink,

  ],
})
export class SalesJournalComponent implements OnInit {
  private readonly api = inject(SalesApiService);
  private readonly userService = inject(UserService);
  private readonly toolbarService = inject(SaleToolbarService);
  private readonly hasAuthorityService = inject(HasAuthorityService);
  private readonly router = inject(Router);
  private readonly tauriPrinter = inject(TauriPrinterService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly datePipe = inject(DatePipe);
  private readonly modalService = inject(NgbModal);
  protected readonly confirmDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly translate = inject(TranslateService);

  // ── État ──────────────────────────────────────────────
  protected loading = signal(false);
  protected sales: ISales[] = [];
  protected users: IUser[] = [];
  protected totalItems = 0;
  protected page = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected useSimpleSale = false;
  // ── Filtres ───────────────────────────────────────────
  protected typeVentes = ['TOUT', 'VNO', 'VO'];
  protected typeVenteSelected = 'TOUT';
  protected search = '';
  protected global = true;
  protected selectedUserId: number | null = null;
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();
  protected fromHour = '01:00';
  protected toHour = '23:59';
  protected hours = TIMES;
  // ── Permissions ───────────────────────────────────────
  protected canEdit = false;
  protected canCancel = false;

  private readonly searchSubject = new Subject<void>();

  ngOnInit(): void {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
    this.canEdit = this.hasAuthorityService.hasAuthorities(Authority.PR_MODIFICATION_VENTE);
    this.canCancel = this.hasAuthorityService.hasAuthorities(Authority.PR_ANNULATION_VENTE);
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
    this.typeVenteSelected = p.typeVente || 'TOUT';
    this.search = p.search || '';
    this.global = p.global ?? true;
    this.selectedUserId = p.selectedUserId;
    this.fromDate = p.fromDate || new Date();
    this.toDate = p.toDate || new Date();
    this.fromHour = p.fromHour || '01:00';
    this.toHour = p.toHour || '23:59';
  }

  private loadAllUsers(): void {
    this.userService.query().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(res => {
      this.users = res.body || [];
    });
  }

  protected openNewSalesHome(): void {
    this.router.navigate(['/sales-home']);
  }

  editSaleUpdatedDate(sale: ISales): void {
    if (sale) {
      showCommonModal(
        this.modalService,
        SaleUpdateDateModalComponent,
        {sale},
        (updatedSale: ISales) => {
          if (updatedSale) {
            this.loadPage();
          }
        },
        '45%',
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

  private fetchSales(page: number, size: number): void {
    this.loading.set(true);
    this.api
      .querySales({
        page,
        size,
        search: this.search || null,
        type: this.typeVenteSelected,
        fromDate: this.datePipe.transform(this.fromDate, 'yyyy-MM-dd'),
        toDate: this.datePipe.transform(this.toDate, 'yyyy-MM-dd'),
        fromHour: this.fromHour,
        toHour: this.toHour,
        global: this.global,
        userId: this.selectedUserId,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.loading.set(false);
          this.onSuccess(res.body, res.headers, page);
        },
        error: () => this.loading.set(false),
      });
  }

  private onSuccess(data: ISales[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
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
      fromDate: this.fromDate,
      toDate: this.toDate,
      fromHour: this.fromHour,
      toHour: this.toHour,
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
        Object.assign(event.data, detail, {_loaded: true});
      });
    }
  }

  protected onSearch(): void {
    this.searchSubject.next();
  }

  // ── Actions ───────────────────────────────────────────

  protected confirmCancel(sale: ISales): void {
    this.confirmDialog().onConfirm(
      () => this.cancelSale(sale),
      'Annulation de vente',
      'Voulez-vous vraiment annuler cette vente ?',
    );
  }

  private cancelSale(sale: ISales): void {
    if (!sale.saleId) return;
    const cancel$ = sale.categorie === 'VNO'
      ? this.api.cancelComptant(sale.saleId)
      : this.api.cancelAssurance(sale.saleId);
    cancel$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.loadPage());
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
        },
      });
    } else {
      const reprint$ = sale.categorie === 'VNO'
        ? this.api.reprintReceiptComptant(sale.saleId)
        : this.api.reprintReceiptAssurance(sale.saleId);
      reprint$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
    }
  }

  protected confirmEdit(sale: ISales): void {
    if (!this.canEdit || !sale.saleId) return;
    this.confirmDialog().onConfirm(
      () => this.editSale(sale),
      'Modification de vente',
      'La vente sera annulée puis recréée. Voulez-vous continuer ?',
    );
  }

  protected onEditCustomer(currSale: ISales): void {
    showCommonModal(
      this.modalService,
      CustomerEditModalComponent,
      {
        sale: currSale,
      },
      () => {
        this.loadPage();
      },
      'xl',
    );
  }

  private editSale(sale: ISales): void {
    this.loading.set(true);
    this.api
      .copyToEdit(sale.saleId!)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: res => {
          const saleId = res.body;
          this.router.navigate(['/sales-home'], {state: {saleInfo: {saleId, isEdit: true}}});
        },
        error: err => {
          this.notificationService.error(this.errorService.getErrorMessage(err), 'Modification de vente');
        },
      });
  }


  protected readonly SalesStatut = SalesStatut;
}
