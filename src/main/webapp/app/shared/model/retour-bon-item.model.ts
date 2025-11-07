export interface IRetourBonItem {
  id?: number;
  dateMtv?: string;
  retourBonId?: number;
  motifRetourId?: number;
  motifRetourLibelle?: string;
  orderLineId?: number;
  orderLineOrderDate?: string;
  produitLibelle?: string;
  produitCip?: string;
  produitId?: number;
  qtyMvt?: number;
  initStock?: number;
  afterStock?: number;
  lotId?: number;
  lotNumero?: string;
  orderLineQuantityRequested?: number;
  orderLineQuantityReceived?: number;
}

export class RetourBonItem implements IRetourBonItem {
  constructor(
    public id?: number,
    public dateMtv?: string,
    public retourBonId?: number,
    public motifRetourId?: number,
    public motifRetourLibelle?: string,
    public orderLineId?: number,
    public orderLineOrderDate?: string,
    public produitLibelle?: string,
    public produitCip?: string,
    public produitId?: number,
    public qtyMvt?: number,
    public initStock?: number,
    public afterStock?: number,
    public lotId?: number,
    public lotNumero?: string,
    public orderLineQuantityRequested?: number,
    public orderLineQuantityReceived?: number,
  ) {}
}
