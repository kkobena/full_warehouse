package com.kobe.warehouse.service.dto.mobile;

import java.time.LocalDate;
import java.util.List;

/**
 * Optimized product info for mobile quick view.
 * Contains all essential information for a product modal.
 */
public record MobileProductQuickInfoDTO(
    Long id,
    String name,
    String codeCip,
    String codeEan,

    // Stock info
    StockInfo stock,

    // Price info
    PriceInfo price,

    // Lot/expiry info
    List<LotInfo> lots,

    // Recent sales stats
    SalesStats salesStats,

    // Supplier info
    String supplierName,
    Long supplierId,

    // Family/category
    String familyName,
    Long familyId
) {
    /**
     * Stock information with total and per-storage breakdown.
     */
    public record StockInfo(
        int totalQuantity,      // Total stock (qty_stock + qty_ug) across all storages
        int totalQtyStock,      // Total qty_stock only
        int totalQtyUg,         // Total qty_ug only
        int minThreshold,
        int maxThreshold,
        String status,          // OK, LOW, RUPTURE
        String statusColor,     // green, orange, red
        int daysOfStock,        // Estimated days before rupture
        List<StorageStock> storageStocks  // Stock by storage
    ) {
        /**
         * Constructor for backward compatibility.
         */
        public StockInfo(int currentQuantity, int minThreshold, int maxThreshold,
                         String status, String statusColor, int daysOfStock) {
            this(currentQuantity, currentQuantity, 0, minThreshold, maxThreshold,
                 status, statusColor, daysOfStock, List.of());
        }
    }

    /**
     * Stock per storage.
     */
    public record StorageStock(
        Long storageId,
        String storageName,
        String storageType,     // PRINCIPAL, RESERVE, etc.
        int qtyStock,
        int qtyUg,
        int totalQuantity       // qtyStock + qtyUg
    ) {}

    /**
     * Price information.
     */
    public record PriceInfo(
        int purchasePrice,
        int sellingPrice,
        double marginPercent,
        int vatRate
    ) {}

    /**
     * Lot/batch information.
     */
    public record LotInfo(
        Long id,
        String lotNumber,
        LocalDate expiryDate,
        int quantity,
        int daysUntilExpiry,
        String expiryStatus   // OK, WARNING, CRITICAL
    ) {}

    /**
     * Sales statistics.
     */
    public record SalesStats(
        int todayQuantity,
        long todayAmount,
        int weekQuantity,
        long weekAmount,
        int monthQuantity,
        long monthAmount,
        double averageDailyQuantity
    ) {}

    /**
     * Builder for creating product quick info.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String codeCip;
        private String codeEan;
        private StockInfo stock;
        private PriceInfo price;
        private List<LotInfo> lots;
        private SalesStats salesStats;
        private String supplierName;
        private Long supplierId;
        private String familyName;
        private Long familyId;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder codeCip(String codeCip) {
            this.codeCip = codeCip;
            return this;
        }

        public Builder codeEan(String codeEan) {
            this.codeEan = codeEan;
            return this;
        }

        public Builder stock(StockInfo stock) {
            this.stock = stock;
            return this;
        }

        public Builder price(PriceInfo price) {
            this.price = price;
            return this;
        }

        public Builder lots(List<LotInfo> lots) {
            this.lots = lots;
            return this;
        }

        public Builder salesStats(SalesStats salesStats) {
            this.salesStats = salesStats;
            return this;
        }

        public Builder supplierName(String supplierName) {
            this.supplierName = supplierName;
            return this;
        }

        public Builder supplierId(Long supplierId) {
            this.supplierId = supplierId;
            return this;
        }

        public Builder familyName(String familyName) {
            this.familyName = familyName;
            return this;
        }

        public Builder familyId(Long familyId) {
            this.familyId = familyId;
            return this;
        }

        public MobileProductQuickInfoDTO build() {
            return new MobileProductQuickInfoDTO(
                id, name, codeCip, codeEan, stock, price, lots, salesStats,
                supplierName, supplierId, familyName, familyId
            );
        }
    }
}
