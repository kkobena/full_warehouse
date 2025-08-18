import { Injectable, signal, WritableSignal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LastCurrencyGivenService {
  lastCurrency: WritableSignal<number> = signal<number>(null);
  givenCurrency: WritableSignal<number> = signal<number>(0);

  setLastCurrency(amount: number): void {
    this.lastCurrency.set(amount);
  }

  setGivenCurrentSale(amount: number): void {
    this.givenCurrency.set(amount);
  }

  resetGivenCurrency(): void {
    this.givenCurrency.set(0);
  }
}
