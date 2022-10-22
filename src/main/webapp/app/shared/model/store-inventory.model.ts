import { Moment } from 'moment';
import { IStoreInventoryLine } from 'app/shared/model/store-inventory-line.model';
export interface IStoreInventory {
  id?: number;
  inventoryValueCostBegin?: number;
  inventoryAmountBegin?: number;
  createdAt?: Moment;
  updatedAt?: Moment;
  inventoryValueCostAfter?: number;
  inventoryAmountAfter?: number;
  storeInventoryLines?: IStoreInventoryLine[];
  statut?: string;
}

export class StoreInventory implements IStoreInventory {
  constructor(
    public id?: number,
    public inventoryValueCostBegin?: number,
    public inventoryAmountBegin?: number,
    public createdAt?: Moment,
    public updatedAt?: Moment,
    public inventoryValueCostAfter?: number,
    public inventoryAmountAfter?: number,
    public storeInventoryLines?: IStoreInventoryLine[],
    public statut?: string
  ) {}
}
