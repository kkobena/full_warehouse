import { IUser } from 'app/core/user/user.model';
import { IReponseRetourBonItem } from './reponse-retour-bon-item.model';

export interface IReponseRetourBon {
  id?: number;
  dateMtv?: string;
  user?: IUser;
  retourBonId?: number;
  reponseRetourBonItems?: IReponseRetourBonItem[];
}

export class ReponseRetourBon implements IReponseRetourBon {
  constructor(
    public id?: number,
    public dateMtv?: string,
    public user?: IUser,
    public retourBonId?: number,
    public reponseRetourBonItems?: IReponseRetourBonItem[],
  ) {
    this.reponseRetourBonItems = this.reponseRetourBonItems ?? [];
  }
}
