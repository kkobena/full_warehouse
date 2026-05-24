export interface IPnlSegment {
  segment?: string;
  segmentLabel?: string;
  ca?: number;
  coutAchat?: number;
  margeBrute?: number;
  tauxMarge?: number;
  nbTransactions?: number;
}

export interface IPnlFamille {
  famille?: string;
  ca?: number;
  coutAchat?: number;
  margeBrute?: number;
  tauxMarge?: number;
}

export interface IPnlEvolutionSerie {
  famille?: string;
  segment?: string;
  tauxMargeValues?: number[];
}

export interface IPnlEvolution {
  labels?: string[];
  series?: IPnlEvolutionSerie[];
}
