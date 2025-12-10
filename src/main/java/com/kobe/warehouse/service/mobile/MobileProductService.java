package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.service.dto.mobile.ExpiryStatus;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO.LotInfo;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO.PriceInfo;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO.SalesStats;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO.StockInfo;
import com.kobe.warehouse.service.dto.mobile.MobileProductQuickInfoDTO.StorageStock;
import com.kobe.warehouse.service.dto.mobile.StockStatus;
import com.kobe.warehouse.service.stock.ProduitService;
import com.kobe.warehouse.service.stock.dto.ProduitSearch;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for mobile product quick info.
 */
@Service
@Transactional(readOnly = true)
public class MobileProductService {

    private static final Logger LOG = LoggerFactory.getLogger(MobileProductService.class);

    private final EntityManager entityManager;
    private final ProduitService produitService;

    public MobileProductService(EntityManager entityManager, ProduitService produitService) {
        this.entityManager = entityManager;
        this.produitService = produitService;
    }

    /**
     * Get quick product information for mobile modal.
     *
     * @param productId Product ID
     * @return Product quick info DTO
     */
    public MobileProductQuickInfoDTO getProductQuickInfo(Integer productId) {
        LOG.debug("Getting product quick info for ID: {}", productId);

        // Get basic product info
        ProductBasicInfo basicInfo = getBasicProductInfo(productId);
        if (basicInfo == null) {
            return null;
        }

        // Get stock info
        StockInfo stockInfo = getStockInfo(productId);

        // Get price info
        PriceInfo priceInfo = getPriceInfo(productId);

        // Get lots info
        List<LotInfo> lots = getLotInfo(productId);

        // Get sales stats
        SalesStats salesStats = getSalesStats(productId);

        return MobileProductQuickInfoDTO.builder()
            .id(basicInfo.id())
            .name(basicInfo.name())
            .codeCip(basicInfo.codeCip())
            .codeEan(basicInfo.codeEan())
            .stock(stockInfo)
            .price(priceInfo)
            .lots(lots)
            .salesStats(salesStats)
            .supplierName(basicInfo.supplierName())
            .supplierId(basicInfo.supplierId())
            .familyName(basicInfo.familyName())
            .familyId(basicInfo.familyId())
            .build();
    }

    /**
     * Search products by name or code using optimized PostgreSQL function.
     *
     * @param query Search query
     * @param limit Max results
     * @return List of matching products
     */
    public List<ProduitSearch> searchProducts(String query, int limit) {
        return searchProducts(query, 1, limit); // Default magasin = 1
    }

    /**
     * Search products by name or code using optimized PostgreSQL function.
     *
     * @param query Search query
     * @param magasinId Magasin ID for stock lookup
     * @param limit Max results
     * @return List of matching products
     */
    public List<ProduitSearch> searchProducts(String query, int magasinId, int limit) {
        return produitService.searchProducts(query, magasinId, Pageable.ofSize(limit));
    }

    /**
     * Get basic product information.
     */
    private ProductBasicInfo getBasicProductInfo(Integer productId) {
        String sql =
            """
            SELECT
                p.id,
                p.libelle,
                fp.code_cip,
                p.code_ean_labo,
                f.libelle as supplier_name,
                f.id as supplier_id,
                fam.libelle as family_name,
                fam.id as family_id
            FROM produit p
            LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
            LEFT JOIN fournisseur f ON fp.fournisseur_id = f.id
            LEFT JOIN famille_produit fam ON p.famille_id = fam.id
            WHERE p.id = :productId
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("productId", productId);

        try {
            Object[] row = (Object[]) query.getSingleResult();
            return new ProductBasicInfo(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                row[5] != null ? ((Number) row[5]).longValue() : null,
                (String) row[6],
                row[7] != null ? ((Number) row[7]).longValue() : null
            );
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Get stock information with total and per-storage breakdown.
     */
    private StockInfo getStockInfo(Integer productId) {
        String sql =
            """
            SELECT
                s.id as storage_id,
                s.name as storage_name,
                s.storage_type,
                COALESCE(sp.qty_stock, 0) as qty_stock,
                COALESCE(sp.qty_ug, 0) as qty_ug,
                p.qty_seuil_mini,
                p.qty_appro
            FROM produit p
            CROSS JOIN storage s
            LEFT JOIN stock_produit sp ON s.id = sp.storage_id AND sp.produit_id = p.id
            WHERE p.id = :productId
              AND s.magasin_id = (SELECT magasin_id FROM storage WHERE storage_type = 'PRINCIPAL' LIMIT 1)
            ORDER BY
                CASE s.storage_type
                    WHEN 'PRINCIPAL' THEN 1
                    WHEN 'RESERVE' THEN 2
                    ELSE 3
                END
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("productId", productId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<StorageStock> storageStocks = new ArrayList<>();
        int totalQtyStock = 0;
        int totalQtyUg = 0;
        int minThreshold = 0;
        int maxThreshold = 0;

        for (Object[] row : results) {
            Long storageId = ((Number) row[0]).longValue();
            String storageName = (String) row[1];
            String storageType = (String) row[2];
            int qtyStock = ((Number) row[3]).intValue();
            int qtyUg = ((Number) row[4]).intValue();
            int storageTotal = qtyStock + qtyUg;

            // Get thresholds from first row
            if (storageStocks.isEmpty()) {
                minThreshold = row[5] != null ? ((Number) row[5]).intValue() : 0;
                maxThreshold = row[6] != null ? ((Number) row[6]).intValue() : 0;
            }

            totalQtyStock += qtyStock;
            totalQtyUg += qtyUg;

            storageStocks.add(new StorageStock(
                storageId,
                storageName,
                storageType,
                qtyStock,
                qtyUg,
                storageTotal
            ));
        }

        int totalQuantity = totalQtyStock + totalQtyUg;

        // Determine status based on total quantity using enum
        StockStatus stockStatus = StockStatus.fromQuantity(totalQuantity, minThreshold);

        // Calculate days of stock based on average daily sales
        int daysOfStock = calculateDaysOfStock(productId, totalQuantity);

        return new StockInfo(
            totalQuantity,
            totalQtyStock,
            totalQtyUg,
            minThreshold,
            maxThreshold,
            stockStatus.getCode(),
            stockStatus.getColor(),
            daysOfStock,
            storageStocks
        );
    }

    /**
     * Get price information.
     */
    private PriceInfo getPriceInfo(Integer productId) {
        String sql =
            """
            SELECT
                COALESCE(p.cost_amount, 0) as purchase_price,
                COALESCE(p.regular_unit_price, 0) as selling_price,
                COALESCE(t.taux, 0) as vat_rate
            FROM produit p
            LEFT JOIN tva t ON p.tva_id = t.id
            WHERE p.id = :productId
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("productId", productId);

        Object[] row = (Object[]) query.getSingleResult();

        int purchasePrice = ((Number) row[0]).intValue();
        int sellingPrice = ((Number) row[1]).intValue();
        int vatRate = ((Number) row[2]).intValue();

        double marginPercent = sellingPrice > 0
            ? BigDecimal.valueOf(((sellingPrice - purchasePrice) * 100.0) / sellingPrice).setScale(2, RoundingMode.HALF_UP).doubleValue()
            : 0;

        return new PriceInfo(purchasePrice, sellingPrice, marginPercent, vatRate);
    }

    /**
     * Get lot/batch information.
     */
    private List<LotInfo> getLotInfo(Integer productId) {
        String sql =
            """
            SELECT
                l.id,
                l.num_lot,
                l.expiry_date,
                l.quantity_received,
                (l.expiry_date - CURRENT_DATE) as days_until_expiry
            FROM lot l
            WHERE l.produit_id = :productId
              AND l.quantity_received > 0
            ORDER BY l.expiry_date
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("productId", productId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<LotInfo> lots = new ArrayList<>();
        for (Object[] row : results) {
            int daysUntilExpiry = ((Number) row[4]).intValue();

            // Determine expiry status using enum
            ExpiryStatus expiryStatus = ExpiryStatus.fromDaysUntilExpiry(daysUntilExpiry);

            lots.add(
                new LotInfo(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    ((java.sql.Date) row[2]).toLocalDate(),
                    ((Number) row[3]).intValue(),
                    daysUntilExpiry,
                    expiryStatus.getCode()
                )
            );
        }

        return lots;
    }

    /**
     * Get sales statistics.
     */
    private SalesStats getSalesStats(Integer productId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(7);
        LocalDate monthStart = today.minusDays(30);

        String sql =
            """
            SELECT
                COALESCE(SUM(CASE WHEN s.sale_date = :today THEN sl.quantity_sold ELSE 0 END), 0) as today_qty,
                COALESCE(SUM(CASE WHEN s.sale_date = :today THEN sl.sales_amount ELSE 0 END), 0) as today_amount,
                COALESCE(SUM(CASE WHEN s.sale_date >= :weekStart THEN sl.quantity_sold ELSE 0 END), 0) as week_qty,
                COALESCE(SUM(CASE WHEN s.sale_date >= :weekStart THEN sl.sales_amount ELSE 0 END), 0) as week_amount,
                COALESCE(SUM(sl.quantity_sold), 0) as month_qty,
                COALESCE(SUM(sl.sales_amount), 0) as month_amount
            FROM sales_line sl
            INNER JOIN sales s ON sl.sales_id = s.id
            WHERE sl.produit_id = :productId
              AND s.sale_date >= :monthStart
              AND s.statut = 'CLOSED'
              AND s.canceled = false
              AND s.ca = 'CA'
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("productId", productId);
        query.setParameter("today", today);
        query.setParameter("weekStart", weekStart);
        query.setParameter("monthStart", monthStart);

        Object[] row = (Object[]) query.getSingleResult();

        int todayQty = ((Number) row[0]).intValue();
        long todayAmount = ((Number) row[1]).longValue();
        int weekQty = ((Number) row[2]).intValue();
        long weekAmount = ((Number) row[3]).longValue();
        int monthQty = ((Number) row[4]).intValue();
        long monthAmount = ((Number) row[5]).longValue();

        double avgDailyQty = monthQty / 30.0;

        return new SalesStats(
            todayQty,
            todayAmount,
            weekQty,
            weekAmount,
            monthQty,
            monthAmount,
            BigDecimal.valueOf(avgDailyQty).setScale(1, RoundingMode.HALF_UP).doubleValue()
        );
    }

    /**
     * Calculate estimated days of stock.
     */
    private int calculateDaysOfStock(Integer productId, int currentStock) {
        if (currentStock == 0) {
            return 0;
        }

        String sql =
            """
            SELECT COALESCE(AVG(daily_qty), 0)
            FROM (
                SELECT SUM(sl.quantity_sold) as daily_qty
                FROM sales_line sl
                INNER JOIN sales s ON sl.sales_id = s.id
                WHERE sl.produit_id = :productId
                  AND s.sale_date >= CURRENT_DATE - 30
                  AND s.statut = 'CLOSED'
                  AND s.canceled = false
                  AND s.ca = 'CA'
                GROUP BY s.sale_date
            ) daily_sales
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("productId", productId);

        double avgDailyQty = ((Number) query.getSingleResult()).doubleValue();

        if (avgDailyQty <= 0) {
            return 999; // Essentially infinite
        }

        return (int) Math.floor(currentStock / avgDailyQty);
    }

    /**
     * Internal records.
     */
    private record ProductBasicInfo(
        Long id,
        String name,
        String codeCip,
        String codeEan,
        String supplierName,
        Long supplierId,
        String familyName,
        Long familyId
    ) {}
}
