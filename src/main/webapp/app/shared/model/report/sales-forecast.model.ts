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
  averageMonthlyGrowthPct?: number;
  predictedYearlyGrowthPct?: number;
  modelAccuracyPct?: number;
  meanAbsoluteError?: number;
  seasonalityDetected?: boolean;
  peakMonth?: string;
  lowMonth?: string;
  forecastMethod?: string;
  dataPointsUsed?: number;
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
