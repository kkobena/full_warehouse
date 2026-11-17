export enum TypeMagasin {
  /** L'etablissement principal : racine, porteur de la licence et de l'identite FNE. */
  OFFICINE = 'OFFICINE',
  /** Etablissement rattache, approvisionne en consignation par son officine. */
  DEPOT = 'DEPOT',
  /** Unite interne au meme lieu — une parapharmacie par exemple — sans existence juridique propre. */
  UNITE_GESTION = 'UNITE_GESTION',
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
  /** Officine dont ce magasin depend ; absent pour une officine, qui est racine. */
  parentId?: number;
  /** Ce magasin passe-t-il ses propres commandes fournisseur ? Faux pour un depot. */
  commandeEnPropre?: boolean;
  email?: string;
  compteBancaire?: string;
  registreImposition?: string;
  fnePointOfSale?: string;
  fneSecretKey?: string;
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
  type?: string;
  magasin?: IMagasin;
}

export const enum StorageType {
  PRINCIPAL = 'Stockage principal',
  SAFETY_STOCK = 'Reserve',
}
