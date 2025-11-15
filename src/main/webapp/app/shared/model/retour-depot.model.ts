import { IUser } from 'app/core/user/user.model';
import { RetourStatut } from 'app/shared/model/enumerations/retour-statut.model';
import { IRetourDepotItem } from 'app/shared/model/retour-depot-item.model';

export interface IRetourDepot {
  id?: number;
  dateMtv?: string;
  user?: IUser;
  statut?: RetourStatut;
  depotId?: number;
  depotName?: string;
  retourDepotItems?: IRetourDepotItem[];
}

export class RetourDepot implements IRetourDepot {
  constructor(
    public id?: number,
    public dateMtv?: string,
    public user?: IUser,
    public statut?: RetourStatut,
    public depotId?: number,
    public depotName?: string,
    public retourDepotItems?: IRetourDepotItem[],
  ) {
    this.retourDepotItems = this.retourDepotItems ?? [];
  }
}
