/**
 * Product Association for Market Basket Analysis
 */
export interface IProductAssociation {
  productAId?: number;
  productAName?: string;
  productACodeCip?: string;
  productBId?: number;
  productBName?: string;
  productBCodeCip?: string;
  transactionsWithBoth?: number;
  transactionsWithA?: number;
  transactionsWithB?: number;
  support?: number; // % of transactions containing both products
  confidence?: number; // % of transactions with A that also have B
  lift?: number; // Likelihood that B is bought when A is bought (lift > 1 = positive correlation)
}

/**
 * Market Basket Summary Statistics
 */
export interface IMarketBasketSummary {
  totalTransactions?: number;
  totalProducts?: number;
  totalAssociations?: number;
  averageBasketSize?: number;
  maxConfidence?: number;
  maxLift?: number;
  mostFrequentPair?: string;
}
