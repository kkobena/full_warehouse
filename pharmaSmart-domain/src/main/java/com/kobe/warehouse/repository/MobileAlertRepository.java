package com.kobe.warehouse.repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for mobile alert queries.
 */
public interface MobileAlertRepository {

    /**
     * Get count of products in stock rupture.
     *
     * @return Number of products with zero stock
     */
    int getStockRuptureCount();

    /**
     * Get stock rupture details.
     *
     * @return List of products with zero stock
     */
    List<StockRuptureProjection> getStockRuptureAlerts();

    /**
     * Get count of products expiring within days.
     *
     * @param days Number of days until expiry
     * @return Number of expiring products
     */
    int getExpiringProductsCount(int days);

    /**
     * Get expiry alert details.
     *
     * @param days Number of days until expiry
     * @return List of expiring products
     */
    List<ExpiryAlertProjection> getExpiryAlerts(int days);

    /**
     * Get cash discrepancy amount for a date.
     *
     * @param date Date to check
     * @return Cash discrepancy amount
     */
    long getCashDiscrepancyAmount(LocalDate date);

    /**
     * Get count of overdue invoices.
     *
     * @param days Number of days overdue
     * @return Number of overdue invoices
     */
    int getOverdueInvoicesCount(int days);

    /**
     * Get overdue invoice alert details.
     *
     * @param days Number of days overdue
     * @return List of overdue invoices
     */
    List<OverdueInvoiceProjection> getOverdueInvoiceAlerts(int days);

    /**
     * Projection for stock rupture.
     */
    record StockRuptureProjection(
        long productId,
        String productName,
        String codeCip,
        int totalQtyStock,
        int totalQtyUg
    ) {}

    /**
     * Projection for expiry alert.
     */
    record ExpiryAlertProjection(
        long lotId,
        long productId,
        String productName,
        String numLot,
        LocalDate expiryDate,
        int currentQuantity,
        int daysUntilExpiry
    ) {}

    /**
     * Projection for overdue invoice.
     */
    record OverdueInvoiceProjection(
        long invoiceId,
        LocalDate invoiceDate,
        String tiersPayantName,
        String telephone,
        long montantFacture,
        long montantRegle,
        int daysOverdue
    ) {}
}
