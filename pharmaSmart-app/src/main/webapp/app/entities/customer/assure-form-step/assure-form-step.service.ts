import { Injectable, signal, WritableSignal } from '@angular/core';
import { ICustomer } from '../../../shared/model';

@Injectable({
  providedIn: 'root',
})
export class AssureFormStepService {
  assure: WritableSignal<ICustomer> = signal<ICustomer>(null);
  typeAssure: WritableSignal<string> = signal<string>(null);
  isEdition: WritableSignal<boolean> = signal<boolean>(false);
  /** Lignes d'ayants droit telles que saisies, même incomplètes : elles survivent à un changement d'onglet. */
  ayantDroitsBrouillon: WritableSignal<any[]> = signal<any[]>([]);
  /** Les ayants droit saisis sont-ils tous complets ? (une ligne à moitié remplie bloque l'enregistrement) */
  ayantDroitsValides: WritableSignal<boolean> = signal<boolean>(true);

  constructor() {}

  setAssure(customer: ICustomer): void {
    this.assure.set(customer);
  }

  setEdition(isEdition: boolean): void {
    this.isEdition.set(isEdition);
  }

  setTypeAssure(typeAssure: string): void {
    this.typeAssure.set(typeAssure);
  }
}
