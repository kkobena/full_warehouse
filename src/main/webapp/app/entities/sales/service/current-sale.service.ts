import { Injectable, signal, WritableSignal } from '@angular/core';
import { ISales } from '../../../shared/model/sales.model';

@Injectable({
  providedIn: 'root',
})
export class CurrentSaleService {
  currentSale: WritableSignal<ISales> = signal<ISales>(null);
  plafondIsReached: WritableSignal<boolean> = signal<boolean>(false);
  isEdit: WritableSignal<boolean> = signal<boolean>(false);
  voFromCashSale: WritableSignal<boolean> = signal<boolean>(false);
  isVenteSansBon: WritableSignal<boolean> = signal<boolean>(false);
  typeVo: WritableSignal<string> = signal<string>(null);

  constructor() {}

  setCurrentSale(sales: ISales): void {
    this.currentSale.set(sales);
  }

  setPlafondIsReached(isReached: boolean): void {
    this.plafondIsReached.set(isReached);
  }

  setIsEdit(isEdit: boolean): void {
    this.isEdit.set(isEdit);
  }

  setVoFromCashSale(voFromCashSale: boolean): void {
    this.voFromCashSale.set(voFromCashSale);
  }

  setVenteSansBon(isVenteSansBon: boolean): void {
    this.isVenteSansBon.set(isVenteSansBon);
  }

  setTypeVo(typeVo: string): void {
    this.typeVo.set(typeVo);
  }

  reset(): void {
    this.currentSale.set(null);
    this.plafondIsReached.set(false);
    this.isEdit.set(false);
    this.voFromCashSale.set(false);
    this.isVenteSansBon.set(false);
    this.typeVo.set(null);
  }
}
