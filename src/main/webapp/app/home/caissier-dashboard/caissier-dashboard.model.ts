import { HttpResponse } from '@angular/common/http';


export interface ICaisseStatus {
  fondOuverture: number;
  encaissementsEspeces: number;
  especesTheoriques: number;
  heureOuverture?: string;
  etat: 'OUVERTE' | 'FERMEE';
  derniereFermeture?: Date;

}

export type PaymentGroup = 'CASH' | 'MOBILE' | 'CB' | 'CHEQUE' | 'VIREMENT' | 'CREDIT' | 'CAUTION';

export interface IEncaissementParMode {
  code: string;
  libelle: string;
  paymentGroup: PaymentGroup;
  montant: number;
}

export interface ISessionEncaissements {
  lignes: IEncaissementParMode[];
  carnet: number;
  differe: number;
  totalEncaisse: number;
  totalARecouvrer: number;          // CREDIT/CAUTION + carnet + differe
  nombreTransactions: number;
}


export interface IDiffereARelancer {
  saleId: number;
  clientNom: string;
  clientTelephone?: string;
  montantDu: number;
  dateEcheance: Date;
  joursRetard: number;
  urgence: 'CRITIQUE' | 'AUJOURD_HUI' | 'RETARD';
}

export interface IResumeDifferes {
  nombreEcheancesAujourdhui: number;
  montantTotalDu: number;
  differes: IDiffereARelancer[];
}


export interface ILivraisonAttendue {
  commandeId: number;
  fournisseurNom: string;
  heureAttendue?: string;
  nombreReferences: number;
}


export interface IVenteRecente {
  saleId: number;
  numeroRecu: string;
  montant: number;
  dateVente: Date;
  modePaiement: string;
  typeVente: string;
  clientNom?: string;
}


export interface ICaissierDashboard {
  caisseStatus: ICaisseStatus;
  sessionEncaissements: ISessionEncaissements;
  resumeDifferes?: IResumeDifferes;
  livraisonsAttendues?: ILivraisonAttendue[];
  ventesRecentes?: IVenteRecente[];
}


export type DashboardResponseType = HttpResponse<ICaissierDashboard>;


