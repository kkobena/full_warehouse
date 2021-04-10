import { Moment } from 'moment';
import { ISalesLine } from 'app/shared/model/sales-line.model';
import { IStoreInventoryLine } from 'app/shared/model/store-inventory-line.model';
import { IOrderLine } from 'app/shared/model/order-line.model';
import { IInventoryTransaction } from 'app/shared/model/inventory-transaction.model';
import { ICategorie } from 'app/shared/model/categorie.model';
import { TypeProduit } from 'app/shared/model/enumerations/type-produit.model';

export interface IProduit {
  id?: number;
  libelle?: string;
  code?: string;
  imageUrl?: string;
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
  categorie?: ICategorie;
  quantityReceived?: number;
  produitId?: number;
  produits?: IProduit[];
  data?: string;
  imageType?: string;
}

export class Produit implements IProduit {
  constructor(
    public id?: number,
    public libelle?: string,
    public code?: string,
    public imageUrl?: string,
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
    public categorie?: ICategorie,
    public itemQuantity?: number,
    public quantityReceived?: number,
    public produitId?: number,
    public produits?: IProduit[],
    public data?: string,
    public imageType?: string
  ) {}
}
