import { inject, Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { FinalyseSale, ISales, Sales, SaveResponse } from '../../../../shared/model/sales.model';
import { ISalesLine } from '../../../../shared/model/sales-line.model';
import { IRemise } from '../../../../shared/model/remise.model';
import { SalesService } from '../../sales.service';
import { CurrentSaleService } from '../../service/current-sale.service';
import { SelectedCustomerService } from '../../service/selected-customer.service';
import { TypePrescriptionService } from '../../service/type-prescription.service';
import { UserCaissierService } from '../../service/user-caissier.service';
import { UserVendeurService } from '../../service/user-vendeur.service';
import { SpinerService } from '../../../../shared/spiner.service';

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
  private readonly spinner = inject(SpinerService);

  private saveResponseSubject = new Subject<SaveResponse>();
  saveResponse$ = this.saveResponseSubject.asObservable();

  private finalyseSaleSubject = new Subject<FinalyseSale>();
  finalyseSale$ = this.finalyseSaleSubject.asObservable();

  private openUninsuredCustomerSubject = new Subject<{
    isVenteDefferee: boolean;
    putsOnStandby: boolean;
  }>();
  openUninsuredCustomer$ = this.openUninsuredCustomerSubject.asObservable();

  confirmDiffereSale(): void {
    this.currentSaleService.currentSale().differe = true;
    if (!this.currentSaleService.currentSale().customerId) {
      this.openUninsuredCustomerSubject.next({ isVenteDefferee: true, putsOnStandby: false });
    } else {
      this.finalizeSale(false, 0, this.currentSaleService.currentSale().commentaire, this.currentSaleService.currentSale().avoir, this.currentSaleService.currentSale().payments);
    }
  }

  finalizeSale(putsOnStandby: boolean, entryAmount: number, commentaire: string, avoir: boolean, payments: any): void {
    this.currentSaleService.currentSale().payments = payments;
    this.currentSaleService.currentSale().type = 'VNO';
    this.currentSaleService.currentSale().avoir = avoir;
    this.currentSaleService.currentSale().commentaire = commentaire;

    if (this.currentSaleService.currentSale().avoir && !this.currentSaleService.currentSale().customerId) {
      this.openUninsuredCustomerSubject.next({ isVenteDefferee: false, putsOnStandby });
      return;
    }

    if (putsOnStandby) {
      this.spinner.show();
      this.putCurrentCashSaleOnHold();
    } else {
      this.saveCashSale(entryAmount);
    }
  }

  computExtraInfo(commentaire: string): void {
    this.currentSaleService.currentSale().commentaire = commentaire;
  }

  openUninsuredCustomer(isVenteDefferee: boolean, putsOnStandby: boolean): void {
    this.openUninsuredCustomerSubject.next({ isVenteDefferee, putsOnStandby });
  }

  saveCashSale(entryAmount: number): void {
    this.spinner.show();
    const currentSale = this.currentSaleService.currentSale();
    this.updateSaleAmounts(currentSale, entryAmount);
    this.subscribeToFinalyseResponse(this.salesService.saveCash(currentSale));
  }

  putCurrentCashSaleOnHold(): void {
    this.subscribeToPutOnHoldResponse(this.salesService.putCurrentCashSaleOnStandBy(this.currentSaleService.currentSale()));
  }

  createComptant(salesLine: ISalesLine): void {
    this.spinner.show();
    this.subscribeToCreateSaleComptantResponse(this.salesService.createComptant(this.createSaleComptant(salesLine)));
  }

  addItemToSale(salesLine: ISalesLine): void {
    this.spinner.show();
    this.subscribeToSaveLineResponse(this.salesService.addItemComptant(salesLine));
  }

  removeItemFromSale(id: number): void {
    this.spinner.show();
    const sale = this.currentSaleService.currentSale();
    this.salesService.deleteItem(id).subscribe({
      next: () => this.subscribeToSaveResponse(this.salesService.find(sale.id)),
      error: (err: any) => this.onSaveSaveError(err, sale),
      complete: () => this.spinner.hide(),
    });
  }

  updateItemQtyRequested(salesLine: ISalesLine): void {
    this.spinner.show();
    const sale = this.currentSaleService.currentSale();
    this.salesService.updateItemQtyRequested(salesLine).subscribe({
      next: () => this.subscribeToSaveResponse(this.salesService.find(sale.id)),
      error: (err: any) => this.onSaveSaveError(err, sale, salesLine),
      complete: () => this.spinner.hide(),
    });
  }

  updateItemQtySold(salesLine: ISalesLine): void {
    this.spinner.show();
    const sale = this.currentSaleService.currentSale();
    this.salesService.updateItemQtySold(salesLine).subscribe({
      next: () => this.subscribeToSaveResponse(this.salesService.find(sale.id)),
      error: (err: any) => this.onSaveSaveError(err, sale),
      complete: () => this.spinner.hide(),
    });
  }

  updateItemPrice(salesLine: ISalesLine): void {
    this.spinner.show();
    const sale = this.currentSaleService.currentSale();
    this.salesService.updateItemPrice(salesLine).subscribe({
      next: () => this.subscribeToSaveResponse(this.salesService.find(sale.id)),
      error: (err: any) => this.onSaveSaveError(err, sale),
      complete: () => this.spinner.hide(),
    });
  }

  addRemise(remise: IRemise): void {
    this.spinner.show();
    this.salesService
      .addRemise({
        key: this.currentSaleService.currentSale()?.id,
        value: remise.id,
      })
      .subscribe({
        next: () => this.subscribeToSaveResponse(this.salesService.find(this.currentSaleService.currentSale().id)),
        error: (err: any) => this.onSaveError(err),
        complete: () => this.spinner.hide(),
      });
  }

  removeRemise(): void {
    this.spinner.show();
    this.salesService.removeRemiseFromCashSale(this.currentSaleService.currentSale().id).subscribe({
      next: () => this.subscribeToSaveResponse(this.salesService.find(this.currentSaleService.currentSale().id)),
      error: (err: any) => this.onSaveError(err),
      complete: () => this.spinner.hide(),
    });
  }

  printInvoice(saleId: number): void {
    this.spinner.show();
    this.salesService.printInvoice(saleId).subscribe({
      next: blod => {
        const blobUrl = URL.createObjectURL(blod);
        window.open(blobUrl);
      },
      complete: () => this.spinner.hide(),
    });
  }

  printReceipt(saleId: number): void {
    this.spinner.show();
    this.salesService.printReceipt(saleId).subscribe({
      complete: () => this.spinner.hide(),
    });
  }

  getCashAmount(modes: any[], cashCode: string): number {
    let cashInput;
    if (modes.length > 0) {
      cashInput = modes.find((input: any) => input.code === cashCode);
      if (cashInput) {
        return cashInput.amount;
      }
      return 0;
    } else {
      cashInput = modes[0];
      if (cashInput.code === cashCode) {
        return cashInput.amount;
      }
      return 0;
    }
  }

  private subscribeToSaveLineResponse(result: Observable<HttpResponse<ISalesLine>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISalesLine>) => this.subscribeToSaveResponse(this.salesService.find(res.body.saleId)),
      error: err => this.onSaveSaveError(err, this.currentSaleService.currentSale()),
      complete: () => this.spinner.hide(),
    });
  }

  private subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body),
      error: error => this.onSaveError(error),
      complete: () => this.spinner.hide(),
    });
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

  private subscribeToFinalyseResponse(result: Observable<HttpResponse<FinalyseSale>>): void {
    result.subscribe({
      next: (res: HttpResponse<FinalyseSale>) => this.onFinalyseSuccess(res.body),
      error: err => this.onFinalyseError(err),
      complete: () => this.spinner.hide(),
    });
  }

  private subscribeToPutOnHoldResponse(result: Observable<HttpResponse<FinalyseSale>>): void {
    result.subscribe({
      next: (res: HttpResponse<FinalyseSale>) => this.onFinalyseSuccess(res.body, true),
      error: err => this.onFinalyseError(err),
      complete: () => this.spinner.hide(),
    });
  }

  private onFinalyseSuccess(response: FinalyseSale | null, putOnStandBy = false): void {
    this.finalyseSaleSubject.next({ saleId: response.saleId, success: true, putOnStandBy });
  }

  private onFinalyseError(err: any): void {
    this.finalyseSaleSubject.next({ error: err, success: false });
  }

  private subscribeToCreateSaleComptantResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe({
      next: (res: HttpResponse<ISales>) => this.onSaleComptantResponseSuccess(res.body),
      error: error => this.onSaveSaveError(error, this.currentSaleService.currentSale()),
      complete: () => this.spinner.hide(),
    });
  }

  private onSaleComptantResponseSuccess(sale: ISales | null): void {
    this.currentSaleService.setCurrentSale(sale);
    this.saveResponseSubject.next({ success: true });
  }

  private updateSaleAmounts(sale: ISales, entryAmount: number): void {
    const restToPay = sale.amountToBePaid - entryAmount;
    sale.payrollAmount = restToPay <= 0 ? sale.amountToBePaid : entryAmount;
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
