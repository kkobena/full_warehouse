export interface IRemise {
  id?: number;
  valeur?: string;
  remiseValue?: number;
  type?: string;
  begin?: string;
  end?: string;
  enable?: boolean;
}

export class Remise implements IRemise {
  constructor(
    public id?: number,
    public valeur?: string,
    public remiseValue?: number,
    public type?: string,
  ) {}
}

export enum RemiseType {
  remiseClient = 'Remise client',
  remiseProduit = 'Remise produit',
}
