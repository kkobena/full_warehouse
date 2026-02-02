import { ISalesLine, SalesLine } from '../../../../shared/model/sales-line.model';
import { ISales } from '../../../../shared/model/sales.model';
import { ProduitSearch } from '../../../../shared/model/produit.model';

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
  currentSale: ISales | null
): ISalesLine {
  // Calculer la quantité vendue:
  // - Ne peut pas dépasser le stock disponible
  // - Si stock négatif ou 0, quantitySold = 0 (pas de stock)
  // - Sinon, prendre le minimum entre stock et quantité demandée
  const availableStock = Math.max(0, product.totalQuantity || 0);
  const quantitySold = Math.min(availableStock, quantity);

  return {
    ...new SalesLine(),
    produitId: product.id,
    regularUnitPrice: product.regularUnitPrice,
    quantitySold,
    quantityRequested: quantity,
    saleId: currentSale?.id,
    sales: null,  // Toujours null pour correspondre au payload attendu par le backend
    saleCompositeId: currentSale?.saleId,
  };
}
