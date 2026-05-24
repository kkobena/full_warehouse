import { Injectable, signal, WritableSignal } from '@angular/core';
import { ICustomer } from '../../../shared/model';

@Injectable({
  providedIn: 'root',
})
export class SelectedCustomerService {
  selectedCustomerSignal: WritableSignal<ICustomer | null> = signal<ICustomer | null>(null);

  setCustomer(customer: ICustomer | null): void {
    this.selectedCustomerSignal.set(customer);
  }
}
