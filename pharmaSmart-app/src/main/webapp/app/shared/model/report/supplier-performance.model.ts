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

/**
 * Achats aupres d'un fournisseur sur la fenetre demandee. Le score et le delai restent des
 * indicateurs de qualite sur douze mois : une fenetre d'un jour ne saurait les mesurer.
 */
export interface ISupplierPurchase {
  fournisseurId?: number;
  fournisseurName?: string;
  nbCommandes?: number;
  montantAchat?: number;
  avgDeliveryDays?: number;
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
