export interface IClientRetentionKpi {
  totalClients?: number;
  clientsActifs?: number;
  clientsARisque?: number;
  clientsPerdus?: number;
  caMoyenParClient?: number;
}

export type RetentionSegment = 'ACTIF' | 'RISQUE' | 'PERDU';

export interface IClientRetentionRow {
  clientId?: number;
  nom?: string;
  premiereVisite?: string;
  derniereVisite?: string;
  nbAchats?: number;
  caTotal?: number;
  joursAbsence?: number;
  segment?: RetentionSegment;
}
