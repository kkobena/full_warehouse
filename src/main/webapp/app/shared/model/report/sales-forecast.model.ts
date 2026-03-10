/**
 * Sales Forecast data point
 */
export interface ISalesForecast {
  forecastDate?: string;
  forecastPeriod?: string;
  forecastedCA?: number;
  actualCA?: number;
  confidenceLevel?: number;
  lowerBound?: number;
  upperBound?: number;
  forecastMethod?: ForecastMethod;
}

/**
 * Forecast Summary with statistics
 */
export interface IForecastSummary {
  totalForecastedCA3M?: number;
  totalForecastedCA6M?: number;
  totalForecastedCA12M?: number;
  averageMonthlyGrowthPct?: number | null;
  predictedYearlyGrowthPct?: number | null;
  modelAccuracyPct?: number | null;
  meanAbsoluteError?: number | null;
  seasonalityDetected?: boolean;
  peakMonth?: string | null;
  lowMonth?: string | null;
  forecastMethod?: string;
  dataPointsUsed?: number;
  /** INSUFFICIENT | LOW | MEDIUM | HIGH */
  dataQuality?: string;
}

export enum ForecastMethod {
  LINEAR_REGRESSION = 'LINEAR_REGRESSION',
  MOVING_AVERAGE = 'MOVING_AVERAGE',
  SEASONAL = 'SEASONAL',
  HISTORICAL = 'HISTORICAL',
}

export const FORECAST_METHOD_LABELS: Record<ForecastMethod, string> = {
  [ForecastMethod.LINEAR_REGRESSION]: 'Régression Linéaire',
  [ForecastMethod.MOVING_AVERAGE]: 'Moyenne Mobile',
  [ForecastMethod.SEASONAL]: 'Saisonnier',
  [ForecastMethod.HISTORICAL]: 'Historique',
};
