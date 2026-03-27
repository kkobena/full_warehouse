import { IProduit } from './produit.model';

export interface IFournisseurProduit {
  id?: number;
  prixAchat?: number;
  prixUni?: number;
  fournisseurLibelle?: string;
  principal?: boolean;
  produitLibelle?: string;
  fournisseurId?: number;
  codeCip?: string;
  produitId?: number;
  produit?: IProduit;
  delaiLivraisonJours?: number;
  /** Nombre d'unités par colis (conditionnement fournisseur). 1 = pas de contrainte. */
  qteColis?: number;
  /** Quantité minimale de commande (en unités). 0 = pas de minimum. */
  qteMinimaleCommande?: number;
}

export class FournisseurProduit implements IFournisseurProduit {
  constructor(
    public id?: number,
    public prixAchat?: number,
    public prixUni?: number,
    public codeCip?: string,
    public fournisseurLibelle?: string,
    public produitLibelle?: string,
    public produitId?: number,
    public fournisseurId?: number,
    public principal?: boolean,
    public produit?: IProduit,
    public qteColis?: number,
    public qteMinimaleCommande?: number,
  ) {}
}
