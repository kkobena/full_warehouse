export interface IConcentrationOrganisme {
  organisme?: string;
  caTp?: number;
  nbFactures?: number;
  partPct?: number;
  delaiReglement?: number;
  stressImpact30j?: number;
}

export interface IConcentrationSummary {
  organismes?: IConcentrationOrganisme[];
  totalCaTp?: number;
  totalRegle?: number;
  totalImpaye?: number;
  hhiIndex?: number;
  riskLevel?: 'FAIBLE' | 'MODERE' | 'ELEVE';
}

export interface IConcentrationEvolutionSerie {
  organisme?: string;
  caValues?: number[];
}

export interface IConcentrationEvolution {
  labels?: string[];
  series?: IConcentrationEvolutionSerie[];
}
