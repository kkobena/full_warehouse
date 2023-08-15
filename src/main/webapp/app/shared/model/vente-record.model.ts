export class VenteRecord {
  salesAmount: number;
  amountToBePaid: number;
  discountAmount: number;
  costAmount: number;
  marge: number;
  amountToBeTakenIntoAccount: number;
  netAmount: number;
  htAmount: number;
  partAssure: number;
  partTiersPayant: number;
  taxAmount: number;
  restToPay: number;
  htAmountUg: number;
  discountAmountHorsUg: number;
  discountAmountUg: number;
  netUgAmount: number;
  margeUg: number;
  montantttcUg: number;
  payrollAmount: number;
  montantTvaUg: number;
  montantnetUg: number;
  paidAmount: number;
  realNetAmount: number;
  saleCount: number;
  panierMoyen: number;
}

export class VenteRecordWrapper {
  close: VenteRecord;
  canceled: VenteRecord;
}

export class VenteByTypeRecord {
  typeVente: string;
  venteRecord: VenteRecord;
}

export class VentePeriodeRecord {
  dateMvt: string;
  statut: string;
  venteRecord: VenteRecord;
}

export class VenteModePaimentRecord {
  code: string;
  libelle: string;
  netAmount: number;
  paidAmount: number;
}
