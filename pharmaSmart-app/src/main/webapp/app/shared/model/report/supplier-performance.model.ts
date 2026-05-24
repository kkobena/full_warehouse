export interface ISupplierPerformance {
  fournisseurId?: number;
  fournisseurName?: string;
  fournisseurCode?: string;
  phone?: string;
  mobile?: string;
  nbOrdersLast30Days?: number;
  purchaseAmountLast30Days?: number;
  nbOrdersLast12Months?: number;
  purchaseAmountLast12Months?: number;
  avgDeliveryDays?: number;
  minDeliveryDays?: number;
  maxDeliveryDays?: number;
  conformityRatePct?: number;
  performanceScore?: number;
}

export interface ISupplierPerformanceSummary {
  totalSuppliers?: number;
  totalPurchaseAmountLast12Months?: number;
  totalPurchaseAmountLast30Days?: number;
  totalOrdersLast12Months?: number;
  totalOrdersLast30Days?: number;
  avgDeliveryDays?: number;
  avgConformityRate?: number;
  suppliersWithGoodPerformance?: number;
  suppliersWithAveragePerformance?: number;
  suppliersWithPoorPerformance?: number;
}

export interface ISupplierEvolution {
  labels?: string[];
  montantsN?: number[];
  montantsN1?: number[];
  delaisN?: number[];
  delaisN1?: number[];
  nbCommandesN?: number[];
  nbCommandesN1?: number[];
}
