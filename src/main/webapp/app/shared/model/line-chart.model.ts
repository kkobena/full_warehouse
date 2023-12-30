export class LineChartData {
  data?: any[];
}

export class LineChartWrapper {
  labeles?: string[];
  saleCostAmount?: LineChartData;
  saleAmount?: LineChartData;
  marge?: LineChartData;
  tva?: LineChartData;
}

export class LineChart {
  data?: any;
  options?: any;
}
