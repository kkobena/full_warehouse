export interface ITypeMvtCaisse {
  id?: number;
  libelle?: string;
  code?: string;
}

export class TypeMvtCaisse implements ITypeMvtCaisse {
  constructor(
    public id?: number,
    public libelle?: string,
    public code?: string,
  ) {}
}
