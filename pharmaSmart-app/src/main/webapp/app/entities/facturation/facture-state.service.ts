import { Injectable, signal, WritableSignal } from '@angular/core';
import { Facture } from './facture.model';
import { InvoiceSearchParams } from './edition-search-params.model';

@Injectable({
  providedIn: 'root',
})
export class FactureStateService {
  selectedInvoice: WritableSignal<Facture> = signal<Facture>(null);
  invoiceSearchParams: WritableSignal<InvoiceSearchParams> = signal<InvoiceSearchParams>(null);

  constructor() {}

  setCurrentInvoice(facture: Facture): void {
    this.selectedInvoice.set(facture);
  }

  setInvoiceSearchParams(searchParams: InvoiceSearchParams): void {
    this.invoiceSearchParams.set(searchParams);
  }
}
