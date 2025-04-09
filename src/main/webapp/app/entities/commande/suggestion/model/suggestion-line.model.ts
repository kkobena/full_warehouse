import { EtatProduit } from '../../../../shared/model/etat-produit.model';

export class SuggestionLine {
  id: number;
  quantity: number;
  updatedAt: Date;
  fournisseurProduitLibelle: string;
  fournisseurProduitCip: string;
  fournisseurProduitCodeEan: string;
  produitId: number;
  fournisseurProduitId: number;
  currentStock: number;
  etatProduit: EtatProduit;
}
