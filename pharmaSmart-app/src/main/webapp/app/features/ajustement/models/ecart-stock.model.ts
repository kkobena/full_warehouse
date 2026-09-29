/** Produit dont le stock négatif dépasse ce que doivent les avoirs ouverts (EcartStockDTO). */
export interface IEcartStock {
  produitId: number;
  libelle: string;
  codeCip: string | null;
  /** Stock machine du magasin, UG comprises. */
  stock: number;
  /** Quantité encore due aux clients (avoirs ouverts). */
  quantiteDue: number;
  /** Unités sorties sans explication, toujours > 0. */
  ecart: number;
}
