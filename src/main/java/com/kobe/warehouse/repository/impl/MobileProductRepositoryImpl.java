package com.kobe.warehouse.repository.impl;

import com.kobe.warehouse.repository.MobileProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Implementation of MobileProductRepository using native SQL queries.
 */
@Repository
public class MobileProductRepositoryImpl implements MobileProductRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ProductBasicInfoProjection getBasicProductInfo(int productId) {
        String sql = """
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
            return new ProductBasicInfoProjection(
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

    @Override
    public StockInfoProjection getStockInfo(int productId) {
        String sql = """
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

        List<StorageStockProjection> storageStocks = new ArrayList<>();
        int totalQtyStock = 0;
        int totalQtyUg = 0;
        int minThreshold = 0;
        int maxThreshold = 0;

        for (Object[] row : results) {
            long storageId = ((Number) row[0]).longValue();
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

            storageStocks.add(new StorageStockProjection(
                storageId,
                storageName,
                storageType,
                qtyStock,
                qtyUg,
                storageTotal
            ));
        }

        int totalQuantity = totalQtyStock + totalQtyUg;

        return new StockInfoProjection(
            totalQtyStock,
            totalQtyUg,
            totalQuantity,
            minThreshold,
            maxThreshold,
            storageStocks
        );
    }

    @Override
    public PriceInfoProjection getPriceInfo(int productId) {
        String sql = """
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
            ? BigDecimal.valueOf(((sellingPrice - purchasePrice) * 100.0) / sellingPrice)
                .setScale(2, RoundingMode.HALF_UP).doubleValue()
            : 0;

        return new PriceInfoProjection(purchasePrice, sellingPrice, marginPercent, vatRate);
    }

    @Override
    public List<LotInfoProjection> getLotInfo(int productId) {
        String sql = """
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

        List<LotInfoProjection> lots = new ArrayList<>();
        for (Object[] row : results) {
            lots.add(new LotInfoProjection(
                ((Number) row[0]).longValue(),
                (String) row[1],
                ((java.sql.Date) row[2]).toLocalDate(),
                ((Number) row[3]).intValue(),
                ((Number) row[4]).intValue()
            ));
        }

        return lots;
    }

    @Override
    public ProductSalesStatsProjection getSalesStats(int productId, LocalDate today, LocalDate weekStart, LocalDate monthStart) {
        String sql = """
            SELECT
                COALESCE(SUM(CASE WHEN s.sale_date = :today THEN sl.quantity_sold ELSE 0 END), 0) as today_qty,
                COALESCE(SUM(CASE WHEN s.sale_date = :today THEN sl.sales_amount ELSE 0 END), 0) as today_amount,
                COALESCE(SUM(CASE WHEN s.sale_date >= :weekStart THEN sl.quantity_sold ELSE 0 END), 0) as week_qty,
                COALESCE(SUM(CASE WHEN s.sale_date >= :weekStart THEN sl.sales_amount ELSE 0 END), 0) as week_amount,
                COALESCE(SUM(sl.quantity_sold), 0) as month_qty,
                COALESCE(SUM(sl.sales_amount), 0) as month_amount
            FROM sales_line sl
            INNER JOIN sales s ON sl.sales_id = s.id AND sl.sale_date = s.sale_date
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

        double avgDailyQty = BigDecimal.valueOf(monthQty / 30.0)
            .setScale(1, RoundingMode.HALF_UP).doubleValue();

        return new ProductSalesStatsProjection(
            todayQty,
            todayAmount,
            weekQty,
            weekAmount,
            monthQty,
            monthAmount,
            avgDailyQty
        );
    }

    @Override
    public double getAverageDailyQuantitySold(int productId, int lookbackDays) {
        String sql = """
            SELECT COALESCE(AVG(daily_qty), 0)
            FROM (
                SELECT SUM(sl.quantity_sold) as daily_qty
                FROM sales_line sl
                INNER JOIN sales s ON sl.sales_id = s.id AND sl.sale_date = s.sale_date
                WHERE sl.produit_id = :productId
                  AND s.sale_date >= CURRENT_DATE - :lookbackDays
                  AND s.statut = 'CLOSED'
                  AND s.canceled = false
                  AND s.ca = 'CA'
                GROUP BY s.sale_date
            ) daily_sales
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("productId", productId);
        query.setParameter("lookbackDays", lookbackDays);

        return ((Number) query.getSingleResult()).doubleValue();
    }
}
