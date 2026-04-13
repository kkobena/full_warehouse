import { HttpResponse } from '@angular/common/http';

// ─── Zone 1 : État Caisse ─────────────────────────────────────────────────────
export interface ICaisseStatus {
  fondOuverture: number;           // initAmount  — fond de départ
  encaissementsEspeces: number;    // cashAmount  — espèces encaissées du jour
  especesTheoriques: number;       // estimateAmount — espèces théoriques en caisse
  heureOuverture?: string;         // beginTime   — ex: "08h30"
  etat: 'OUVERTE' | 'FERMEE';
  derniereFermeture?: Date;
  // NE PAS EXPOSER : gap (écart) — réservé au manager
}

// ─── Zone 2 : Encaissements de MA Session ────────────────────────────────────
export type PaymentGroup = 'CASH' | 'MOBILE' | 'CB' | 'CHEQUE' | 'VIREMENT' | 'CREDIT' | 'CAUTION';

export interface IEncaissementParMode {
  code: string;
  libelle: string;
  paymentGroup: PaymentGroup;
  montant: number;
}

export interface ISessionEncaissements {
  lignes: IEncaissementParMode[];   // une ligne par mode utilisé dans la session
  carnet: number;
  differe: number;
  totalEncaisse: number;            // somme des groupes CASH/MOBILE/CB/CHEQUE/VIREMENT
  totalARecouvrer: number;          // CREDIT/CAUTION + carnet + differe
  nombreTransactions: number;
}

// ─── Zone 3 : Différés à relancer ────────────────────────────────────────────
export interface IDiffereARelancer {
  saleId: number;
  clientNom: string;
  clientTelephone?: string;
  montantDu: number;
  dateEcheance: Date;
  joursRetard: number;             // 0 = aujourd'hui, >0 = retard
  urgence: 'CRITIQUE' | 'AUJOURD_HUI' | 'RETARD';
}

export interface IResumeDifferes {
  nombreEcheancesAujourdhui: number;
  montantTotalDu: number;
  differes: IDiffereARelancer[];
}

// ─── Zone 4 : Livraisons attendues du jour ───────────────────────────────────
export interface ILivraisonAttendue {
  commandeId: number;
  fournisseurNom: string;
  heureAttendue?: string;
  nombreReferences: number;
}

// ─── Zone 5 : Dernières transactions de la session ───────────────────────────
export interface IVenteRecente {
  saleId: number;
  numeroRecu: string;
  montant: number;
  dateVente: Date;
  modePaiement: string;
  typeVente: string;               // COMPTANT | ASSURANCE | CARNET | DIFFERE
  clientNom?: string;              // Nom du client ou null si vente anonyme
}

// ─── Wrapper Dashboard ───────────────────────────────────────────────────────
export interface ICaissierDashboard {
  caisseStatus: ICaisseStatus;
  sessionEncaissements: ISessionEncaissements;
  resumeDifferes?: IResumeDifferes;
  livraisonsAttendues?: ILivraisonAttendue[];
  ventesRecentes?: IVenteRecente[];
}

// Response types
export type DashboardResponseType = HttpResponse<ICaissierDashboard>;
export type CaisseResponseType = HttpResponse<ICaisseStatus>;
export type SessionEncaissementsResponseType = HttpResponse<ISessionEncaissements>;
export type DifferesResponseType = HttpResponse<IResumeDifferes>;
export type LivraisonsResponseType = HttpResponse<ILivraisonAttendue[]>;
export type VentesRecentesResponseType = HttpResponse<IVenteRecente[]>;

