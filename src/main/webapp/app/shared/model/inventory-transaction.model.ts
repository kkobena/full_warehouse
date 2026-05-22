import { IProduit } from 'app/shared/model/produit.model';

import { TransactionType } from 'app/shared/model/enumerations/transaction-type.model';
import { User } from 'app/core/user/user.model';

export interface IInventoryTransaction {
  id?: number;
  transactionType?: TransactionType;
  amount?: number;
  createdAt?: string;
  updatedAt?: string;
  quantity?: number;
  quantityBefor?: number;
  quantityAfter?: number;
  produit?: IProduit;
  user?: User;
  abbrName?: string;
}

export class InventoryTransaction implements IInventoryTransaction {
  constructor(
    public id?: number,
    public transactionType?: TransactionType,
    public amount?: number,
    public createdAt?: string,
    public updatedAt?: string,
    public quantity?: number,
    public quantityBefor?: number,
    public quantityAfter?: number,
    public produit?: IProduit,
    public user?: User,
    public abbrName?: string,
  ) {}
}
