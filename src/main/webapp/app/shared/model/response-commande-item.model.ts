export interface IResponseCommandeItem {
  codeCip?: string;
  codeEan?: string;
  produitLibelle?: string;
  quantitePriseEnCompte?: number;
  quantite?: number;
}

export class ResponseCommandeItem implements IResponseCommandeItem {
  constructor(
    public codeCip?: string,
    public codeEan?: string,
    public produitLibelle?: string,
    public quantitePriseEnCompte?: number,
    public quantite?: number
  ) {}
}
