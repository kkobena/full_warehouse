import { Moment } from 'moment';
import { IProduit } from 'app/shared/model/produit.model';

import { TransactionType } from 'app/shared/model/enumerations/transaction-type.model';
import { User } from 'app/core/user/user.model';

export interface IInventoryTransaction {
  id?: number;
  transactionType?: TransactionType;
  amount?: number;
  createdAt?: Moment;
  updatedAt?: Moment;
  quantity?: number;
  quantityBefor?: number;
  quantityAfter?: number;
  produit?: IProduit;
  user?: User;
}

export class InventoryTransaction implements IInventoryTransaction {
  constructor(
    public id?: number,
    public transactionType?: TransactionType,
    public amount?: number,
    public createdAt?: Moment,
    public updatedAt?: Moment,
    public quantity?: number,
    public quantityBefor?: number,
    public quantityAfter?: number,
    public produit?: IProduit,
    public user?: User
  ) {}
}
