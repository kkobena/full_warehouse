export interface IRecapProduitVendu {
  id?: number;
  libelle?: string;
  codeCip?: string;
  codeEanLaboratoire?: string;
  rayonName?: string;
  quantitySold?: number;
  quantityAvoir?: number;
  totalSalesAmount?: number;
  totalPurchaseAmount?: number;
  stock?: number;
}

export interface IRecapProduitVenduSummary {
  totalProducts?: number;
  quantitySold?: number;
  quantityAvoir?: number;
  totalSalesAmount?: number;
  totalPurchaseAmount?: number;
  totalStock?: number;
}

export enum SeuilFilterType {
  GREATER_THAN = 'GREATER_THAN',
  LESS_THAN = 'LESS_THAN',
  LESS_THAN_OR_EQUAL_TO = 'LESS_THAN_OR_EQUAL_TO',
  GREATER_THAN_OR_EQUAL_TO = 'GREATER_THAN_OR_EQUAL_TO',
  EQUAL_TO = 'EQUAL_TO',
  SEUIL_MINI_ATTEINT = 'SEUIL_MINI_ATTEINT',
}

export enum StockFilterType {
  LESS_THAN = 'LESS_THAN',
  GREATER_THAN = 'GREATER_THAN',
  EQUAL_TO = 'EQUAL_TO',
  LESS_THAN_OR_EQUAL_TO = 'LESS_THAN_OR_EQUAL_TO',
  GREATER_THAN_OR_EQUAL_TO = 'GREATER_THAN_OR_EQUAL_TO',
  NOT_EQUAL_TO = 'NOT_EQUAL_TO',
  OUT_OF_STOCK = 'OUT_OF_STOCK',
}

export interface IRecapProduitVenduRequestParam {
  startDate: string;
  endDate: string;
  startTime?: string;
  endTime?: string;
  userId?: number;
  searchTerm?: string;
  rayonId?: number;
  fournisseurId?: number;
  seuilFilterType?: SeuilFilterType;
  stockFilterType?: StockFilterType;
  seuilValue?: number;
  stockValue?: number;
  quantitySold?: number;
  unitPriceLessThanPurchasePrice?: boolean;
  suggerQuantitySold?: boolean;
  page?: number;
  size?: number;
  isInvendu?: boolean;
}
