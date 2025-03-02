import { Injectable, signal, WritableSignal } from '@angular/core';
import { ICustomer } from '../../../shared/model/customer.model';

@Injectable({
  providedIn: 'root',
})
export class SelectedCustomerService {
  selectedCustomerSignal: WritableSignal<ICustomer> = signal<ICustomer>(null);
  setCustomer(customer: ICustomer): void {
    this.selectedCustomerSignal.set(customer);
  }
}
