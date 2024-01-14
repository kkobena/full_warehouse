import dayjs from 'dayjs';

export class ProductStatRecord {
  id?: number;
  roduitCount?: number;
  codeCip?: string;
  codeEan?: string;
  libelle?: string;
  quantitySold?: number;
  quantityUg?: number;
  netAmount?: number;
  costAmount?: number;
  salesAmount?: number;
  discountAmount?: number;
  montantTvaUg?: number;
  discountAmountHorsUg?: number;
  amountToBeTakenIntoAaccount?: number;
  taxAmount?: number;
  htAmount?: number;
  quantityAvg?: number;
  amountAvg?: number;
}

export class ProduitAuditingState {
  mvtDate: dayjs.Dayjs | null;
  initStock?: number;
  saleQuantity?: number;
  deleveryQuantity?: number;
  retourFournisseurQuantity?: number;
  perimeQuantity?: number;
  ajustementPositifQuantity?: number;
  ajustementNegatifQuantity?: number;
  deconPositifQuantity?: number;
  deconNegatifQuantity?: number;
  canceledQuantity?: number;
  retourDepot?: number;
  storeInventoryQuantity?: number;
  inventoryGap?: number;
  afterStock?: number;
}

export class ProduitAuditingParam {
  produitId?: number;
  fromDate?: string;
  toDate?: string;
}
