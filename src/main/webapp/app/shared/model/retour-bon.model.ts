import { IUser } from 'app/core/user/user.model';
import { RetourBonStatut } from 'app/shared/model/enumerations/retour-bon-statut.model';
import { IRetourBonItem } from 'app/shared/model/retour-bon-item.model';

export interface IRetourBon {
  id?: number;
  dateMtv?: string;
  user?: IUser;
  statut?: RetourBonStatut;
  commentaire?: string;
  commandeId?: number;
  receiptDate?: string;
  commandeOrderDate?: string;
  commandeOrderReference?: string;
  receiptReference?: string;
  fournisseurLibelle?: string;
  retourBonItems?: IRetourBonItem[];
}

export class RetourBon implements IRetourBon {
  constructor(
    public id?: number,
    public dateMtv?: string,
    public user?: IUser,
    public statut?: RetourBonStatut,
    public commentaire?: string,
    public commandeId?: number,
    public commandeOrderDate?: string,
    public commandeOrderReference?: string,
    public fournisseurLibelle?: string,
    public retourBonItems?: IRetourBonItem[],
  ) {
    this.retourBonItems = this.retourBonItems ?? [];
  }
}
