/**
 * Comparative CA DTO
 */
export interface IComparativeCA {
  period?: string;
  periodLabel?: string;
  currentCA?: number;
  previousCA?: number;
  evolutionPct?: number;
  evolutionAmount?: number;
  currentTransactions?: number;
  previousTransactions?: number;
  comparisonType?: 'MONTHLY' | 'QUARTERLY' | 'YEARLY';
}

/**
 * Comparative by Type DTO
 */
export interface IComparativeByType {
  saleType?: string;
  saleTypeLabel?: string;
  currentYearCA?: number;
  previousYearCA?: number;
  evolutionPct?: number;
  currentYearCount?: number;
  previousYearCount?: number;
}

/**
 * Comparative Summary DTO
 */
export interface IComparativeSummary {
  // Year to date
  ytdCurrentCA?: number;
  ytdPreviousCA?: number;
  ytdEvolutionPct?: number;

  // Last 12 months
  last12MonthsCA?: number;
  previous12MonthsCA?: number;
  last12MonthsEvolutionPct?: number;

  // Best/worst months
  bestMonthLabel?: string;
  bestMonthCA?: number;
  worstMonthLabel?: string;
  worstMonthCA?: number;

  // Averages
  avgMonthlyCA?: number;
  avgMonthlyEvolution?: number;
}
