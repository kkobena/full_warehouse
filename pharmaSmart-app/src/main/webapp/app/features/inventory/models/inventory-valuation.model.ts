export type ValuationGroupBy = 'STORAGE' | 'FAMILLE' | 'RAYON';

export interface IInventoryGlobalSummary {
  costValueBegin: number;
  costValueAfter: number;
  amountValueBegin: number;
  amountValueAfter: number;
  gapCost: number;
  gapAmount: number;
}

export interface IValuationGroup {
  groupKey: string;
  groupLabel: string;
  lineCount: number;
  costBefore: number;
  costAfter: number;
  amountBefore: number;
  amountAfter: number;
  gapCost: number;
  gapAmount: number;
}

export const VALUATION_GROUP_OPTIONS: { label: string; value: ValuationGroupBy }[] = [
  {label: 'Par emplacement', value: 'STORAGE'},
  {label: 'Par famille', value: 'FAMILLE'},
  {label: 'Par rayon', value: 'RAYON'},
];
