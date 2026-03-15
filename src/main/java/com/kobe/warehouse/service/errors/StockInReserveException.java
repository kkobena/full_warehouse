package com.kobe.warehouse.service.errors;

import java.io.Serial;

/**
 * Exception levée lorsque le stock du rayon (PRINCIPAL) est insuffisant pour couvrir
 * la demande, mais que la réserve (SAFETY_STOCK) dispose de stock disponible.
 *
 * <p>Deux cas déclencheurs :</p>
 * <ul>
 *   <li>Rayon = 0, réserve &gt; 0 : rupture rayon totale</li>
 *   <li>Rayon &gt; 0 mais rayon &lt; demande, réserve &gt; 0 : couverture partielle possible</li>
 * </ul>
 *
 * <p>Le payload {@link StockReserveInfo} contient rayonStock, reserveStock et totalAvailable,
 * permettant au client d'afficher les options :
 * <ol>
 *   <li>Transférer (demande - rayonStock) unités de réserve → rayon, puis vendre normalement</li>
 *   <li>Vendre totalAvailable et mettre (demande - totalAvailable) en avoir</li>
 *   <li>Vente en avoir totale (forceStock)</li>
 * </ol></p>
 */
public class StockInReserveException extends BadRequestAlertException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Payload envoyé au frontend décrivant la situation de stock.
     *
     * @param rayonStock       stock disponible dans le rayon (PRINCIPAL)
     * @param reserveStock     stock disponible dans la réserve (SAFETY_STOCK)
     * @param totalAvailable   rayonStock + reserveStock
     */
    public record StockReserveInfo(int rayonStock, int reserveStock, int totalAvailable) {}

    public StockInReserveException(int rayonStock, int reserveStock) {
        super(
            "Stock rayon insuffisant, réserve disponible",
            "stock.reserve.available",
            new StockReserveInfo(rayonStock, reserveStock, rayonStock + reserveStock)
        );
    }
}
