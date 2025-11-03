export enum TypeMagasin {
  OFFICINE = 'OFFICINE',
  DEPOT = 'DEPOT',
  DEPOT_AGGREE = 'DEPOT_AGGREE',
}

export interface IMagasin {
  id?: number;
  name?: string;
  fullName?: string;
  phone?: string;
  address?: string;
  note?: string;
  registre?: string;
  welcomeMessage?: string;
  compteContribuable?: string;
  managerLastName?: string;
  managerFirstName?: string;
  numComptable?: string;
  typeMagasin?: TypeMagasin;
  typeLibelle?: string;
  email?: string;
  compteBancaire?: string;
  registreImposition?: string;
  primaryStorage?: IStorage;
  pointOfSale?: IStorage;

}

export class Magasin implements IMagasin {
  constructor(
    public id?: number,
    public name?: string,
    public fullName?: string,
    public phone?: string,
    public address?: string,
    public note?: string,
    public registre?: string,
    public welcomeMessage?: string,
    public compteContribuable?: string,
    public numComptable?: string,
    public typeMagasin?: TypeMagasin,
    public email?: string,
    public compteBancaire?: string,
    public registreImposition?: string,
    public primaryStorage?: IStorage,
    public pointOfSale?: IStorage,
  ) {
    this.typeMagasin = this.typeMagasin || TypeMagasin.OFFICINE;
  }
}

export interface IStorage {
  id?: number;
  name?: string;
  storageType?: string;
  magasin?: IMagasin;
}

export const enum StorageType {
  PRINCIPAL = 'Stockage principal',
  SAFETY_STOCK = 'Reserve',
  POINT_DE_VENTE = 'Point de vente',
}
