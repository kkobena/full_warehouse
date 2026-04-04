import { PeremptionStatut } from './peremption-statut';

/** Représente la présence d'un lot dans un emplacement précis avec sa quantité disponible. */
export interface LotLocation {
  storageId: number;
  storageName: string;
  qty: number;
}

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
  /**
   * Localisations (LotStockLocation) où ce lot est présent avec du stock.
   * [] = pas de données multi-emplacement.
   * length === 1 = lot dans un seul emplacement → retrait ciblé automatique.
   * length > 1  = lot multi-site → l'utilisateur doit choisir l'emplacement.
   */
  locations: LotLocation[] = [];
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
