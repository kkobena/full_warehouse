import { IStockProduit } from '../../shared/model/stock-produit.model';

export interface IRepartitionStockProduit {
  id?: number;
  userFullName?: string;
  userId?: number;
  mvtQty?: number;
  created?: Date;
  produitName?: string;
  produitCode?: string;
  codeCip?: string;
  codeEanFabricant?: string;
  typeRepartition?: string;
  stockProduitSrc?: IStockProduit;
  stockProduitDest?: IStockProduit;
  destFinalStock?: number;
  destInitStock?: number;
  sourceFinalStock?: number;
  sourceInitStock?: number;
}

export class RepartitionStockProduit implements IRepartitionStockProduit {
  constructor(
    public id?: number,
    public userFullName?: string,
    public mvtQty?: number,
    public created?: Date,
    public produitName?: string,
    public produitCode?: string,
    public codeEanFabricant?: string,
    public stockProduitSrc?: IStockProduit,
    public stockProduitDest?: IStockProduit,
    public destFinalStock?: number,
    public destInitStock?: number,
    public sourceFinalStock?: number,
    public sourceInitStock?: number,
  ) {}
}

export interface ISuggestionReassort {
  id?: number;
  reference?: string;
  created?: Date;
  updated?: Date;
  statut?: StatutReassort;
  typeReassort?: TypeReassort;
  magasinId?: number;
  ligneReassorts?: ILigneReassort[];
}

export interface ILigneReassort {
  id?: number;
  stockProduitId?: number;
  produitLibelle?: string;
  produitCode?: string;
  storageName?: string;
  quantity?: number;
  stockAvailable?: number;
  seuilMini?: number;
  stockActuel?: number;
}

export class LigneReassort implements ILigneReassort {
  constructor(
    public id?: number,
    public stockProduitId?: number,
    public produitLibelle?: string,
    public produitCode?: string,
    public storageName?: string,
    public quantity?: number,
    public stockAvailable?: number,
    public seuilMini?: number,
    public stockActuel?: number,
  ) {}
}

export enum StatutReassort {
  OPEN = 'OPEN',
  CLOSED = 'CLOSED',
}

export enum TypeReassort {
  RAYON = 'RAYON',
  RESERVE = 'RESERVE',
}

export interface IRepartitionSearchQuery {
  storageId?: number;
  userId?: number;
  searchTerm?: string;
  dateDebut?: string;
  dateFin?: string;
  typeRepartition?: string;
  stockProduitId?: number;
  page?: number;
  size?: number;
}
