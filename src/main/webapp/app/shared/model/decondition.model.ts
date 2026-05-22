import { IUser } from 'app/core/user/user.model';
import { IProduit } from 'app/shared/model/produit.model';

export interface IDecondition {
  id?: number;
  qtyMvt?: number;
  dateMtv?: string;
  stockBefore?: number;
  stockAfter?: number;
  user?: IUser;
  produit?: IProduit;
  produitId?: number;
}

export class Decondition implements IDecondition {
  constructor(
    public id?: number,
    public qtyMvt?: number,
    public dateMtv?: string,
    public stockBefore?: number,
    public stockAfter?: number,
    public user?: IUser,
    public produit?: IProduit,
    public produitId?: number,
  ) {}
}
