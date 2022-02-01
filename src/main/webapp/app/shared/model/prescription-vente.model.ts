export interface ITypePrescription {
  name?: string;
  code?: string;
}

export class TypePrescription implements ITypePrescription {
  constructor(public code?: string, public name?: string) {}
}
