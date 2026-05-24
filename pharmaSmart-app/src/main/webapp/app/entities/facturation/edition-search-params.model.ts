export class EditionSearchParams {
  modeEdition?: string;
  startDate: string;
  endDate: string;
  groupIds?: number[];
  tiersPayantIds?: number[];
  ids?: number[];
  all?: boolean;
  categorieTiersPayants?: string[];
  factureProvisoire?: boolean;
}

export class InvoiceSearchParams {
  search?: string;
  startDate: string;
  endDate: string;
  groupIds?: number[];
  tiersPayantIds?: number[];
  statuts?: string[];
  factureProvisoire?: boolean;
  factureGroupees?: boolean;
}
