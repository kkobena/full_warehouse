import { Injectable, signal, WritableSignal } from '@angular/core';
import { ICustomer } from '../../../shared/model/customer.model';

@Injectable({
  providedIn: 'root',
})
export class AssureFormStepService {
  assure: WritableSignal<ICustomer> = signal<ICustomer>(null);

  isEdition: WritableSignal<boolean> = signal<boolean>(false);

  constructor() {}

  setAssure(customer: ICustomer): void {
    this.assure.set(customer);
  }

  setEdition(isEdition: boolean): void {
    this.isEdition.set(isEdition);
  }
}
