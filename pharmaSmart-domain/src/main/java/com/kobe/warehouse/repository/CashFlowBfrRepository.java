package com.kobe.warehouse.repository;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class CashFlowBfrRepository {

    private static final String STOCK_VALUE_SQL =
        "SELECT COALESCE(SUM(total_purchase_value), 0) FROM mv_stock_valuation WHERE magasin_id = :magasinId";

    private static final String CREANCE_TP_SQL = """
        SELECT COALESCE(SUM(tpsl.montant - COALESCE(tpsl.montant_regle, 0)), 0)
        FROM third_party_sale_line tpsl
        INNER JOIN sales s ON s.id = tpsl.sale_id AND s.sale_date = tpsl.sale_sale_date
        WHERE tpsl.statut NOT IN ('PAID', 'DELETE')
          AND s.statut = 'CLOSED' AND s.canceled = false AND s.ca = 'CA'
          AND tpsl.sale_date >= CURRENT_DATE - INTERVAL '1 year'
        """;

    private static final String DETTE_FOURNISSEUR_SQL = """
        SELECT COALESCE(SUM(c.final_amount), 0) - COALESCE((
          SELECT SUM(pt.paid_amount) FROM payment_transaction pt
          WHERE pt.dtype = 'PaymentFournisseur'
            AND pt.commande_id IN (
              SELECT c2.id FROM commande c2
              WHERE c2.paiment_status != 'PAID'
                AND c2.order_status = 'CLOSED'
                AND c2.receipt_date >= CURRENT_DATE - INTERVAL '1 year'
            )
        ), 0)
        FROM commande c
        WHERE c.paiment_status != 'PAID'
          AND c.order_status = 'CLOSED'
          AND c.receipt_date >= CURRENT_DATE - INTERVAL '1 year'
        """;

    // Cost of goods sold for last 12 months — denominator for DIO
    private static final String COGS_12M_SQL = """
        SELECT COALESCE(SUM(sl.cost_amount * sl.quantity_requested), 0)
        FROM sales_line sl
        INNER JOIN sales s ON s.id = sl.sales_id
        WHERE s.statut = 'CLOSED' AND s.canceled = false AND s.ca = 'CA'
          AND s.sale_date >= CURRENT_DATE - INTERVAL '1 year'
        """;

    // TP invoiced last 12 months — denominator for DSO
    private static final String CA_TP_12M_SQL = """
        SELECT COALESCE(SUM(CAST(f.montant_net AS bigint)), 0)
        FROM facture_tiers_payant f
        WHERE f.invoice_date >= CURRENT_DATE - INTERVAL '1 year'
          AND f.groupe_facture_tiers_payant_id IS NULL
        """;

    // Supplier purchases CLOSED last 12 months — denominator for DPO
    private static final String ACHATS_12M_SQL = """
        SELECT COALESCE(SUM(c.final_amount), 0)
        FROM commande c
        WHERE c.order_status = 'CLOSED'
          AND c.receipt_date >= CURRENT_DATE - INTERVAL '1 year'
        """;

    // Monthly TP invoices + supplier purchases for the rolling 12-month chart
    private static final String EVOLUTION_SQL =
        "WITH months AS (" +
        "  SELECT DATE_TRUNC('month', CURRENT_DATE - make_interval(months => gs::int)) AS month_start" +
        "  FROM generate_series(0, 11) gs" +
        ")," +
        "creances_by_month AS (" +
        "  SELECT DATE_TRUNC('month', f.invoice_date) AS month_start," +
        "    SUM(CAST(f.montant_net AS bigint)) AS creances_emises" +
        "  FROM facture_tiers_payant f" +
        "  WHERE f.groupe_facture_tiers_payant_id IS NULL" +
        "    AND f.invoice_date >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '11 months'" +
        "    AND f.invoice_date <= CURRENT_DATE" +
        "  GROUP BY DATE_TRUNC('month', f.invoice_date)" +
        ")," +
        "achats_by_month AS (" +
        "  SELECT DATE_TRUNC('month', c.receipt_date) AS month_start," +
        "    SUM(c.final_amount) AS achats_recus" +
        "  FROM commande c" +
        "  WHERE c.order_status = 'CLOSED'" +
        "    AND c.receipt_date >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '11 months'" +
        "    AND c.receipt_date <= CURRENT_DATE" +
        "  GROUP BY DATE_TRUNC('month', c.receipt_date)" +
        ")" +
        " SELECT" +
        "   CAST(EXTRACT(YEAR FROM m.month_start) AS int) AS yr," +
        "   CAST(EXTRACT(MONTH FROM m.month_start) AS int) AS mo," +
        "   COALESCE(cm.creances_emises, 0) AS creances_emises," +
        "   COALESCE(am.achats_recus, 0) AS achats_recus" +
        " FROM months m" +
        " LEFT JOIN creances_by_month cm ON cm.month_start = m.month_start" +
        " LEFT JOIN achats_by_month am ON am.month_start = m.month_start" +
        " ORDER BY yr, mo";

    private final EntityManager entityManager;

    public CashFlowBfrRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long getStockValue(Integer magasinId) {
        Object result = entityManager.createNativeQuery(STOCK_VALUE_SQL)
            .setParameter("magasinId", magasinId)
            .getSingleResult();
        return result != null ? ((Number) result).longValue() : 0L;
    }

    public long getCreanceTp() {
        Object result = entityManager.createNativeQuery(CREANCE_TP_SQL).getSingleResult();
        return result != null ? ((Number) result).longValue() : 0L;
    }

    public long getDetteFournisseur() {
        Object result = entityManager.createNativeQuery(DETTE_FOURNISSEUR_SQL).getSingleResult();
        return result != null ? ((Number) result).longValue() : 0L;
    }

    public long getCogs12m() {
        Object result = entityManager.createNativeQuery(COGS_12M_SQL).getSingleResult();
        return result != null ? ((Number) result).longValue() : 0L;
    }

    public long getCaTp12m() {
        Object result = entityManager.createNativeQuery(CA_TP_12M_SQL).getSingleResult();
        return result != null ? ((Number) result).longValue() : 0L;
    }

    public long getAchats12m() {
        Object result = entityManager.createNativeQuery(ACHATS_12M_SQL).getSingleResult();
        return result != null ? ((Number) result).longValue() : 0L;
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findEvolution() {
        return entityManager.createNativeQuery(EVOLUTION_SQL).getResultList();
    }
}
