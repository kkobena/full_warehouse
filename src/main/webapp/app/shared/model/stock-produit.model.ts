import { IStorage } from './magasin.model';

export interface IStockProduit {
  id?: number;
  qtyStock?: number;
  qtyVirtual?: number;
  qtyUG?: number;
  nomLongMagasin?: string;
  nomMagasin?: string;
  rayonLibelle?: string;
  rayonId?: number;
  magasinId?: number;
  produitLibelle?: string;
  produitId?: number;
  storageId?: number;
  storageName?: string;
  storageType?: string; // libelle
  type?: string; // code type
  seuilMini?: number;
  stockReassort?: number;
  totalStockQuantity?: number;
  storage?: IStorage;
  produit?: IProduitBasic;
}

export interface IProduitBasic {
  id?: number;
  libelle?: string;
  codeCip?: string;
  stockProduits?: IStockProduit[];
}

export class StockProduit implements IStockProduit {
  constructor(
    public id?: number,
    public qtyStock?: number,
    public qtyVirtual?: number,
    public qtyUG?: number,
    public nomLongMagasin?: string,
    public nomMagasin?: string,
    public rayonLibelle?: string,
    public rayonId?: number,
    public produitLibelle?: string,
    public magasinId?: number,
    public produitId?: number,
    public storageId?: number,
    public storageName?: string,
    public storageType?: string,
    public seuilMini?: number,
    public stockReassort?: number,
  ) {}
}
