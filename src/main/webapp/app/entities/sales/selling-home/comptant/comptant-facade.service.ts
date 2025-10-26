import {inject, Injectable} from '@angular/core';
import {Observable, Subject} from 'rxjs';
import {finalize, switchMap} from 'rxjs/operators';
import {HttpResponse} from '@angular/common/http';
import {
  FinalyseSale,
  ISales,
  SaleId,
  Sales,
  SaveResponse
} from '../../../../shared/model/sales.model';
import {ISalesLine, SaleLineId} from '../../../../shared/model/sales-line.model';
import {IRemise} from '../../../../shared/model/remise.model';
import {SalesService} from '../../sales.service';
import {CurrentSaleService} from '../../service/current-sale.service';
import {SelectedCustomerService} from '../../service/selected-customer.service';
import {TypePrescriptionService} from '../../service/type-prescription.service';
import {UserCaissierService} from '../../service/user-caissier.service';
import {UserVendeurService} from '../../service/user-vendeur.service';
import {TauriPrinterService} from '../../../../shared/services/tauri-printer.service';

@Injectable({
  providedIn: 'root',
})
export class ComptantFacadeService {
  private readonly salesService = inject(SalesService);
  private readonly currentSaleService = inject(CurrentSaleService);
  private readonly selectedCustomerService = inject(SelectedCustomerService);
  private readonly typePrescriptionService = inject(TypePrescriptionService);
  private readonly userCaissierService = inject(UserCaissierService);
  private readonly userVendeurService = inject(UserVendeurService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private saveResponseSubject = new Subject<SaveResponse>();
  saveResponse$ = this.saveResponseSubject.asObservable();
  private finalyseSaleSubject = new Subject<FinalyseSale>();
  finalyseSale$ = this.finalyseSaleSubject.asObservable();

  private spinnerService = new Subject<boolean>();
  spinnerService$ = this.spinnerService.asObservable();

  private openUninsuredCustomerSubject = new Subject<{
    isVenteDefferee: boolean;
    putsOnStandby: boolean;
  }>();
  openUninsuredCustomer$ = this.openUninsuredCustomerSubject.asObservable();

  confirmDiffereSale(entryAmount: number): void {
    this.currentSaleService.currentSale().differe = true;
    if (!this.currentSaleService.currentSale().customerId) {
      this.openUninsuredCustomerSubject.next({ isVenteDefferee: true, putsOnStandby: false });
    } else {
      this.finalizeSale(
        false,
        entryAmount,
        this.currentSaleService.currentSale().commentaire,
        this.currentSaleService.currentSale().avoir,
        this.currentSaleService.currentSale().payments,
      );
    }
  }

  finalizeSale(putsOnStandby: boolean, entryAmount: number, commentaire: string, avoir: boolean, payments: any): void {
    const currentSale = this.currentSaleService.currentSale();
    currentSale.payments = payments;
    currentSale.type = 'VNO';
    currentSale.avoir = avoir;
    currentSale.commentaire = commentaire;

    if (currentSale.avoir && !currentSale.customerId) {
      this.openUninsuredCustomerSubject.next({ isVenteDefferee: false, putsOnStandby });
      return;
    }

    if (putsOnStandby) {
      this.putCurrentCashSaleOnHold();
    } else {
      console.log('saving cash sale with entry amount:', entryAmount);
      this.saveCashSale(entryAmount);
    }
  }

  createComptant(salesLine: ISalesLine): void {
    this.spinnerService.next(true);
    this.salesService
      .createComptant(this.createSaleComptant(salesLine))
      .pipe(finalize(() => this.spinnerService.next(false)))
      .subscribe({
        next: (res: HttpResponse<ISales>) => this.onSaleComptantResponseSuccess(res.body),
        error: error => this.onSaveSaveError(error, this.currentSaleService.currentSale()),
      });
  }

  addItemToSale(salesLine: ISalesLine): void {
    const sale = this.currentSaleService.currentSale();
    this.handleSaleUpdate(
      this.salesService
        .addItemComptant({
          ...salesLine,
          saleCompositeId: sale.saleId,
        })
        .pipe(switchMap(res => this.salesService.find(sale.saleId))),
    );
  }

  removeItemFromSale(id: SaleLineId): void {
    const sale = this.currentSaleService.currentSale();
    this.handleSaleUpdate(this.salesService.deleteItem(id).pipe(switchMap(() => this.salesService.find(sale.saleId))));
  }

  updateItemQtyRequested(salesLine: ISalesLine): void {
    const sale = this.currentSaleService.currentSale();
    this.handleSaleUpdate(
      this.salesService
        .updateItemQtyRequested({
          ...salesLine,
          saleCompositeId: sale.saleId,
        })
        .pipe(
          switchMap(() => {
            return this.salesService.find(sale.saleId);
          }),
        ),
      salesLine,
    );
  }

  updateItemQtySold(salesLine: ISalesLine): void {
    const sale = this.currentSaleService.currentSale();
    this.handleSaleUpdate(
      this.salesService
        .updateItemQtySold({
          ...salesLine,
          saleCompositeId: sale.saleId,
        })
        .pipe(switchMap(() => this.salesService.find(sale.saleId))),
    );
  }

  updateItemPrice(salesLine: ISalesLine): void {
    const sale = this.currentSaleService.currentSale();
    this.handleSaleUpdate(
      this.salesService
        .updateItemPrice({
          ...salesLine,
          saleCompositeId: sale.saleId,
        })
        .pipe(switchMap(() => this.salesService.find(sale.saleId))),
    );
  }

  updateRemise(remise?: IRemise): void {
    const sale = this.currentSaleService.currentSale();
    const action$ = remise
      ? this.salesService.addRemise({ id: sale.saleId, value: remise.id })
      : this.salesService.removeRemiseFromCashSale(sale.saleId);
    this.handleSaleUpdate(action$.pipe(switchMap(() => this.salesService.find(sale.saleId))));
  }

  printInvoice(saleId: SaleId): void {
    this.spinnerService.next(true);
    this.salesService
      .printInvoice(saleId)
      .pipe(finalize(() => this.spinnerService.next(false)))
      .subscribe(blob => {
        const blobUrl = URL.createObjectURL(blob);
        window.open(blobUrl);
      });
  }

  printReceipt(saleId: SaleId): void {
    this.spinnerService.next(true);
    this.salesService
      .printReceipt(saleId)
      .pipe(finalize(() => this.spinnerService.next(false)))
      .subscribe();
  }

  /**
   * Print receipt for Tauri clients using ESC/POS
   * Gets receipt as ESC/POS commands and prints directly to thermal printer
   * Much more efficient than PNG method (2-5 KB vs 200-500 KB)
   * @param saleId Sale ID and date
   * @param isEdition Whether this is a reprint (affects number of copies)
   */
  printReceiptForTauri(saleId: SaleId, isEdition: boolean = false): void {
    this.spinnerService.next(true);
    this.salesService
      .getEscPosReceiptForTauri(saleId, isEdition)
      .pipe(finalize(() => this.spinnerService.next(false)))
      .subscribe({
        next: async (escposData: ArrayBuffer) => {
          try {
            await this.tauriPrinterService.printEscPosFromBuffer(escposData);
            console.log('ESC/POS receipt printed successfully');
          } catch (error) {
            console.error('Error printing ESC/POS receipt:', error);
            this.onSaveError(error);
          }
        },
        error: err => {
          console.error('Error getting ESC/POS receipt for Tauri:', err);
          this.onSaveError(err);
        },
      });
  }

  onAddUninsuredCustomer(): void {
    this.saveResponseSubject.next({ success: true });
  }

  private onSaveSuccess(sale: ISales | null): void {
    this.currentSaleService.setCurrentSale(sale);
    this.saveResponseSubject.next({ success: true });
  }

  private onSaveError(err: any): void {
    this.saveResponseSubject.next({ success: false, error: err });
  }

  private onSaveSaveError(err: any, sale?: ISales, payload: any = null): void {
    this.saveResponseSubject.next({ success: false, error: err, payload });
    this.currentSaleService.setCurrentSale(sale);
  }

  private onFinalyseSuccess(response: FinalyseSale | null, putOnStandBy = false): void {
    this.finalyseSaleSubject.next({ saleId: response.saleId, success: true, putOnStandBy });
  }

  private onFinalyseError(err: any): void {
    this.finalyseSaleSubject.next({ error: err, success: false });
  }

  private onSaleComptantResponseSuccess(sale: ISales | null): void {
    this.currentSaleService.setCurrentSale(sale);
    this.saveResponseSubject.next({ success: true });
  }

  private saveCashSale(entryAmount: number): void {
    this.spinnerService.next(true);
    const currentSale = this.currentSaleService.currentSale();
    this.updateSaleAmounts(currentSale, entryAmount);
    this.salesService
      .saveCash(currentSale)
      .pipe(finalize(() => this.spinnerService.next(false)))
      .subscribe({
        next: (res: HttpResponse<FinalyseSale>) => this.onFinalyseSuccess(res.body),
        error: err => this.onFinalyseError(err),
      });
  }

  private putCurrentCashSaleOnHold(): void {
    this.salesService
      .putCurrentCashSaleOnStandBy(this.currentSaleService.currentSale())
      .pipe(finalize(() => this.spinnerService.next(false)))
      .subscribe({
        next: (res: HttpResponse<FinalyseSale>) => this.onFinalyseSuccess(res.body, true),
        error: err => this.onFinalyseError(err),
      });
  }

  private handleSaleUpdate(observable: Observable<HttpResponse<ISales>>, payload: any = null): void {
    this.spinnerService.next(true);
    observable.pipe(finalize(() => this.spinnerService.next(false))).subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body),
      error: err => this.onSaveSaveError(err, this.currentSaleService.currentSale(), payload),
    });
  }

  private updateSaleAmounts(sale: ISales, entryAmount: number): void {
    const numericEntryAmount = Number(entryAmount) || 0;
    const restToPay = sale.amountToBePaid - numericEntryAmount;
    sale.payrollAmount = restToPay <= 0 ? sale.amountToBePaid : numericEntryAmount;
    sale.restToPay = restToPay <= 0 ? 0 : restToPay;
    sale.montantRendu = sale.montantVerse - sale.amountToBePaid;
  }

  private createSaleComptant(salesLine: ISalesLine): ISales {
    let currentCustomer = this.selectedCustomerService.selectedCustomerSignal();
    if (currentCustomer && currentCustomer.type === 'ASSURE') {
      currentCustomer = null;
    }
    return {
      ...new Sales(),
      salesLines: [salesLine],
      customerId: currentCustomer?.id,
      natureVente: 'COMPTANT',
      typePrescription: this.typePrescriptionService.typePrescription()?.code,
      cassierId: this.userCaissierService.caissier()?.id,
      sellerId: this.userVendeurService.vendeur()?.id,
      type: 'VNO',
      categorie: 'VNO',
    };
  }
}
