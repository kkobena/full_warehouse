export interface IOrderItem {
  produitCip?: string;
  produitEan?: string;
  produitLibelle?: string;
  referenceBonLivraison?: string;
  dateBonLivraison?: string;
  montant?: number;
  ug?: number;
  prixUn?: number;
  tva?: number;
  facture?: string;
  ligne?: number;
  prixAchat?: number;
  quantityReceived?: number;
  quantityRequested?: number;
}

export class OrderItem implements IOrderItem {
  constructor(
    public produitCip?: string,
    public produitEan?: string,
    public produitLibelle?: string,
    public referenceBonLivraison?: string,
    public dateBonLivraison?: string,
    public facture?: string,
    public montant?: number,
    public ug?: number,
    public prixUn?: number,
    public tva?: number,
    public ligne?: number,
    public prixAchat?: number,
    public quantityReceived?: number,
    public quantityRequested?: number
  ) {}
}
