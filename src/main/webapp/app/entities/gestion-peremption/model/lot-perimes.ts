import { PeremptionStatut } from './peremption-statut';

export class LotPerimes {
  id: number;
  numLot: string;
  founisseur: string;
  produitName: string;
  produitCode: string;
  datePeremption: string;
  quantity: number;
  prixAchat: number;
  prixVente: number;
  prixTotalVente: number;
  prixTotaAchat: number;
  statutPerime: string;
  rayonName: string;
  familleProduitName: string;
  peremptionStatut: PeremptionStatut;
  produitId: number;
}

export class LotFilterParam {
  dayCount?: number;
  produitId?: number;
  numLot?: string;
  searchTerm?: string;
  fromDate?: string;
  toDate?: string;
  fournisseurId?: number;
  rayonId?: number;
  familleProduitId?: number;
  magasinId?: number;
  storageId?: number;
  type?: string;
  page?: number;
  size?: number;
}

export class LotPerimeValeurSum {
  valeurAchat: number;
  valeurVente: number;
  quantite: number;
  count: number;
  retoursFourn: number;
  prochainesPerimes: number;
}
