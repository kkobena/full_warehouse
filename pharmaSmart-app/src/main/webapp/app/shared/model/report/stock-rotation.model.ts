/**
 * Categorie ABC pour l'analyse de rotation de stock
 * Basée sur le score Z statistique (distribution normale)
 */
export enum CategorieABC {
  /** Produits à forte rotation (z >= 1.96, top ~2.5%) */
  A = 'A',
  /** Produits à rotation moyenne (z >= 1.65, top ~5%) */
  B = 'B',
  /** Produits à faible rotation (z < 1.65) */
  C = 'C',
}

export interface IStockRotation {
  produitId?: number;
  libelle?: string;
  codeCip?: string;
  categorie?: string;
  stockQuantity?: number;
  unitCost?: number;
  stockValue?: number;
  caLast30Days?: number;
  qtySoldLast30Days?: number;
  nbSalesLast30Days?: number;
  caLast12Months?: number;
  qtySoldLast12Months?: number;
  rotationRateAnnual?: number;
  avgDaysInStock?: number;
  categorieABC?: CategorieABC;
}
