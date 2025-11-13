export interface IReponseRetourBonItem {
  id?: number;
  dateMtv?: string;
  reponseRetourBonId?: number;
  retourBonItemId?: number;
  qtyMvt?: number;
  initStock?: number;
  afterStock?: number;
  // Display properties
  produitLibelle?: string;
  produitCip?: string;
  lotNumero?: string;
  requestedQty?: number;
   acceptedQty?: number;
}

export class ReponseRetourBonItem implements IReponseRetourBonItem {
  constructor(
    public id?: number,
    public dateMtv?: string,
    public reponseRetourBonId?: number,
    public retourBonItemId?: number,
    public qtyMvt?: number,
    public initStock?: number,
    public afterStock?: number,
    public produitLibelle?: string,
    public produitCip?: string,
    public lotNumero?: string,
    public requestedQty?: number,
    public acceptedQty?: number,
  ) {}
}
