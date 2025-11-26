export enum InvoiceStatus {
  PAID = 'PAID',
  UNPAID = 'UNPAID',
  PARTIAL = 'PARTIAL',
}

export enum AgeCategory {
  LESS_THAN_30 = 'LESS_THAN_30',
  BETWEEN_30_60 = 'BETWEEN_30_60',
  BETWEEN_60_90 = 'BETWEEN_60_90',
  MORE_THAN_90 = 'MORE_THAN_90',
}

export interface ITiersPayantInvoice {
  factureId?: number;
  numeroFacture?: string;
  dateFacture?: string;
  tiersPayantLibelle?: string;
  groupeTiersPayantLibelle?: string;
  montantFacture?: number;
  montantPaye?: number;
  montantRestant?: number;
  statut?: InvoiceStatus;
  daysSinceInvoice?: number;
  ageCategory?: AgeCategory;
}

export class TiersPayantInvoice implements ITiersPayantInvoice {
  constructor(
    public factureId?: number,
    public numeroFacture?: string,
    public dateFacture?: string,
    public tiersPayantLibelle?: string,
    public groupeTiersPayantLibelle?: string,
    public montantFacture?: number,
    public montantPaye?: number,
    public montantRestant?: number,
    public statut?: InvoiceStatus,
    public daysSinceInvoice?: number,
    public ageCategory?: AgeCategory,
  ) {}
}

export interface ITiersPayantCreancesSummary {
  groupeTiersPayantId?: number;
  groupeTiersPayantLibelle?: string;
  nombreFactures?: number;
  montantTotal?: number;
  montantMoinsDe30Jours?: number;
  montantEntre30Et60Jours?: number;
  montantEntre60Et90Jours?: number;
  montantPlusDe90Jours?: number;
}

export class TiersPayantCreancesSummary implements ITiersPayantCreancesSummary {
  constructor(
    public groupeTiersPayantId?: number,
    public groupeTiersPayantLibelle?: string,
    public nombreFactures?: number,
    public montantTotal?: number,
    public montantMoinsDe30Jours?: number,
    public montantEntre30Et60Jours?: number,
    public montantEntre60Et90Jours?: number,
    public montantPlusDe90Jours?: number,
  ) {}
}
