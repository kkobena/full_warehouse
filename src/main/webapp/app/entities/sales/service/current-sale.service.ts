import { Injectable, signal, WritableSignal } from '@angular/core';
import { ISales } from '../../../shared/model/sales.model';

@Injectable({
  providedIn: 'root',
})
export class CurrentSaleService {
  currentSale: WritableSignal<ISales> = signal<ISales>(null);

  constructor() {}

  setCurrentSale(sales: ISales): void {
    this.currentSale.set(sales);
  }
}
