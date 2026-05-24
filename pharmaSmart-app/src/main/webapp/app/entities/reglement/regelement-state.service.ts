import { Injectable, signal, WritableSignal } from '@angular/core';
import { InvoiceSearchParams } from '../facturation/edition-search-params.model';
import { InvoicePaymentParam } from './model/reglement.model';

@Injectable({
  providedIn: 'root',
})
export class RegelementStateService {
  invoiceSearchParams: WritableSignal<InvoiceSearchParams> = signal<InvoiceSearchParams>(null);
  invoicePaymentParam: WritableSignal<InvoicePaymentParam> = signal<InvoicePaymentParam>(null);

  setInvoiceSearchParams(searchParams: InvoiceSearchParams): void {
    this.invoiceSearchParams.set(searchParams);
  }

  setInvoicePaymentParam(invoicePaymentParam: InvoicePaymentParam): void {
    this.invoicePaymentParam.set(invoicePaymentParam);
  }
}
