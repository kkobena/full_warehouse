export enum StockAlertType {
  RUPTURE = 'RUPTURE',
  ALERTE = 'ALERTE',
  PEREMPTION = 'PEREMPTION',
}

export interface IStockAlert {
  produitId?: number;
  libelle?: string;
  codeCip?: string;
  stockQuantity?: number;
  seuilMin?: number;
  expiryDate?: string;
  alertType?: StockAlertType;
}

export class StockAlert implements IStockAlert {
  constructor(
    public produitId?: number,
    public libelle?: string,
    public codeCip?: string,
    public stockQuantity?: number,
    public seuilMin?: number,
    public expiryDate?: string,
    public alertType?: StockAlertType,
  ) {}
}
