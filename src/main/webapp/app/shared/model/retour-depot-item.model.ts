export interface IRetourDepotItem {
  id?: number;
  retourDepotId?: number;
  produitLibelle?: string;
  produitCip?: string;
  produitId?: number;
  qtyMvt?: number;
  regularUnitPrice?: number;
  initStock?: number;
  afterStock?: number;
  totalQuantity?: number;
}

export class RetourDepotItem implements IRetourDepotItem {
  constructor(
    public id?: number,
    public retourDepotId?: number,
    public produitLibelle?: string,
    public produitCip?: string,
    public produitId?: number,
    public qtyMvt?: number,
    public regularUnitPrice?: number,
    public initStock?: number,
    public afterStock?: number,
    public totalQuantity?: number,
  ) {}
}
