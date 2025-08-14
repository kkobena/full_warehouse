export interface IMagasin {
  id?: number;
  name?: string;
  fullName?: string;
  phone?: string;
  address?: string;
  note?: string;
  registre?: string;
  welcomeMessage?: string;
}

export class Magasin implements IMagasin {
  constructor(
    public id?: number,
    public name?: string,
    public phone?: string,
    public address?: string,
    public note?: string,
    public registre?: string,
    public welcomeMessage?: string,
  ) {}
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
