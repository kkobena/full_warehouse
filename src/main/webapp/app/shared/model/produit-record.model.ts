import dayjs from 'dayjs';
import { MonthEnum } from './enumerations/month-enum';

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

export class HistoriqueProduitVDonneesMensuelles {
  annee: number;
  quantites: Map<MonthEnum, number>;
}

export class HistoriqueProduitVente {
  mvtDate: Date;

  reference: string;

  quantite: number;

  prixUnitaire: number;

  montantNet: number;

  montantRemise: number;

  montantTtc: number;

  montantTva: number;

  montantHt: number;

  user: string;
}

export class HistoriqueProduitAchats {
  mvtDate: Date;

  reference: string;

  quantite: number;

  prixAchat: number;
  montantAchat: number;
  user: string;
}
export class HistoriqueProduitAchatsSummary {
  quantite: number;

  montantAchat: number;
}
export class HistoriqueProduitVenteSummary {
  quantite: number;
  montantNet: number;
  montantRemise: number;
  montantTtc: number;
  montantTva: number;
  montantHt: number;
}
export class HistoriqueProduitVenteMensuelleSummary {
  quantite: number;
}
