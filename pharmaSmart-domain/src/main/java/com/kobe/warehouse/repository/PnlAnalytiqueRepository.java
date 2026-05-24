package com.kobe.warehouse.repository;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class PnlAnalytiqueRepository {

    private static final String SEGMENT_SQL =
        "SELECT s.nature_vente AS segment," +
        "  SUM(sl.sales_amount) AS ca," +
        "  SUM(sl.cost_amount * sl.quantity_requested) AS cout_achat," +
        "  SUM(sl.sales_amount - sl.cost_amount * sl.quantity_requested) AS marge_brute," +
        "  CASE WHEN SUM(sl.sales_amount) > 0" +
        "       THEN ROUND(SUM(sl.sales_amount - sl.cost_amount * sl.quantity_requested)::numeric" +
        "                  / SUM(sl.sales_amount)::numeric * 100, 2)" +
        "       ELSE 0 END AS taux_marge," +
        "  COUNT(DISTINCT s.id) AS nb_transactions" +
        " FROM sales s" +
        " INNER JOIN sales_line sl ON s.id = sl.sales_id" +
        " WHERE s.statut = 'CLOSED' AND s.canceled = false AND s.ca = 'CA'" +
        "   AND EXTRACT(YEAR FROM s.sale_date) = :year" +
        " GROUP BY s.nature_vente" +
        " ORDER BY ca DESC";

    private static final String FAMILLE_SQL =
        "SELECT fp.libelle AS famille," +
        "  SUM(sl.sales_amount) AS ca," +
        "  SUM(sl.cost_amount * sl.quantity_requested) AS cout_achat," +
        "  SUM(sl.sales_amount - sl.cost_amount * sl.quantity_requested) AS marge_brute," +
        "  CASE WHEN SUM(sl.sales_amount) > 0" +
        "       THEN ROUND(SUM(sl.sales_amount - sl.cost_amount * sl.quantity_requested)::numeric" +
        "                  / SUM(sl.sales_amount)::numeric * 100, 2)" +
        "       ELSE 0 END AS taux_marge" +
        " FROM sales s" +
        " INNER JOIN sales_line sl ON s.id = sl.sales_id" +
        " INNER JOIN produit p ON sl.produit_id = p.id" +
        " INNER JOIN famille_produit fp ON p.famille_id = fp.id" +
        " WHERE s.statut = 'CLOSED' AND s.canceled = false AND s.ca = 'CA'" +
        "   AND EXTRACT(YEAR FROM s.sale_date) = :year" +
        " GROUP BY fp.id, fp.libelle" +
        " ORDER BY ca DESC";

    private static final String EVOLUTION_SQL =
        "WITH monthly_data AS (" +
        "  SELECT" +
        "    CAST(EXTRACT(YEAR FROM s.sale_date) AS int) AS yr," +
        "    CAST(EXTRACT(MONTH FROM s.sale_date) AS int) AS mo," +
        "    fp.libelle AS famille," +
        "    SUM(sl.sales_amount) AS ca," +
        "    SUM(sl.sales_amount - sl.cost_amount * sl.quantity_requested) AS marge_brute" +
        "  FROM sales s" +
        "  INNER JOIN sales_line sl ON s.id = sl.sales_id" +
        "  INNER JOIN produit p ON sl.produit_id = p.id" +
        "  INNER JOIN famille_produit fp ON p.famille_id = fp.id" +
        "  WHERE s.statut = 'CLOSED' AND s.canceled = false AND s.ca = 'CA'" +
        "    AND s.sale_date >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '11 months'" +
        "    AND s.sale_date <= CURRENT_DATE" +
        "  GROUP BY yr, mo, fp.id, fp.libelle" +
        ") " +
        "SELECT m.yr, m.mo, m.famille," +
        "  CASE WHEN m.ca > 0" +
        "       THEN ROUND(m.marge_brute::numeric / m.ca::numeric * 100, 2)" +
        "       ELSE 0 END AS taux_marge," +
        "  SUM(m.ca) OVER (PARTITION BY m.famille) AS famille_total_ca" +
        " FROM monthly_data m" +
        " WHERE m.famille IN (" +
        "   SELECT famille FROM monthly_data GROUP BY famille ORDER BY SUM(ca) DESC LIMIT 5" +
        " )" +
        " ORDER BY m.yr, m.mo, famille_total_ca DESC";

    private static final String EVOLUTION_SEGMENT_SQL =
        "SELECT" +
        "    CAST(EXTRACT(YEAR  FROM s.sale_date) AS int) AS yr," +
        "    CAST(EXTRACT(MONTH FROM s.sale_date) AS int) AS mo," +
        "    s.nature_vente AS segment," +
        "    CASE WHEN SUM(sl.sales_amount) > 0" +
        "         THEN ROUND(SUM(sl.sales_amount - sl.cost_amount * sl.quantity_requested)::numeric" +
        "                    / SUM(sl.sales_amount)::numeric * 100, 2)" +
        "         ELSE 0 END AS taux_marge" +
        "  FROM sales s" +
        "  INNER JOIN sales_line sl ON s.id = sl.sales_id" +
        "  WHERE s.statut = 'CLOSED' AND s.canceled = false AND s.ca = 'CA'" +
        "    AND s.sale_date >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '11 months'" +
        "    AND s.sale_date <= CURRENT_DATE" +
        "  GROUP BY yr, mo, s.nature_vente" +
        "  ORDER BY yr, mo, segment";

    private final EntityManager entityManager;

    public PnlAnalytiqueRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findSnapshotBySegment(int year) {
        return entityManager.createNativeQuery(SEGMENT_SQL)
            .setParameter("year", year)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findSnapshotByFamille(int year) {
        return entityManager.createNativeQuery(FAMILLE_SQL)
            .setParameter("year", year)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findEvolutionMonthlyByFamille() {
        return entityManager.createNativeQuery(EVOLUTION_SQL).getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findEvolutionMonthlyBySegment() {
        return entityManager.createNativeQuery(EVOLUTION_SEGMENT_SQL).getResultList();
    }
}
