import { HttpResponse } from '@angular/common/http';

// Models for Vendeur Dashboard

export interface IMesPerformances {
  caJour: number;
  caObjectif: number;
  tauxAtteinte: number;
  rang: number;
  totalVendeurs: number;
  badge: 'BRONZE' | 'ARGENT' | 'OR' | 'PLATINE';
  progression: number; // Progression vs hier en %
}

export interface IMesClients {
  clientsServis: number;
  nouveauxClients: number;
  clientsFideles: number;
  tauxFidelisation: number;
  panierMoyen: number;
}

export interface IVentesParType {
  ordonnance: number;
  conseil: number;
  parapharmacie: number;
  total: number;
}

export interface ICommission {
  montantJour: number;
  montantMois: number;
  tauxCommission: number;
  objectifCommission: number;
}

export interface ITopProduitVendeur {
  produitId: number;
  produitLibelle: string;
  codeCip?: string;
  quantiteVendue: number;
  montantTotal: number;
  marge: number;
  nombreVentes: number;
}

export interface IOpportuniteVente {
  type: 'ABONNEMENT' | 'COMPLEMENTAIRE' | 'FORT_POTENTIEL';
  titre: string;
  description: string;
  nombreClients: number;
  potentielCA: number;
}

export interface IObjectifMensuel {
  libelle: string;
  valeurActuelle: number;
  valeurCible: number;
  unite: string;
  tauxAtteinte: number;
}

export interface IClientFidele {
  clientId: number;
  clientNom: string;
  nombreAchats: number;
  montantTotal: number;
  dernierAchat: Date;
  categorieClient: 'VIP' | 'FIDELE' | 'POTENTIEL';
}

export interface IVenteRecenteVendeur {
  saleId: number;
  numeroRecu: string;
  montant: number;
  dateVente: Date;
  clientNom?: string;
  nombreArticles: number;
  typeVente: 'ORDONNANCE' | 'CONSEIL' | 'PARAPHARMACIE';
}

export interface IVendeurDashboard {
  mesPerformances: IMesPerformances;
  mesClients: IMesClients;
  ventesParType: IVentesParType;
  commission?: ICommission;
  topProduits?: ITopProduitVendeur[];
  ventesRecentes?: IVenteRecenteVendeur[];
  opportunites?: IOpportuniteVente[];
  objectifsMensuels?: IObjectifMensuel[];
  clientsFideles?: IClientFidele[];
}

// Response Types
export type PerformancesResponseType = HttpResponse<IMesPerformances>;
export type ClientsResponseType = HttpResponse<IMesClients>;
export type VentesParTypeResponseType = HttpResponse<IVentesParType>;
export type CommissionResponseType = HttpResponse<ICommission>;
export type TopProduitsVendeurResponseType = HttpResponse<ITopProduitVendeur[]>;
export type VentesRecentesVendeurResponseType = HttpResponse<IVenteRecenteVendeur[]>;
export type OpportunitesResponseType = HttpResponse<IOpportuniteVente[]>;
export type ObjectifsMensuelsResponseType = HttpResponse<IObjectifMensuel[]>;
export type ClientsFidelesResponseType = HttpResponse<IClientFidele[]>;
export type VendeurDashboardResponseType = HttpResponse<IVendeurDashboard>;
