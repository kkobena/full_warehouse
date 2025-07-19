import {PeremptionStatut} from './peremption-statut';

export class ProductToDestroy {
  id: number;
  produitName: string;
  produitCodeCip: string;
  numLot: string;
  quantity: number;
  datePeremption: string;
  dateDestruction: string;
  user: string;
  createdDate: string;
  updatedDate: string;
  fournisseur: string;
  prixAchat: number;
  prixUni: number;
  peremptionStatut: PeremptionStatut;
  destroyed: boolean;
}

export class ProductToDestroyPayload {
  lotId?: number;
  produitId?: number;
  quantity?: number;
  datePeremption?: string;
  fournisseurId?: number;
  numLot?: string;
  editing?: boolean;
  magasinId?: number;
  id?: number;
  stockInitial?: number;
}

export class ProductsToDestroyPayload {
  magasinId?: number;
  products: ProductToDestroyPayload[];
}

export class ProductToDestroySum {
  quantity: number;
  valeurAchat: number;
  valeurVente: number;
  productCount: number;
}

export class ProductToDestroyFilter {
  fromDate?: string;
  toDate?: string;
  destroyed?: boolean;
  userId?: number;
  rayonId?: number;
  fournisseurId?: number;
  searchTerm?: string;
  storageId?: number;
  magasinId?: number;
  editing?: boolean = false;
  page?: number;
  size?: number;
}
