// KPI Models
export interface IStockAlerts {
  rupture: number;
  stockCritique: number;
  bientotEnRupture: number;
  reassortStockRayon: number;
}

export interface ICommandesEnCours {
  enAttente: number;
  aReceptionner: number;
  totalMontant: number;
}

export interface IPeremptions {
  unMois: number;
  unATroisMois: number;
  troisASixMois: number;
  valeurTotale: number;
}

export interface IRotationStock {
  rotationMoyenne: number;
  rapide: number; // > 4x
  normal: number; // 2-4x
  lent: number; // < 2x
}

// Suggestion de réapprovisionnement
export interface ISuggestionReappro {
  produitId: number;
  produitLibelle: string;
  codeCip?: string;
  stockActuel: number;
  consommationMoyenne: number;
  quantiteSuggeree: number;
  fournisseurId?: number;
  fournisseurName?: string;
  delaiLivraison?: number;
  prixUnitaire?: number;
  selected?: boolean;
}

// Commande à réceptionner
export interface ICommandeAReceptionner {
  commandeId: number;
  commandeNumero: string;
  fournisseurName: string;
  montantTotal: number;
  nombreArticles: number;
  dateCommande: Date;
  dateLivraisonPrevue?: Date;
  statut: string;
}

// Analyse ABC
export interface IAnalyseABC {
  classeA: {
    nombreProduits: number;
    pourcentageProduits: number;
    pourcentageCA: number;
    valeur: number;
  };
  classeB: {
    nombreProduits: number;
    pourcentageProduits: number;
    pourcentageCA: number;
    valeur: number;
  };
  classeC: {
    nombreProduits: number;
    pourcentageProduits: number;
    pourcentageCA: number;
    valeur: number;
  };
}

// Performance fournisseur
export interface IPerformanceFournisseur {
  fournisseurId: number;
  fournisseurName: string;
  nombreCommandes: number;
  delaiMoyenJours: number;
  tauxConformite: number;
  caAnnuel: number;
  note: number; // 1-5 étoiles
}

// Alert notification
export interface IAlertNotification {
  type: 'URGENT' | 'ATTENTION' | 'INFO' | 'OK';
  message: string;
  count?: number;
  actions?: {
    label: string;
    action: string;
  }[];
}

// Dashboard summary
export interface IResponsableCommandeDashboard {
  stockAlerts: IStockAlerts;
  commandesEnCours: ICommandesEnCours;
  peremptions: IPeremptions;
  rotationStock: IRotationStock;
  suggestions?: ISuggestionReappro[];
  commandesAReceptionner?: ICommandeAReceptionner[];
  analyseABC?: IAnalyseABC;
  performanceFournisseurs?: IPerformanceFournisseur[];
  notifications?: IAlertNotification[];
}
