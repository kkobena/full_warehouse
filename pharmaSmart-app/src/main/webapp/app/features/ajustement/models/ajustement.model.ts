import { IAjust } from '../../../shared/model/ajust.model';
import { IAjustement } from '../../../shared/model/ajustement.model';

export type AjustDirection = 'IN' | 'OUT';

export type AjustEventType =
  | 'AJUST_CREATED'
  | 'AJUST_FINALIZED'
  | 'LINE_ADDED'
  | 'LINE_REMOVED'
  | 'LINE_UPDATED'
  | 'LINES_LOADED'
  | 'HISTORY_LOADED'
  | 'ERROR';

export interface AjustEvent {
  type: AjustEventType;
  payload?: unknown;
  seq: number;
}

/** Lot disponible pour sélection (AJUSTEMENT_IN gestion_lot=true) */
export interface ILotItem {
  id: number;
  numLot?: string;
  expiryDate?: string;
  currentQuantity: number;
  createdDate?: string;
}

export interface AjustHistoryFilter {
  search: string;
  fromDate: Date;
  toDate: Date;
  userId: number | null;
}

// Re-export shared models so feature code imports from one place
export { IAjust, IAjustement };
