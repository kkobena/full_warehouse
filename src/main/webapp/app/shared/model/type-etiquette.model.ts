export interface ITypeEtiquette {
  id?: number;
  libelle?: string;
}

export class TypeEtiquette implements ITypeEtiquette {
  constructor(public id?: number, public libelle?: string) {}
}
