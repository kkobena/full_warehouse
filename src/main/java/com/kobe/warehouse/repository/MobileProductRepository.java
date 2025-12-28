package com.kobe.warehouse.repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for mobile product queries.
 */
public interface MobileProductRepository {

    /**
     * Get basic product information.
     *
     * @param productId Product ID
     * @return Basic product info or null if not found
     */
    ProductBasicInfoProjection getBasicProductInfo(int productId);

    /**
     * Get stock information for a product with storage breakdown.
     *
     * @param productId Product ID
     * @return Stock info
     */
    StockInfoProjection getStockInfo(int productId);

    /**
     * Get price information for a product.
     *
     * @param productId Product ID
     * @return Price info
     */
    PriceInfoProjection getPriceInfo(int productId);

    /**
     * Get lot information for a product.
     *
     * @param productId Product ID
     * @return List of lots
     */
    List<LotInfoProjection> getLotInfo(int productId);

    /**
     * Get sales statistics for a product.
     *
     * @param productId Product ID
     * @param today Today's date
     * @param weekStart Week start date
     * @param monthStart Month start date
     * @return Sales stats
     */
    ProductSalesStatsProjection getSalesStats(int productId, LocalDate today, LocalDate weekStart, LocalDate monthStart);

    /**
     * Calculate average daily quantity sold.
     *
     * @param productId Product ID
     * @param lookbackDays Number of days to look back
     * @return Average daily quantity
     */
    double getAverageDailyQuantitySold(int productId, int lookbackDays);

    /**
     * Projection for basic product info.
     */
    record ProductBasicInfoProjection(
        long id,
        String name,
        String codeCip,
        String codeEan,
        String supplierName,
        Long supplierId,
        String familyName,
        Long familyId
    ) {}

    /**
     * Projection for stock info.
     */
    record StockInfoProjection(
        int totalQtyStock,
        int totalQtyUg,
        int totalQuantity,
        int minThreshold,
        int maxThreshold,
        List<StorageStockProjection> storageStocks
    ) {}

    /**
     * Projection for storage stock.
     */
    record StorageStockProjection(
        long storageId,
        String storageName,
        String storageType,
        int qtyStock,
        int qtyUg,
        int total
    ) {}

    /**
     * Projection for price info.
     */
    record PriceInfoProjection(
        int purchasePrice,
        int sellingPrice,
        double marginPercent,
        int vatRate
    ) {}

    /**
     * Projection for lot info.
     */
    record LotInfoProjection(
        long lotId,
        String numLot,
        LocalDate expiryDate,
        int quantityReceived,
        int daysUntilExpiry
    ) {}

    /**
     * Projection for product sales stats.
     */
    record ProductSalesStatsProjection(
        int todayQty,
        long todayAmount,
        int weekQty,
        long weekAmount,
        int monthQty,
        long monthAmount,
        double avgDailyQty
    ) {}
}
