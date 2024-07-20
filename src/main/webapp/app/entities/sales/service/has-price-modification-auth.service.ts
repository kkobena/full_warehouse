import { Injectable, signal, WritableSignal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class HasPriceModificationAuthService {
  priceAuthority: WritableSignal<boolean> = signal<boolean>(true);

  constructor() {}

  setQuantity(asAuthory: boolean): void {
    this.priceAuthority.set(asAuthory);
  }
}
