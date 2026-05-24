package com.kobe.warehouse.repository.impl;

import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.MobilePerformanceRepository;
import com.kobe.warehouse.service.dto.mobile.PerformancePeriod;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Implementation of MobilePerformanceRepository using native SQL queries.
 */
@Repository
public class MobilePerformanceRepositoryImpl implements MobilePerformanceRepository {

    private static final String SALES_CA_TYPE = "CA";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public PeriodSummaryProjection getPeriodSummary(LocalDate startDate, LocalDate endDate) {
        // Note: cost_amount is stored in sales_line, not in sales table
        // We need to join with sales_line to calculate margin
        String sql = """
            SELECT
                COALESCE(SUM(s.sales_amount), 0) as ca_total,
                COUNT(DISTINCT s.id) as transactions_count,
                COUNT(DISTINCT s.customer_id) as customers_count,
                COALESCE(SUM(s.sales_amount) - SUM(sl.cost_amount), 0) as margin_total
            FROM sales s
            LEFT JOIN sales_line sl ON sl.sales_id = s.id AND sl.sale_date = s.sale_date
            WHERE s.sale_date BETWEEN :startDate AND :endDate
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        Object[] row = (Object[]) query.getSingleResult();

        return new PeriodSummaryProjection(
            ((Number) row[0]).longValue(),
            ((Number) row[1]).intValue(),
            ((Number) row[2]).intValue(),
            ((Number) row[3]).longValue()
        );
    }

    @Override
    public List<PaymentMethodProjection> getPaymentMethodsSummary(LocalDate startDate, LocalDate endDate) {
        // payment_transaction uses SINGLE_TABLE inheritance
        // sale_id and sale_date are in the table for SalePayment records
        String sql = """
            SELECT
                pm.code,
                pm.libelle,
                COALESCE(SUM(pt.paid_amount), 0) as amount,
                COUNT(DISTINCT pt.id) as transactions_count
            FROM payment_transaction pt
            INNER JOIN payment_mode pm ON pt.payment_mode_code = pm.code
            INNER JOIN sales s ON pt.sale_id = s.id AND pt.sale_date = s.sale_date
            WHERE s.sale_date BETWEEN :startDate AND :endDate
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
              AND pt.dtype = 'SalePayment'
            GROUP BY pm.code, pm.libelle
            ORDER BY amount DESC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<PaymentMethodProjection> paymentMethods = new ArrayList<>();
        for (Object[] row : results) {
            paymentMethods.add(new PaymentMethodProjection(
                (String) row[0],
                (String) row[1],
                ((Number) row[2]).longValue(),
                ((Number) row[3]).intValue()
            ));
        }

        return paymentMethods;
    }

    @Override
    public List<TopProductProjection> getTopProducts(
        LocalDate startDate,
        LocalDate endDate,
        LocalDate previousStartDate,
        LocalDate previousEndDate,
        int limit
    ) {
        String sql = """
            WITH current_period AS (
                SELECT
                    p.id,
                    p.libelle,
                    fp.code_cip,
                    SUM(sl.sales_amount) as sales_amount,
                    SUM(sl.quantity_sold) as quantity_sold
                FROM sales_line sl
                INNER JOIN sales s ON sl.sales_id = s.id AND sl.sale_date = s.sale_date
                INNER JOIN produit p ON sl.produit_id = p.id
                LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id
                WHERE s.sale_date BETWEEN :startDate AND :endDate
                  AND s.statut = :statut
                  AND s.canceled = false
                  AND s.ca = :caType
                GROUP BY p.id, p.libelle, fp.code_cip
            ),
            previous_period AS (
                SELECT
                    p.id,
                    SUM(sl.sales_amount) as sales_amount
                FROM sales_line sl
                INNER JOIN sales s ON sl.sales_id = s.id AND sl.sale_date = s.sale_date
                INNER JOIN produit p ON sl.produit_id = p.id
                WHERE s.sale_date BETWEEN :previousStartDate AND :previousEndDate
                  AND s.statut = :statut
                  AND s.canceled = false
                  AND s.ca = :caType
                GROUP BY p.id
            ),
            total_ca AS (
                SELECT COALESCE(SUM(sales_amount), 1) as total FROM current_period
            )
            SELECT
                cp.id,
                cp.libelle,
                cp.code_cip,
                cp.sales_amount,
                cp.quantity_sold,
                (cp.sales_amount * 100.0 / t.total) as percent_of_total,
                CASE
                    WHEN pp.sales_amount > 0
                    THEN ((cp.sales_amount - pp.sales_amount) * 100.0 / pp.sales_amount)
                    ELSE 0
                END as variation_percent
            FROM current_period cp
            CROSS JOIN total_ca t
            LEFT JOIN previous_period pp ON cp.id = pp.id
            ORDER BY cp.sales_amount DESC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("previousStartDate", previousStartDate);
        query.setParameter("previousEndDate", previousEndDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);
        query.setMaxResults(limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<TopProductProjection> topProducts = new ArrayList<>();
        for (Object[] row : results) {
            topProducts.add(new TopProductProjection(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).longValue(),
                ((Number) row[4]).intValue(),
                ((Number) row[5]).doubleValue(),
                ((Number) row[6]).doubleValue()
            ));
        }

        return topProducts;
    }

    @Override
    public List<DataPointProjection> getDataPoints(LocalDate startDate, LocalDate endDate, PerformancePeriod period) {
        String groupBy = period.getSqlGroupBy();

        // Margin is calculated from sales_line.cost_amount
        String sql = """
            SELECT
                %s as period_date,
                COALESCE(SUM(s.sales_amount), 0) as ca_amount,
                COUNT(DISTINCT s.id) as transactions_count,
                COALESCE(SUM(s.sales_amount) - SUM(sl.cost_amount), 0) as margin_amount
            FROM sales s
            LEFT JOIN sales_line sl ON sl.sales_id = s.id AND sl.sale_date = s.sale_date
            WHERE s.sale_date BETWEEN :startDate AND :endDate
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            GROUP BY %s
            ORDER BY period_date
            """.formatted(groupBy, groupBy);

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<DataPointProjection> dataPoints = new ArrayList<>();
        for (Object[] row : results) {
            LocalDate date = LocalDate.parse(row[0].toString());
            dataPoints.add(new DataPointProjection(
                date,
                ((Number) row[1]).longValue(),
                ((Number) row[2]).intValue(),
                ((Number) row[3]).longValue()
            ));
        }

        return dataPoints;
    }
}
