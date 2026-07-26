export type LotConflictAction = 'MERGE' | 'DELETE';

export interface ILotConflict {
  numLot?: string;
  sourceLotId?: number;
  sourceQuantity?: number;
  sourceExpiryDate?: string;
  targetLotId?: number;
  targetQuantity?: number;
  targetExpiryDate?: string;
}

export interface ILotResolution {
  lotId: number;
  action: LotConflictAction;
}

export interface IStockConflict {
  storageId?: number;
  storageLibelle?: string;
  sourceQtyStock?: number;
  sourceQtyVirtual?: number;
  sourceQtyUG?: number;
  targetQtyStock?: number;
  targetQtyVirtual?: number;
  targetQtyUG?: number;
}

export interface IProduitMergePreview {
  targetId?: number;
  sourceIds?: number[];
  rejectedSourceIds?: number[];
  rejectionReasons?: Record<string, string>;
  entityCounts?: Record<string, number>;
  lotConflicts?: ILotConflict[];
  stockConflicts?: IStockConflict[];
}

export interface IProduitMergeRequest {
  targetId: number;
  sourceIds: number[];
  lotResolutions: ILotResolution[];
}

export interface IProduitMergeResult {
  targetId?: number;
  mergedSourceIds?: number[];
  entityCounts?: Record<string, number>;
  stockConflicts?: IStockConflict[];
}
