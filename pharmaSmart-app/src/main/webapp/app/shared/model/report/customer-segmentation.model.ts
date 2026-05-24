export enum CustomerClassification {
  CHAMPION = 'CHAMPION',
  LOYAL = 'LOYAL',
  BIG_SPENDER = 'BIG_SPENDER',
  ACTIVE = 'ACTIVE',
  AT_RISK = 'AT_RISK',
  NEED_ATTENTION = 'NEED_ATTENTION',
  INACTIVE = 'INACTIVE',
}

export interface ICustomerSegmentation {
  customerId?: number;
  customerName?: string;
  phone?: string;
  lastPurchaseDate?: string;
  daysSinceLastPurchase?: number;
  nbPurchasesLastYear?: number;
  totalSpentLastYear?: number;
  avgBasketValue?: number;
  recencyScore?: number;
  frequencyScore?: number;
  monetaryScore?: number;
  rfmSegment?: number;
  customerClassification?: CustomerClassification;
}
