import { CommandeId } from './abstract-commande.model';

export interface IPendingRetourBon {
  id: number;
  dateMtv: string;
  commentaire?: string;
  itemCount: number;
}

export interface IStockEntryResult {
  commandeId: CommandeId;
  pendingRetourBons: IPendingRetourBon[];
}
