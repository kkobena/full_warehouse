export type TypeCommande = 'NORMALE' | 'EXCEPTIONNELLE';
export type PharmaMlStatut = 'PENDING' | 'SUBMITTED' | 'PARTIAL' | 'REJECTED' | 'ERROR';

export interface IPharmaMlEnvoi {
  id: number;
  statut: PharmaMlStatut;
  refMessage: string | null;
  tentatives: number;
  derniereTentative: string | null;
  totalLignes: number | null;
  lignesAcceptees: number | null;
  lignesRupture: number | null;
  createdAt: string;
  fournisseurLibelle: string;
}

export interface IEnvoiPharmaParams {
  commandeId: { id: number; orderDate: string };
  grossisteId?: number;
  dateLivraisonSouhaitee?: string;
  typeCommande: TypeCommande;
  commentaire?: string;
  ruptureId?: number;
}

export interface IPharmamlCommandeResponse {
  success: boolean;
  totalProduit: number;
  successCount: number;
  outOfStockCount: number;
  reliquatCommandeId?: number;
}

export interface IPharmamlRupture {
  codeProduit: string;
  designation: string;
  codeReponse: string;
  additif?: string;
  remplacant?: IPharmamlRemplacant;
}

export interface IPharmamlRemplacant {
  typeRemplacement: string;
  typeCodification: string;
  codeProduit: string;
  designation: string;
}

export interface IVerificationItem {
  codeCip: string;
  codeEan: string;
  produitLibelle: string;
  quantitePriseEnCompte: number;
  quantite: number;
}

export interface IVerificationResponse {
  items: IVerificationItem[];
  extraItems: IVerificationItem[];
}

export interface IInfoProduit {
  codeProduit: string;
  designation: string | null;
  stockDisponible: number;
  prixAchat: number;
  disponible: boolean;
}

export interface IDispoGrossisteResult {
  grossisteId: number;
  fournisseurLibelle: string | null;
  produits: IInfoProduit[];
}

export type MotifRetour = 'AVARIE' | 'NON_CONFORME' | 'PERIME' | 'ERREUR_LIVRAISON' | 'EXCEDENT';

export interface ILigneRetour {
  codeProduit: string;
  quantite: number;
  motifRetour: MotifRetour;
}

export type SubstitutionStatut = 'EN_ATTENTE' | 'ACCEPTEE' | 'REFUSEE';

export interface ISubstitutionProposee {
  id: number;
  cipPropose: string;
  designation: string | null;
  typeCodification: string | null;
  quantite: number;
  statut: SubstitutionStatut;
  cipOriginal: string;
  designationOriginale: string;
  createdAt: string;
  codeReponse: string | null;
  additif: string | null;
  typeRemplacement: string | null;
  substitutConnu: boolean;
}
