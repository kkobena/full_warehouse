export interface IFormProduit {
  id?: number;
  code?: string;
  libelle?: string;
}

export class FormProduit implements IFormProduit {
  constructor(public id?: number, public code?: string, public libelle?: string) {}
}
