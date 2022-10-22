import { IStoreInventory } from 'app/shared/model/store-inventory.model';
import { IProduit } from 'app/shared/model/produit.model';

export interface IStoreInventoryLine {
  id?: number;
  quantityOnHand?: number;
  quantityInit?: number;
  quantitySold?: number;
  gap?: number;
  inventoryValueCost?: number;
  inventoryValueLatestSellingPrice?: number;
  storeInventory?: IStoreInventory;
  produit?: IProduit;
  updated?: boolean;
  storeInventoryId?: number;
}

export class StoreInventoryLine implements IStoreInventoryLine {
  constructor(
    public id?: number,
    public quantityOnHand?: number,
    public quantityInit?: number,
    public quantitySold?: number,
    public inventoryValueCost?: number,
    public inventoryValueLatestSellingPrice?: number,
    public storeInventory?: IStoreInventory,
    public produit?: IProduit,
    public gap?: number,
    public updated?: boolean,
    public storeInventoryId?: number
  ) {}
}
