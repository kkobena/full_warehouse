import { DoughnutChartWrapper } from '../../../shared/model/doughnut-chart.model';

export class TaxeReport {
  mvtDate: any;
  montantHt: number;
  montantTaxe: number;
  montantTtc: number;
  montantNet: number;
  montantRemise: number;
  montantAchat: number;
  montantRemiseUg: number;
  montantTvaUg: number;
  codeTva: number;
  amountToBeTakenIntoAccount: number;
  montantTtcUg: number;
}

export class TaxeWrapper {
  montantHt: number;
  montantTaxe: number;
  montantTtc: number;
  montantNet: number;
  montantRemise: number;
  montantAchat: number;
  montantRemiseUg: number;
  montantTvaUg: number;
  amountToBeTakenIntoAccount: number;
  montantTtcUg: number;
  taxes: TaxeReport[];
  chart: DoughnutChartWrapper;
}
