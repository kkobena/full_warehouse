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
    public principal?: boolean
  ) {}
}
