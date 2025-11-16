import { Injectable, signal, WritableSignal } from '@angular/core';
import { ITypePrescription } from '../../../shared/model/prescription-vente.model';

@Injectable({
  providedIn: 'root',
})
export class TypePrescriptionService {
  typePrescription: WritableSignal<ITypePrescription> = signal<ITypePrescription>({
    code: 'PRESCRIPTION',
    name: 'Prescription',
  });
  typePrescriptionDefault: WritableSignal<ITypePrescription> = signal<ITypePrescription>({
    code: 'PRESCRIPTION',
    name: 'Prescription',
  });

  setTypePrescription(typePrescription: ITypePrescription): void {
    this.typePrescription.set(typePrescription);
  }
}
