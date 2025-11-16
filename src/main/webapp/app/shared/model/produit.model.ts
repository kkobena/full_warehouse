import { Moment } from 'moment';
import { ISalesLine } from 'app/shared/model/sales-line.model';
import { IStoreInventoryLine } from 'app/shared/model/store-inventory-line.model';
import { IOrderLine } from 'app/shared/model/order-line.model';
import { IInventoryTransaction } from 'app/shared/model/inventory-transaction.model';
import { TypeProduit } from 'app/shared/model/enumerations/type-produit.model';
import { IStockProduit } from './stock-produit.model';
import { IFournisseurProduit } from './fournisseur-produit.model';
import { IRayonProduit } from './rayon-produit.model';
import { ITableau } from './tableau.model';
import { EtatProduit } from './etat-produit.model';
import { StorageType } from './magasin.model';

export interface IProduit {
  id?: number;
  libelle?: string;
  codeCip?: string;
  typeProduit?: TypeProduit;
  itemQuantity?: number;
  quantity?: number;
  costAmount?: number;
  regularUnitPrice?: number;
  netUnitPrice?: number;
  createdAt?: Moment;
  updatedAt?: Moment;
  itemQty?: number;
  itemCostAmount?: number;
  itemRegularUnitPrice?: number;
  salesLines?: ISalesLine[];
  storeInventoryLines?: IStoreInventoryLine[];
  orderLines?: IOrderLine[];
  inventoryTransactions?: IInventoryTransaction[];
  quantityReceived?: number;
  produitId?: number;
  produits?: IProduit[];
  qtyAppro?: number;
  qtySeuilMini?: number;
  chiffre?: boolean;
  dateperemption?: boolean;
  exclude?: boolean;
  deconditionnable?: boolean;
  prixMnp?: number;
  codeEan?: string;
  rayonLibelle?: string;
  stockProduits?: IStockProduit[];
  fournisseurProduits?: IFournisseurProduit[];
  fournisseurProduit?: IFournisseurProduit;
  stockProduit?: IStockProduit;
  laboratoireLibelle?: string;
  laboratoireId?: number;
  formeLibelle?: string;
  formeId?: number;
  typeEtiquetteLibelle?: string;
  typeEtiquetteId?: number;
  familleLibelle?: string;
  familleId?: number;
  gammeLibelle?: string;
  gammeId?: number;
  tvaTaux?: string;
  tvaId?: number;
  remiseId?: number;
  tauxRemise?: number;
  totalQuantity?: number;
  perimeAt?: Moment;
  lastDateOfSale?: Moment;
  lastOrderDate?: Moment;
  lastInventoryDate?: Moment;
  qtyStatus?: string;
  fournisseurId?: number;
  rayonId?: number;
  expirationDate?: string;
  displayField?: string;
  rayonProduits?: IRayonProduit[];
  tableau?: ITableau;
  remiseCode?: string;
  etatProduit?: EtatProduit;
  dciId?: number;
  categorie?: string; //categorie ABC
  codeEanLaboratoire?: string; //categorie ABC
}

export class Produit implements IProduit {
  constructor(
    public id?: number,
    public libelle?: string,
    public typeProduit?: TypeProduit,
    public quantity?: number,
    public costAmount?: number,
    public regularUnitPrice?: number,
    public netUnitPrice?: number,
    public createdAt?: Moment,
    public updatedAt?: Moment,
    public itemQty?: number,
    public itemCostAmount?: number,
    public itemRegularUnitPrice?: number,
    public salesLines?: ISalesLine[],
    public storeInventoryLines?: IStoreInventoryLine[],
    public orderLines?: IOrderLine[],
    public inventoryTransactions?: IInventoryTransaction[],
    public itemQuantity?: number,
    public quantityReceived?: number,
    public produitId?: number,
    public produits?: IProduit[],
    public qtyAppro?: number,
    public qtySeuilMini?: number,
    public chiffre?: boolean,
    public dateperemption?: boolean,
    public exclude?: boolean,
    public deconditionnable?: boolean,
    public prixMnp?: number,
    public codeEan?: string,
    public stockProduits?: IStockProduit[],
    public fournisseurProduits?: IFournisseurProduit[],
    public fournisseurProduit?: IFournisseurProduit,
    public stockProduit?: IStockProduit,
    public laboratoireLibelle?: string,
    public laboratoireId?: number,
    public formeLibelle?: string,
    public formeId?: number,
    public typeEtiquetteLibelle?: string,
    public typeEtiquetteId?: number,
    public familleLibelle?: string,
    public familleId?: number,
    public gammeLibelle?: string,
    public gammeId?: number,
    public tvaTaux?: string,
    public tvaId?: number,
    public remiseId?: number,
    public tauxRemise?: number,
    public totalQuantity?: number,
    public perimeAt?: Moment,
    public lastDateOfSale?: Moment,
    public lastOrderDate?: Moment,
    public lastInventoryDate?: Moment,
    public qtyStatus?: string,
    public codeCip?: string,
    fournisseurId?: number,
    public rayonId?: number,
    public expirationDate?: string,
    public displayField?: string,
    public rayonProduits?: IRayonProduit[],
  ) {}
}

export class Dci {
  id?: number;
  libelle?: string;
  code?: string;
}

export class ProduitFournisseurSearch {
  id: number;
  codeCip: string;
  codeEan: string;
  prixUni: number;
  prixAchat: number;
}

export class ProduitRayonSearch {
  code: string;
  libelle: string;
}

export class ProduitStockSearch {
  quantite: number;
  qteUg: number;
  storage: number;
  storageType: StorageType;
}

export class ProduitSearch {
  id: number;
  itemQty: number;
  deconditionnable: boolean;
  parentId: number;
  codeCipPrincipalId: number;
  libelle: string;
  codeEanLabo: string;
  fournisseurs: ProduitFournisseurSearch[];
  fournisseurProduit: ProduitFournisseurSearch;
  rayons: ProduitRayonSearch[];
  stocks: ProduitStockSearch[];
  totalQuantity: number;
  regularUnitPrice: number;
}
