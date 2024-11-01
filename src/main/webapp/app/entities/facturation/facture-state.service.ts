import { Injectable, signal, WritableSignal } from '@angular/core';
import { Facture } from './facture.model';

@Injectable({
  providedIn: 'root',
})
export class FactureStateService {
  selectedInvoice: WritableSignal<Facture> = signal<Facture>(null);

  constructor() {}

  setCurrentInvoice(facture: Facture): void {
    this.selectedInvoice.set(facture);
  }
}
