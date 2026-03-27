import { TypeCommande } from 'app/shared/model/pharmaml.model';

/** Résultat retourné par le modal de commande à son appelant. */
export interface CommanderModalResult {
  type: 'INTERNE' | 'PHARMAML';
  fournisseurId?: number;
  fournisseurLibelle?: string;
  pharmamlParams?: {
    typeCommande: TypeCommande;
    dateLivraisonSouhaitee?: string;
    commentaire?: string;
  };
}
