import { computed, inject, Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { finalize, switchMap } from 'rxjs/operators';
import { HttpResponse } from '@angular/common/http';

import { DepotService } from '../depot.service';
import { TauriPrinterService } from '../../../shared/services/tauri-printer.service';
import { FinalyseSale, ISales, SaleId, Sales, SaveResponse } from '../../../shared/model/sales.model';
import { ISalesLine, SaleLineId } from '../../../shared/model/sales-line.model';
import { IRemise } from '../../../shared/model/remise.model';

@Injectable({
  providedIn: 'root',
})
export class DepotFacadeService {
  private readonly depotService = inject(DepotService);

  private readonly tauriPrinterService = inject(TauriPrinterService);
  private saveResponseSubject = new Subject<SaveResponse>();
  saveResponse$ = this.saveResponseSubject.asObservable();
  private finalyseSaleSubject = new Subject<FinalyseSale>();
  finalyseSale$ = this.finalyseSaleSubject.asObservable();

  private spinnerService = new Subject<boolean>();
  spinnerService$ = this.spinnerService.asObservable();
  totalQtyProduit = computed(() => this.depotService.currentSale().salesLines.reduce((sum, current) => sum + current.quantityRequested, 0));
  totalQtyServi = computed(() => this.depotService.currentSale().salesLines.reduce((sum, current) => sum + current.quantitySold, 0));
  isAvoir(): boolean {
    return this.totalQtyProduit() - this.totalQtyServi() != 0;
  }

  finalizeSale(): void {
    const currentSale = this.depotService.currentSale();
    currentSale.payments = [];
    currentSale.type = 'DEPOT';
    currentSale.avoir = this.isAvoir();

    this.save();
  }

  create(salesLine: ISalesLine): void {
    this.spinnerService.next(true);
    this.depotService
      .create(this.createSale(salesLine))
      .pipe(finalize(() => this.spinnerService.next(false)))
      .subscribe({
        next: (res: HttpResponse<ISales>) => this.onSaleComptantResponseSuccess(res.body),
        error: error => this.onSaveSaveError(error, this.depotService.currentSale()),
      });
  }

  addItemToSale(salesLine: ISalesLine): void {
    const sale = this.depotService.currentSale();
    this.handleSaleUpdate(
      this.depotService
        .addItem({
          ...salesLine,
          saleCompositeId: sale.saleId,
        })
        .pipe(switchMap(res => this.depotService.find(sale.saleId))),
    );
  }

  removeItemFromSale(id: SaleLineId): void {
    const sale = this.depotService.currentSale();
    this.handleSaleUpdate(this.depotService.deleteItem(id).pipe(switchMap(() => this.depotService.find(sale.saleId))));
  }

  updateItemQtyRequested(salesLine: ISalesLine): void {
    const sale = this.depotService.currentSale();
    this.handleSaleUpdate(
      this.depotService
        .updateItemQtyRequested({
          ...salesLine,
          saleCompositeId: sale.saleId,
        })
        .pipe(
          switchMap(() => {
            return this.depotService.find(sale.saleId);
          }),
        ),
      salesLine,
    );
  }

  updateItemQtySold(salesLine: ISalesLine): void {
    const sale = this.depotService.currentSale();
    this.handleSaleUpdate(
      this.depotService
        .updateItemQtySold({
          ...salesLine,
          saleCompositeId: sale.saleId,
        })
        .pipe(switchMap(() => this.depotService.find(sale.saleId))),
    );
  }

  updateItemPrice(salesLine: ISalesLine): void {
    const sale = this.depotService.currentSale();
    this.handleSaleUpdate(
      this.depotService
        .updateItemPrice({
          ...salesLine,
          saleCompositeId: sale.saleId,
        })
        .pipe(switchMap(() => this.depotService.find(sale.saleId))),
    );
  }

  updateRemise(remise?: IRemise): void {
    const sale = this.depotService.currentSale();
    const action$ = remise
      ? this.depotService.addRemise({ id: sale.saleId, value: remise.id })
      : this.depotService.removeRemise(sale.saleId);
    this.handleSaleUpdate(action$.pipe(switchMap(() => this.depotService.find(sale.saleId))));
  }

  printInvoice(saleId: SaleId): void {
    this.spinnerService.next(true);
    this.depotService
      .printInvoice(saleId)
      .pipe(finalize(() => this.spinnerService.next(false)))
      .subscribe(blob => {
        const blobUrl = URL.createObjectURL(blob);
        window.open(blobUrl);
      });
  }

  printReceipt(saleId: SaleId): void {
    this.spinnerService.next(true);
    this.depotService
      .printReceipt(saleId)
      .pipe(finalize(() => this.spinnerService.next(false)))
      .subscribe();
  }

  printReceiptForTauri(saleId: SaleId, isEdition: boolean = false): void {
    this.spinnerService.next(true);
    this.depotService
      .getEscPosReceiptForTauri(saleId, isEdition)
      .pipe(finalize(() => this.spinnerService.next(false)))
      .subscribe({
        next: async (escposData: ArrayBuffer) => {
          try {
            await this.tauriPrinterService.printEscPosFromBuffer(escposData);
          } catch (error) {
            this.onSaveError(error);
          }
        },
        error: err => {
          this.onSaveError(err);
        },
      });
  }

  private onSaveSuccess(sale: ISales | null): void {
    this.depotService.setCurrentSale(sale);
    this.saveResponseSubject.next({ success: true });
  }

  private onSaveError(err: any): void {
    this.saveResponseSubject.next({ success: false, error: err });
  }

  private onSaveSaveError(err: any, sale?: ISales, payload: any = null): void {
    this.saveResponseSubject.next({ success: false, error: err, payload });
    this.depotService.setCurrentSale(sale);
  }

  private onFinalyseSuccess(response: FinalyseSale | null, putOnStandBy = false): void {
    this.finalyseSaleSubject.next({ saleId: response.saleId, success: true, putOnStandBy });
  }

  private onFinalyseError(err: any): void {
    this.finalyseSaleSubject.next({ error: err, success: false });
  }

  private onSaleComptantResponseSuccess(sale: ISales | null): void {
    this.depotService.setCurrentSale(sale);
    this.saveResponseSubject.next({ success: true });
  }

  private save(): void {
    this.spinnerService.next(true);
    const currentSale = this.depotService.currentSale();
    this.updateSaleAmounts(currentSale);
    this.depotService
      .save(currentSale)
      .pipe(finalize(() => this.spinnerService.next(false)))
      .subscribe({
        next: (res: HttpResponse<FinalyseSale>) => this.onFinalyseSuccess(res.body),
        error: err => this.onFinalyseError(err),
      });
  }

  private handleSaleUpdate(observable: Observable<HttpResponse<ISales>>, payload: any = null): void {
    this.spinnerService.next(true);
    observable.pipe(finalize(() => this.spinnerService.next(false))).subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body),
      error: err => this.onSaveSaveError(err, this.depotService.currentSale(), payload),
    });
  }

  private updateSaleAmounts(sale: ISales): void {
    sale.payrollAmount = sale.amountToBePaid;
    sale.restToPay = sale.amountToBePaid;
  }

  private createSale(salesLine: ISalesLine): ISales {
    return {
      ...new Sales(),
      salesLines: [salesLine],
      natureVente: 'ASSURANCE', //TODO A SUPPRIMER
      magasin: { id: this.depotService.selectedDepot()?.id },
      typePrescription: 'DEPOT',
      cassierId: this.depotService.caissier()?.id,
      sellerId: this.depotService.vendeur()?.id,
      type: 'DEPOT',
      categorie: 'VO',
    };
  }
}
