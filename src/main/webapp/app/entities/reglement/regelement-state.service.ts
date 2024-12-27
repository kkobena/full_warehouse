import { Injectable, signal, WritableSignal } from '@angular/core';
import { InvoiceSearchParams } from '../facturation/edition-search-params.model';

@Injectable({
  providedIn: 'root',
})
export class RegelementStateService {
  invoiceSearchParams: WritableSignal<InvoiceSearchParams> = signal<InvoiceSearchParams>(null);

  constructor() {}

  setInvoiceSearchParams(searchParams: InvoiceSearchParams): void {
    this.invoiceSearchParams.set(searchParams);
  }
}
