// Extended category types (10 types, extending the original 4)
export type InventoryCategoryType =
  | 'MAGASIN'
  | 'STORAGE'
  | 'RAYON'
  | 'FAMILLY'
  | 'PERIME'
  | 'ALERTE_PEREMPTION'
  | 'VENDU'
  | 'INVENDU'
  | 'SOUS_SEUIL'
  | 'EN_RUPTURE';

export interface InventoryProgressRecord {
  inventoryId: number;
  totalLines: number;
  updatedLines: number;
  withGap: number;
  progressPercent: number;
}

export interface ImportLineErrorRecord {
  lineNumber: number;
  rawCode: string;
  rawQuantity: string;
  reason: string;
}

export interface ImportResultRecord {
  imported: number;
  ignored: number;
  rejected: number;
  errors: ImportLineErrorRecord[];
}

export interface BatchSyncResultRecord {
  saved: number;
  failed: number;
  failedIds: number[];
}

// For /api/store-inventory-lines/v2 response
export interface IInventoryLine {
  id?: number;
  produitId?: number;
  produitLibelle?: string;
  codeCip?: string;
  produitCip?: string;
  codeEanLabo?: string;
  quantityOnHand?: number;
  quantityInit?: number;
  gap?: number;
  prixAchat?: number;
  prixUni?: number;
  updated?: boolean;
  currentStock?: number;
  storeInventoryId?: number;
}

// Pending edit buffered locally before batch save
export interface PendingEdit {
  lineId: number;
  quantityOnHand: number;
}

// Filter for line query
export type InventoryLineFilter =
  'NONE'
  | 'UPDATED'
  | 'NOT_UPDATED'
  | 'GAP'
  | 'GAP_NEGATIF'
  | 'GAP_POSITIF';

// Creation request
export interface StoreInventoryCreateRecord {
  inventoryCategory: InventoryCategoryType;
  description?: string;
  storage?: number;
  rayon?: number;
  famillyId?: number;
  dateFrom?: string;
  dateTo?: string;
  alerteJours?: number;
}

export interface InventoryCategoryInfo {
  value: InventoryCategoryType;
  label: string;
  icon: string;
  group: 'scope' | 'thematic';
  needsStorage?: boolean;
  needsRayon?: boolean;
  needsFamilly?: boolean;
  needsDateRange?: boolean;
  needsAlerteJours?: boolean;
}

export const INVENTORY_CATEGORIES: InventoryCategoryInfo[] = [
  {value: 'MAGASIN', label: 'Inventaire global (magasin)', icon: 'pi pi-building', group: 'scope'},
  {
    value: 'STORAGE',
    label: "Inventaire d'un emplacement",
    icon: 'pi pi-box',
    group: 'scope',
    needsStorage: true
  },
  {
    value: 'RAYON',
    label: "Inventaire d'un rayon",
    icon: 'pi pi-th-large',
    group: 'scope',
    needsStorage: true,
    needsRayon: true
  },
  {
    value: 'FAMILLY',
    label: "Inventaire d'une famille",
    icon: 'pi pi-tags',
    group: 'scope',
    needsFamilly: true
  },
  {
    value: 'PERIME',
    label: 'Produits périmés',
    icon: 'pi pi-exclamation-triangle',
    group: 'thematic'
  },
  {
    value: 'ALERTE_PEREMPTION',
    label: 'Alerte péremption',
    icon: 'pi pi-clock',
    group: 'thematic',
    needsAlerteJours: true
  },
  {
    value: 'VENDU',
    label: 'Produits vendus (période)',
    icon: 'pi pi-shopping-cart',
    group: 'thematic',
    needsDateRange: true
  },
  {
    value: 'INVENDU',
    label: 'Produits invendus (période)',
    icon: 'pi pi-inbox',
    group: 'thematic',
    needsDateRange: true
  },
  {
    value: 'SOUS_SEUIL',
    label: 'Stock sous seuil minimum',
    icon: 'pi pi-arrow-down',
    group: 'thematic'
  },
  {value: 'EN_RUPTURE', label: 'Rupture de stock', icon: 'pi pi-ban', group: 'thematic'},
];

export const LINE_FILTERS = [
  {value: 'NONE', label: 'Tous'},
  {value: 'UPDATED', label: 'Saisis'},
  {value: 'NOT_UPDATED', label: 'Non saisis'},
  {value: 'GAP', label: 'Avec écart'},
  {value: 'GAP_NEGATIF', label: 'Écart négatif'},
  {value: 'GAP_POSITIF', label: 'Écart positif'},
];

export interface InventoryEvent {
  type: InventoryEventType;
  payload?: any;
  seq: number;
}

export type InventoryEventType =
  | 'INVENTORY_CREATED'
  | 'INVENTORY_CLOSED'
  | 'INVENTORY_DELETED'
  | 'LINES_LOADED'
  | 'LINE_EDITED'
  | 'BATCH_SAVED'
  | 'BATCH_SAVE_ERROR'
  | 'IMPORT_COMPLETED'
  | 'PROGRESS_UPDATED'
  | 'LINE_SAVED'
  | 'LINE_SAVE_ERROR';
