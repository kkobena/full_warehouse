import { Moment } from 'moment';
import { IAjustement } from './ajustement.model';

export interface IAjust {
  id?: number;
  dateMtv?: Moment;
  storageId?: number;
  userId?: number;
  storageLibelle?: string;
  userFullName?: string;
  commentaire?: string;
  ajustements?: IAjustement[];
}

export class Ajust implements IAjust {
  constructor(
    public id?: number,
    public userId?: number,
    public dateMtv?: Moment,
    public storageId?: number,
    public storageLibelle?: string,
    public userFullName?: string,
    public commentaire?: string,
    public ajustements?: IAjustement[]
  ) {}
}
