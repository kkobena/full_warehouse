export interface IRemise {
  id?: number;
  valeur?: string;
  remiseValue?: number;
  type?: string;
  typeLibelle?: string;
  enable?: boolean;
  grilles?: GrilleRemise[];
}

export class Remise implements IRemise {
  constructor(
    public id?: number,
    public valeur?: string,
    public typeLibelle?: string,
    public remiseValue?: number,
    public type?: string,
    public enable?: boolean,
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

export class GroupRemise {
  constructor(
    public type: string,
    public typeLibelle: string,
    public items: IRemise[],
  ) {}
}
