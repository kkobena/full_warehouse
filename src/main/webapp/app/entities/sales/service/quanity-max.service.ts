import { Injectable, signal, WritableSignal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class QuanityMaxService {
  quantityMax: WritableSignal<number> = signal<number>(null);

  constructor() {}

  setQuantity(quantity: number): void {
    this.quantityMax.set(quantity);
  }
}
