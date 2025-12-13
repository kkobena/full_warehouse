// Models for Caissier Dashboard

export interface IVentesJour {
  montantTotal: number;
  nombreVentes: number;
  montantEspeces: number;
  montantCB: number;
  montantCheque: number;
  montantMobileMoney: number;
  montantVirement: number;
  montantAssurance: number;
  ticketMoyen: number;
  objectifJour?: number;
  tauxAtteinte?: number;
}

export interface ICaisseStatus {
  soldeOuverture: number;
  soldeActuel: number;
  soldeAttendu: number;
  ecart: number;
  derniereFermeture?: Date;
  etat: 'OUVERTE' | 'FERMEE';
}

export interface IVenteRecente {
  saleId: number;
  numeroRecu: string;
  montant: number;
  dateVente: Date;
  modePaiement: string;
  vendeur?: string;
  nombreLignes: number;
  statut: string;
}

export interface ITopProduit {
  produitId: number;
  produitLibelle: string;
  codeCip?: string;
  quantiteVendue: number;
  montantTotal: number;
  nombreVentes: number;
}

export interface IPerformanceVendeur {
  vendeurId: number;
  vendeurNom: string;
  nombreVentes: number;
  montantTotal: number;
  ticketMoyen: number;
  tauxRemise: number;
}

export interface IAlerteCaisse {
  type: 'INFO' | 'ATTENTION' | 'URGENT' | 'OK';
  titre: string;
  message: string;
  horodatage: Date;
}

export interface IStatistiquesRapides {
  ventesEnCours: number;
  clientsServis: number;
  produitsVendus: number;
  tempsMoyenVente: number;
}

export interface ICaissierDashboard {
  ventesJour: IVentesJour;
  caisseStatus: ICaisseStatus;
  statistiquesRapides: IStatistiquesRapides;
  ventesRecentes?: IVenteRecente[];
  topProduits?: ITopProduit[];
  performanceVendeurs?: IPerformanceVendeur[];
  alertes?: IAlerteCaisse[];
}

export type VentesResponseType = HttpResponse<IVentesJour>;
export type CaisseResponseType = HttpResponse<ICaisseStatus>;
export type StatistiquesResponseType = HttpResponse<IStatistiquesRapides>;
export type VentesRecentesResponseType = HttpResponse<IVenteRecente[]>;
export type TopProduitsResponseType = HttpResponse<ITopProduit[]>;
export type PerformanceResponseType = HttpResponse<IPerformanceVendeur[]>;
export type AlertesResponseType = HttpResponse<IAlerteCaisse[]>;
export type DashboardResponseType = HttpResponse<ICaissierDashboard>;

import { HttpResponse } from '@angular/common/http';
