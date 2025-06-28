import { Moment } from 'moment';
import { IStoreInventoryLine } from 'app/shared/model/store-inventory-line.model';
import { IUser } from '../../core/user/user.model';
import { IStorage } from './magasin.model';
import { IRayon } from './rayon.model';

export interface IStoreInventory {
  id?: number;
  abbrName?: string;
  inventoryValueCostBegin?: number;
  inventoryAmountBegin?: number;
  createdAt?: Moment;
  updatedAt?: Moment;
  inventoryValueCostAfter?: number;
  inventoryAmountAfter?: number;
  storeInventoryLines?: IStoreInventoryLine[];
  statut?: InventoryStatut;
  user?: IUser;
  storage?: IStorage;
  rayon?: IRayon;
  inventoryType?: InventoryType;
  inventoryCategory?: InventoryCategory;
  gapCost?: number;
  gapAmount?: number;
  description?: string;
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
    public inventoryType?: InventoryType,
    public statut?: InventoryStatut,
  ) {
    this.inventoryType = this.inventoryType || 'MANUEL';
  }
}

export type InventoryStatut = 'CREATE' | 'CLOSED' | 'PROCESSING';
export type InventoryType = 'MANUEL' | 'PROGRAMME';
export type InventoryCategoryType = 'STORAGE' | 'RAYON' | 'MAGASIN' | 'FAMILLY' | 'NONE';

export class InventoryCategory {
  name: InventoryCategoryType;
  label: string;

  constructor(name: InventoryCategoryType, label: string) {
    this.name = name;
    this.label = label;
  }
}

export class StoreInventoryFilterRecord {
  inventoryCategories?: InventoryCategoryType[];
  statuts?: InventoryStatut[];
  storageId?: number;
  rayonId?: number;
  userId?: number;
}

export class StoreInventoryExportRecord {
  exportGroupBy?: string;
  filterRecord: any;
}

export class ItemsCountRecord {
  count?: number;
}

export const CATEGORY_INVENTORY: InventoryCategory[] = [
  new InventoryCategory('MAGASIN', 'Inventaire global'),
  new InventoryCategory('STORAGE', "Inventaire d'un emplacement"),
  new InventoryCategory('RAYON', "Inventaire d'un rayon"),
];
export const GROUPING_BY: InventoryCategory[] = [
  new InventoryCategory('RAYON', 'Grouper par rayon'),
  new InventoryCategory('STORAGE', 'Grouper par emplacement'),
  new InventoryCategory('FAMILLY', 'Grouper par famille'),
];
