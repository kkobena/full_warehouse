import { ISalesLine, SalesLine } from '../../../../shared/model/sales-line.model';
import { ISales } from '../../../../shared/model';
import { ProduitSearch } from '../../../../shared/model';

/**
 * Utilitaires pour la création et manipulation de lignes de vente
 */

/**
 * Créer une ligne de vente à partir d'un produit
 *
 * Gère automatiquement:
 * - Stock disponible (quantitySold ne dépasse jamais totalQuantity)
 * - Stocks négatifs (quantitySold = 0 si stock <= 0)
 * - Association à la vente parente
 *
 * @param product - Produit à ajouter
 * @param quantity - Quantité demandée par l'utilisateur
 * @param currentSale - Vente en cours (peut être null si création)
 * @returns ISalesLine prête à être envoyée au backend
 */
export function createSalesLineFromProduct(
  product: ProduitSearch,
  quantity: number,
  currentSale: ISales | null,
  codeScan?: string | null,
): ISalesLine {
  return {
    ...new SalesLine(),
    produitId: product.id,
    regularUnitPrice: product.regularUnitPrice,
    quantityRequested: quantity,
    saleId: currentSale?.id,
    sales: null, // Toujours null pour correspondre au payload attendu par le backend
    saleCompositeId: currentSale?.saleId,
    codeScan: codeScan ?? null,
    // Informations CH nécessaires pour le déconditionnement (non envoyées au backend)
    produit: {
      id: product.id,
      produitId: product.parentId || undefined, // ID du conditionnement parent (CH)
      itemQty: product.itemQty,
    },
  };
}
