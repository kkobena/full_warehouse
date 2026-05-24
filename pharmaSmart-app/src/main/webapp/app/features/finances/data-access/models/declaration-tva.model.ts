export interface ITaxeDetailLine {
  codeTva: number;
  tauxLabel: string;
  baseHtVentes: number;
  tvaCollectee: number;
  baseHtAchats: number;
  tvaDeductible: number;
}

export interface IDeclarationTvaSummary {
  montantHt: number;
  tvaCollectee: number;
  tvaDeductible: number;
  tvaNette: number;
  montantTtc: number;
  taxes: ITaxeDetailLine[];
}

export interface IDeclarationTvaParams {
  startDate: string;
  endDate: string;
  typeTva?: string;
}
