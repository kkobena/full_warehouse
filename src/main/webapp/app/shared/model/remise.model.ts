export interface IRemise {
  id?: number;
  valeur?: string;
  remiseValue?: number;
  type?: string;
  begin?: string;
  end?: string;
  enable?: boolean;
  grilles?: GrilleRemise[];
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

export class CodeRemise {
  constructor(
    public value: string,
    public codeVno?: string,
    public codeVo?: string,
    public remise?: Remise,
  ) {}
}

export class GrilleRemise {
  constructor(
    public id?: number,
    public remiseValue?: number,
    public tauxRemise?: number,
    public enable?: boolean,
    public code?: string,
    public codeRemise?: CodeRemise,
    public grilleType?: string,
  ) {}
}
