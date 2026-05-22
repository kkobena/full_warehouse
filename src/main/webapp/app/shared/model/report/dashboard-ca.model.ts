/**
 * Daily CA Summary
 */
export interface IDailyCA {
  saleDate?: string;
  nbTransactions?: number;
  nbAvoirs?: number;
  caTotal?: number;
  caAvoirs?: number;
  caNet?: number;
  panierMoyen?: number;
  coutTotal?: number;
  margeBrute?: number;
  tauxMargePct?: number;
  nbClients?: number;
  montantEncaisse?: number;
  montantCredit?: number;
}

/**
 * Payment Method CA Distribution
 */
export interface IPaymentMethodCA {
  paymentDate?: string;
  paymentMethod?: string;
  paymentCode?: string;
  nbPayments?: number;
  montantTotal?: number;
  montantAvoirs?: number;
  montantMoyen?: number;
}

/**
 * Product Family CA Distribution
 */
export interface IProductFamilyCA {
  saleDate?: string;
  famille?: string;
  quantiteVendue?: number;
  caTotal?: number;
  coutTotal?: number;
  margeBrute?: number;
  tauxMargePct?: number;
  nbLignesVente?: number;
}

/**
 * Dashboard CA Summary with KPIs
 */
export interface IDashboardCASummary {
  // Today
  caToday?: number;
  caTodayPrevious?: number;
  caTodayEvolutionPct?: number;

  // Week
  caWeek?: number;
  caWeekPrevious?: number;
  caWeekEvolutionPct?: number;

  // Month
  caMonth?: number;
  caMonthPrevious?: number;
  caMonthEvolutionPct?: number;

  // Year
  caYear?: number;
  caYearPrevious?: number;
  caYearEvolutionPct?: number;

  // Additional metrics
  nbTransactionsToday?: number;
  nbTransactionsWeek?: number;
  nbTransactionsMonth?: number;
  nbTransactionsYear?: number;

  panierMoyenToday?: number;
  panierMoyenWeek?: number;
  panierMoyenMonth?: number;
  panierMoyenYear?: number;

  tauxMargeToday?: number;
  tauxMargeWeek?: number;
  tauxMargeMonth?: number;
  tauxMargeYear?: number;
}

/**
 * Dashboard CA Evolution for charts
 */
export interface IDashboardCAEvolution {
  labels?: string[];
  caValues?: number[];
  caPreviousValues?: number[];
  transactionCounts?: number[];
  period?: 'daily' | 'weekly' | 'monthly';
}

/**
 * Payment Method Distribution Summary (aggregated for pie chart)
 */
export interface IPaymentMethodSummary {
  paymentMethod: string;
  paymentCode: string;
  montantTotal: number;
  nbPayments: number;
  percentage: number;
}

/**
 * Product Family Distribution Summary (aggregated for bar chart)
 */
export interface IProductFamilySummary {
  famille: string;
  caTotal: number;
  margeBrute: number;
  tauxMargePct: number;
  percentage: number;
}

/**
 * Average basket evolution over 12 months — GAP-C2
 */
export interface IBasketEvolution {
  labels?: string[];
  values?: number[];
  currentValue?: number;
  previousValue?: number;
  evolutionPct?: number;
  evolutionAmount?: number;
  bestMonthLabel?: string;
  bestMonthValue?: number;
  trend6MPct?: number;
}

/**
 * Sales performance by staff member (vendeur) — Phase 5
 */
export interface IPerformanceVendeur {
  vendeurId?: number;
  vendeurNom?: string;
  nombreVentes?: number;
  montantTotal?: number;
  ticketMoyen?: number;
  tauxRemise?: number;
}

/**
 * Remises (discount) KPIs — Phase 6
 */
export interface IRemisesAnalysisKpi {
  totalRemise?: number;
  caApresRemise?: number;
  tauxRemise?: number;
  nbVentesAvecRemise?: number;
  nbVentesTotal?: number;
}

export interface ITopRemiseProduit {
  libelle?: string;
  montantRemise?: number;
  nbVentes?: number;
}

/**
 * Generics vs branded substitution statistics — Phase 5
 */
export interface IGenericsSubstitution {
  totalProduits?: number;
  produitsGeneriques?: number;
  princepsAvecGenerique?: number;
  caTotal?: number;
  caGeneriques?: number;
  caPrincepsAvecGenerique?: number;
  tauxGeneriques?: number;
  tauxPrincepsSubstituables?: number;
  tauxCaGeneriques?: number;
}
