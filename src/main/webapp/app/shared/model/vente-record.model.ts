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
}

export class VenteRecordWrapper {
  close: VenteRecord;
  canceled: VenteRecord;
}
