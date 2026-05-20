export interface IVieillissementGlobal {
  totalEncours?: number;
  tranche0_30?: number;
  tranche31_60?: number;
  tranche61_90?: number;
  tranche90Plus?: number;
  nbFactures?: number;
  nbEnRetard?: number;
}

export interface IDsoOrganisme {
  organisme?: string;
  encours?: number;
  tranche0_30?: number;
  tranche31_60?: number;
  tranche61_90?: number;
  tranche90Plus?: number;
  nbFactures?: number;
  nbEnRetard?: number;
  dsoJours?: number;
  delaiReglement?: number;
  fiabilite?: 'BON' | 'SURVEILLER' | 'RISQUE';
}

export interface IEncoursMensuel {
  labels?: string[];
  montantFacture?: number[];
  encoursRestant?: number[];
}
