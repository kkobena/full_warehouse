package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.MarketBasketSummaryDTO;
import com.kobe.warehouse.service.dto.report.ProductAssociationDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of Market Basket Analysis Service
 */
@Service
@Transactional(readOnly = true)
public class MarketBasketAnalysisServiceImpl implements MarketBasketAnalysisService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Cacheable(value = "marketBasketCache", key = "#startDate + '-' + #endDate + '-' + #minSupport + '-' + #minConfidence + '-' + #limit")
    public List<ProductAssociationDTO> getProductAssociations(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal minSupport,
        BigDecimal minConfidence,
        Integer limit
    ) {
        String sql =
            """
            WITH sales_products AS (
                SELECT DISTINCT
                    s.id as sale_id,
                    sl.produit_id as product_id
                FROM sales s
                INNER JOIN sales_line sl ON s.id = sl.sales_id
                WHERE s.statut = 'CLOSED'
                  AND s.canceled = false
                  AND s.sale_date BETWEEN :startDate AND :endDate
            ),
            product_pairs AS (
                SELECT
                    sp1.product_id as product_a_id,
                    sp2.product_id as product_b_id,
                    COUNT(DISTINCT sp1.sale_id) as transactions_with_both
                FROM sales_products sp1
                INNER JOIN sales_products sp2
                    ON sp1.sale_id = sp2.sale_id
                    AND sp1.product_id < sp2.product_id
                GROUP BY sp1.product_id, sp2.product_id
            ),
            product_counts AS (
                SELECT
                    product_id,
                    COUNT(DISTINCT sale_id) as transactions_with_product
                FROM sales_products
                GROUP BY product_id
            ),
            total_count AS (
                SELECT COUNT(DISTINCT sale_id) as total_transactions
                FROM sales_products
            )
            SELECT
                pp.product_a_id,
                pa.libelle as product_a_name,
                COALESCE(fpa.code_cip, '') as product_a_code_cip,
                pp.product_b_id,
                pb.libelle as product_b_name,
                COALESCE(fpb.code_cip, '') as product_b_code_cip,
                pp.transactions_with_both,
                pca.transactions_with_product as transactions_with_a,
                pcb.transactions_with_product as transactions_with_b,
                tc.total_transactions
            FROM product_pairs pp
            INNER JOIN produit pa ON pp.product_a_id = pa.id
            LEFT JOIN fournisseur_produit fpa ON pa.fournisseur_produit_principal_id = fpa.id
            INNER JOIN produit pb ON pp.product_b_id = pb.id
            LEFT JOIN fournisseur_produit fpb ON pb.fournisseur_produit_principal_id = fpb.id
            INNER JOIN product_counts pca ON pp.product_a_id = pca.product_id
            INNER JOIN product_counts pcb ON pp.product_b_id = pcb.product_id
            CROSS JOIN total_count tc
            WHERE
                (pp.transactions_with_both::decimal / tc.total_transactions * 100) >= :minSupport
                AND (pp.transactions_with_both::decimal / pca.transactions_with_product * 100) >= :minConfidence
            ORDER BY pp.transactions_with_both DESC, pa.libelle, pb.libelle

            """;

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager
            .createNativeQuery(sql)
            .setMaxResults(limit)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .setParameter("minSupport", minSupport)
            .setParameter("minConfidence", minConfidence)
            .getResultList();

        List<ProductAssociationDTO> associations = new ArrayList<>();
        for (Object[] row : results) {
            associations.add(
                ProductAssociationDTO.create(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    ((Number) row[3]).longValue(),
                    (String) row[4],
                    (String) row[5],
                    ((Number) row[6]).longValue(),
                    ((Number) row[7]).longValue(),
                    ((Number) row[8]).longValue(),
                    ((Number) row[9]).longValue()
                )
            );
        }

        return associations;
    }

    @Override
    @Cacheable(value = "marketBasketCache", key = "'product-' + #productId + '-' + #startDate + '-' + #endDate")
    public List<ProductAssociationDTO> getAssociationsForProduct(Long productId, LocalDate startDate, LocalDate endDate, Integer limit) {
        String sql =
            """
            WITH target_product_sales AS (
                SELECT DISTINCT s.id as sale_id
                FROM sales s
                INNER JOIN sales_line sl ON s.id = sl.sales_id
                WHERE sl.produit_id = :productId
                  AND s.statut = 'CLOSED'
                  AND s.canceled = false
                  AND s.sale_date BETWEEN :startDate AND :endDate
            ),
            associated_products AS (
                SELECT
                    sl.produit_id,
                    COUNT(DISTINCT s.id) as transactions_with_both
                FROM sales s
                INNER JOIN target_product_sales tps ON s.id = tps.sale_id
                INNER JOIN sales_line sl ON s.id = sl.sales_id
                WHERE sl.produit_id != :productId
                GROUP BY sl.produit_id
            ),
            product_counts AS (
                SELECT
                    sl.produit_id,
                    COUNT(DISTINCT s.id) as transactions_with_product
                FROM sales s
                INNER JOIN sales_line sl ON s.id = sl.sales_id
                WHERE s.statut = 'CLOSED'
                  AND s.canceled = false
                  AND s.sale_date BETWEEN :startDate AND :endDate
                GROUP BY sl.produit_id
            ),
            target_count AS (
                SELECT COUNT(*) as transactions_with_target
                FROM target_product_sales
            ),
            total_count AS (
                SELECT COUNT(DISTINCT s.id) as total_transactions
                FROM sales s
                WHERE s.statut = 'CLOSED'
                  AND s.canceled = false
                  AND s.sale_date BETWEEN :startDate AND :endDate
            )
            SELECT
                :productId,
                pt.libelle as target_name,
                COALESCE(fpt.code_cip, '') as target_code_cip,
                ap.produit_id,
                p.libelle as product_name,
                COALESCE(fp.code_cip, '') as product_code_cip,
                ap.transactions_with_both,
                tc.transactions_with_target,
                pc.transactions_with_product,
                tot.total_transactions
            FROM associated_products ap
            INNER JOIN produit p ON ap.produit_id = p.id
            LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
            INNER JOIN product_counts pc ON ap.produit_id = pc.produit_id
            CROSS JOIN target_count tc
            CROSS JOIN total_count tot
            CROSS JOIN produit pt
            LEFT JOIN fournisseur_produit fpt ON pt.fournisseur_produit_principal_id = fpt.id
            WHERE pt.id = :productId
            ORDER BY ap.transactions_with_both DESC

            """;

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager
            .createNativeQuery(sql)
            .setMaxResults(limit)
            .setParameter("productId", productId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();

        List<ProductAssociationDTO> associations = new ArrayList<>();
        for (Object[] row : results) {
            associations.add(
                ProductAssociationDTO.create(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    ((Number) row[3]).longValue(),
                    (String) row[4],
                    (String) row[5],
                    ((Number) row[6]).longValue(),
                    ((Number) row[7]).longValue(),
                    ((Number) row[8]).longValue(),
                    ((Number) row[9]).longValue()
                )
            );
        }

        return associations;
    }

    @Override
    @Cacheable(value = "marketBasketCache", key = "'summary-' + #startDate + '-' + #endDate")
    public MarketBasketSummaryDTO getMarketBasketSummary(LocalDate startDate, LocalDate endDate) {
        // Get total transactions
        String transactionsSql =
            """
            SELECT COUNT(DISTINCT s.id)
            FROM sales s
            WHERE s.statut = 'CLOSED'
              AND s.canceled = false
              AND s.sale_date BETWEEN :startDate AND :endDate
            """;

        Long totalTransactions = ((Number) entityManager
                .createNativeQuery(transactionsSql)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult())
            .longValue();

        // Get total unique products sold
        String productsSql =
            """
            SELECT COUNT(DISTINCT sl.produit_id)
            FROM sales s
            INNER JOIN sales_line sl ON s.id = sl.sales_id
            WHERE s.statut = 'CLOSED'
              AND s.canceled = false
              AND s.sale_date BETWEEN :startDate AND :endDate
            """;

        Long totalProducts = ((Number) entityManager
                .createNativeQuery(productsSql)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult())
            .longValue();

        // Get average basket size
        String basketSizeSql =
            """
            SELECT AVG(items_per_sale)
            FROM (
                SELECT COUNT(DISTINCT sl.produit_id) as items_per_sale
                FROM sales s
                INNER JOIN sales_line sl ON s.id = sl.sales_id
                WHERE s.statut = 'CLOSED'
                  AND s.canceled = false
                  AND s.sale_date BETWEEN :startDate AND :endDate
                GROUP BY s.id
            ) basket_sizes
            """;

        BigDecimal averageBasketSize = ((Number) entityManager
                .createNativeQuery(basketSizeSql)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult())
            .doubleValue() == 0.0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(
                ((Number) entityManager
                        .createNativeQuery(basketSizeSql)
                        .setParameter("startDate", startDate)
                        .setParameter("endDate", endDate)
                        .getSingleResult())
                    .doubleValue()
            )
                .setScale(2, RoundingMode.HALF_UP);

        // Get top associations with default thresholds
        List<ProductAssociationDTO> topAssociations = getProductAssociations(
            startDate,
            endDate,
            BigDecimal.valueOf(0.5),
            BigDecimal.valueOf(5.0),
            100
        );

        Long totalAssociations = (long) topAssociations.size();

        BigDecimal maxConfidence = topAssociations.isEmpty()
            ? BigDecimal.ZERO
            : topAssociations.stream().map(ProductAssociationDTO::confidence).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        BigDecimal maxLift = topAssociations.isEmpty()
            ? BigDecimal.ZERO
            : topAssociations.stream().map(ProductAssociationDTO::lift).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        String mostFrequentPair = topAssociations.isEmpty()
            ? "Aucune"
            : topAssociations.getFirst().productAName() + " + " + topAssociations.getFirst().productBName();

        return new MarketBasketSummaryDTO(
            totalTransactions,
            totalProducts,
            totalAssociations,
            averageBasketSize,
            maxConfidence,
            maxLift,
            mostFrequentPair
        );
    }

    @Override
    @Cacheable(value = "marketBasketCache", key = "'recommendations-' + #productId")
    public List<ProductAssociationDTO> getCrossSellRecommendations(Long productId) {
        // Get recommendations for last 6 months
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(6);

        return getAssociationsForProduct(productId, startDate, endDate, 10);
    }
}
