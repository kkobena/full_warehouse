import { Moment } from 'moment';

export interface IAjustement {
  id?: number;
  qtyMvt?: number;
  dateMtv?: Moment;
  produitId?: number;
  ajustId?: number;
  produitlibelle?: string;
  codeCip?: string;
  userFullName?: string;
  stockBefore?: number;
  stockAfter?: number;
  commentaire?: string;
  motifAjustementId?: number;
  motifAjustementLibelle?: string;
}

export class Ajustement implements IAjustement {
  constructor(
    public id?: number,
    public qtyMvt?: number,
    public dateMtv?: Moment,
    public produitId?: number,
    public produitlibelle?: string,
    public commentaire?: string,
    public ajustId?: number,
    public userFullName?: string,
    public stockBefore?: number,
    public stockAfter?: number,
    public codeCip?: string,
    public motifAjustementId?: number,
    public motifAjustementLibelle?: string
  ) {}
}
