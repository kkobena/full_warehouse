package com.kobe.warehouse.repository.impl;

import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.MobileSalesRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * Implementation of MobileSalesRepository using native SQL queries.
 * Note: Margin is calculated from sales_line.cost_amount (not sales.cost_amount which is @Transient).
 */
@Repository
public class MobileSalesRepositoryImpl implements MobileSalesRepository {

    private static final Logger LOG = LoggerFactory.getLogger(MobileSalesRepositoryImpl.class);
    private static final String SALES_CA_TYPE = "CA";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public DailySalesSummaryProjection getDailySalesSummary(LocalDate date) {
        // Note: cost_amount is in sales_line, not in sales table
        String sql = """
            SELECT
                COALESCE(SUM(s.sales_amount), 0) as ca_total,
                COUNT(DISTINCT s.id) as transactions_count,
                COUNT(DISTINCT s.customer_id) as customers_count,
                COALESCE(SUM(
                    CASE WHEN s.rest_to_pay = 0 AND s.part_tiers_payant = 0
                    THEN s.sales_amount ELSE 0 END
                ), 0) as amount_collected,
                COALESCE(SUM(s.rest_to_pay + s.part_tiers_payant), 0) as amount_credit,
                COALESCE(SUM(s.sales_amount) - SUM(sl.cost_amount), 0) as margin_amount
            FROM sales s
            LEFT JOIN sales_line sl ON sl.sales_id = s.id AND sl.sale_date = s.sale_date
            WHERE s.sale_date = :date
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("date", date);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        Object[] row = (Object[]) query.getSingleResult();

        long caTotal = ((Number) row[0]).longValue();
        int transactionsCount = ((Number) row[1]).intValue();
        int customersCount = ((Number) row[2]).intValue();
        long amountCollected = ((Number) row[3]).longValue();
        long amountCredit = ((Number) row[4]).longValue();
        long marginAmount = ((Number) row[5]).longValue();

        long averageBasket = transactionsCount > 0 ? caTotal / transactionsCount : 0;
        double marginPercent = caTotal > 0
            ? BigDecimal.valueOf((marginAmount * 100.0) / caTotal).setScale(2, RoundingMode.HALF_UP).doubleValue()
            : 0;

        return new DailySalesSummaryProjection(
            caTotal,
            transactionsCount,
            customersCount,
            averageBasket,
            amountCollected,
            amountCredit,
            marginAmount,
            marginPercent
        );
    }

    @Override
    public SalesSummaryProjection getSalesSummary(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                COALESCE(SUM(s.sales_amount), 0) as ca_total,
                COUNT(DISTINCT s.id) as transactions_count,
                COUNT(DISTINCT s.customer_id) as customers_count,
                COALESCE(SUM(s.sales_amount) - SUM(sl.cost_amount), 0) as margin_amount
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

        return new SalesSummaryProjection(
            ((Number) row[0]).longValue(),
            ((Number) row[1]).intValue(),
            ((Number) row[2]).intValue(),
            ((Number) row[3]).longValue()
        );
    }

    @Override
    public UserSalesSummaryProjection getUserSalesSummary(int userId, LocalDate date) {
        String sql = """
            SELECT
                COALESCE(SUM(s.sales_amount), 0) as total_ca,
                COUNT(DISTINCT s.id) as sales_count,
                COALESCE(SUM(s.sales_amount) - SUM(sl.cost_amount), 0) as margin_amount
            FROM sales s
            LEFT JOIN sales_line sl ON sl.sales_id = s.id AND sl.sale_date = s.sale_date
            WHERE s.sale_date = :date
              AND s.seller_id = :userId
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("date", date);
        query.setParameter("userId", userId);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        Object[] row = (Object[]) query.getSingleResult();

        long totalCA = ((Number) row[0]).longValue();
        int salesCount = ((Number) row[1]).intValue();
        long marginAmount = ((Number) row[2]).longValue();

        long averageBasket = salesCount > 0 ? totalCA / salesCount : 0;
        double marginPercent = totalCA > 0
            ? BigDecimal.valueOf((marginAmount * 100.0) / totalCA).setScale(2, RoundingMode.HALF_UP).doubleValue()
            : 0;

        return new UserSalesSummaryProjection(
            totalCA,
            salesCount,
            averageBasket,
            marginAmount,
            marginPercent
        );
    }

    @Override
    public List<TopProductProjection> getTopProducts(LocalDate date, int limit) {
        return getTopProducts(date, date, limit);
    }

    @Override
    public List<TopProductProjection> getTopProducts(LocalDate startDate, LocalDate endDate, int limit) {
        String sql = """
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
            ORDER BY sales_amount DESC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);
        query.setMaxResults(limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<TopProductProjection> topProducts = new ArrayList<>();
        int rank = 1;
        for (Object[] row : results) {
            topProducts.add(new TopProductProjection(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).longValue(),
                ((Number) row[4]).intValue(),
                rank++
            ));
        }

        return topProducts;
    }

    @Override
    public List<DailyCATrendProjection> getCATrend(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                s.sale_date,
                COALESCE(SUM(s.sales_amount), 0) as ca_total,
                COUNT(DISTINCT s.id) as transactions_count
            FROM sales s
            WHERE s.sale_date BETWEEN :startDate AND :endDate
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            GROUP BY s.sale_date
            ORDER BY s.sale_date
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<DailyCATrendProjection> trend = new ArrayList<>();
        for (Object[] row : results) {
            trend.add(new DailyCATrendProjection(
                LocalDate.parse(row[0].toString()),
                ((Number) row[1]).longValue(),
                ((Number) row[2]).intValue()
            ));
        }

        return trend;
    }

    @Override
    public long getAverageCA(LocalDate date, int lookbackDays) {
        String sql = """
            SELECT COALESCE(AVG(daily_ca), 0)
            FROM (
                SELECT SUM(s.sales_amount) AS daily_ca
                FROM sales s
                WHERE s.sale_date BETWEEN :startDate AND :endDate
                  AND s.statut = :statut
                  AND s.canceled = false
                  AND s.ca = :caType
                GROUP BY s.sale_date
            ) daily_sales
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", date.minusDays(lookbackDays));
        query.setParameter("endDate", date.minusDays(1));
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public List<PaymentMethodProjection> getPaymentMethodsSummary(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                pm.code,
                pm.libelle,
                COALESCE(SUM(pt.paid_amount), 0) as amount
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
                ((Number) row[2]).longValue()
            ));
        }

        return paymentMethods;
    }

    @Override
    public int getCustomersCount(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT COUNT(DISTINCT s.customer_id)
            FROM sales s
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

        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public String getUserName(int userId) {
        String sql = "SELECT CONCAT(u.first_name, ' ', u.last_name) FROM app_user u WHERE u.id = :userId";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);

        try {
            return (String) query.getSingleResult();
        } catch (Exception e) {
            LOG.warn("Could not find user with ID: {}", userId);
            return "Unknown User";
        }
    }
}
