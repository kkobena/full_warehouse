package com.kobe.warehouse.repository;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class SupplierEvolutionRepository {

    private static final String EVOLUTION_SQL =
        "WITH months AS (" +
        "  SELECT DATE_TRUNC('month', CURRENT_DATE - make_interval(months => gs::int)) AS month_start" +
        "  FROM generate_series(0, 11) gs" +
        ")," +
        "n_monthly AS (" +
        "  SELECT" +
        "    DATE_TRUNC('month', c.receipt_date) AS month_start," +
        "    COALESCE(SUM(c.final_amount), 0)::bigint AS montant," +
        "    COALESCE(ROUND(AVG(c.receipt_date - c.order_date)), 0)::int AS delai_jours," +
        "    COUNT(DISTINCT c.id)::int AS nb_commandes" +
        "  FROM commande c" +
        "  WHERE c.order_status = 'RECEIVED'" +
        "    AND c.receipt_date >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '11 months'" +
        "    AND c.receipt_date <= CURRENT_DATE" +
        "  GROUP BY DATE_TRUNC('month', c.receipt_date)" +
        ")," +
        "n1_monthly AS (" +
        "  SELECT" +
        "    DATE_TRUNC('month', c.receipt_date) + INTERVAL '1 year' AS month_start," +
        "    COALESCE(SUM(c.final_amount), 0)::bigint AS montant," +
        "    COALESCE(ROUND(AVG(c.receipt_date - c.order_date)), 0)::int AS delai_jours," +
        "    COUNT(DISTINCT c.id)::int AS nb_commandes" +
        "  FROM commande c" +
        "  WHERE c.order_status = 'RECEIVED'" +
        "    AND c.receipt_date >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '23 months'" +
        "    AND c.receipt_date < DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '11 months'" +
        "  GROUP BY DATE_TRUNC('month', c.receipt_date)" +
        ")" +
        " SELECT" +
        "  CAST(EXTRACT(YEAR FROM m.month_start) AS int) AS yr," +
        "  CAST(EXTRACT(MONTH FROM m.month_start) AS int) AS mo," +
        "  COALESCE(n.montant, 0) AS montant_n," +
        "  COALESCE(n1.montant, 0) AS montant_n1," +
        "  COALESCE(n.delai_jours, 0) AS delai_n," +
        "  COALESCE(n1.delai_jours, 0) AS delai_n1," +
        "  COALESCE(n.nb_commandes, 0) AS nb_commandes_n," +
        "  COALESCE(n1.nb_commandes, 0) AS nb_commandes_n1" +
        " FROM months m" +
        " LEFT JOIN n_monthly n ON n.month_start = m.month_start" +
        " LEFT JOIN n1_monthly n1 ON n1.month_start = m.month_start" +
        " ORDER BY yr, mo";

    private final EntityManager entityManager;

    public SupplierEvolutionRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findEvolution() {
        return entityManager.createNativeQuery(EVOLUTION_SQL).getResultList();
    }
}
