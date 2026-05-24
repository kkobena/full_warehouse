export interface IStockValuation {
  produitId?: number;
  libelle?: string;
  codeCip?: string;
  categorie?: string;
  storageLocation?: string;
  stockQuantity?: number;
  purchasePrice?: number;
  salesPrice?: number;
  totalPurchaseValue?: number;
  totalSalesValue?: number;
  potentialMargin?: number;
  marginPercentage?: number;
}

export interface IStockValuationSummary {
  totalPurchaseValue?: number;
  totalSalesValue?: number;
  totalPotentialMargin?: number;
  averageMarginPercentage?: number;
  totalProducts?: number;
  totalQuantity?: number;
}
