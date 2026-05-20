package com.kobe.warehouse.repository;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class ConcentrationPayersRepository {

    // CA by TP organism for a date range with HHI share (% of total TP CA)
    private static final String CONCENTRATION_SQL = """
        WITH tp_ca AS (
          SELECT
            COALESCE(tp.name, gtp.name, 'Inconnu') AS organisme,
            SUM(GREATEST(CAST(f.montant_net AS bigint), 0)) AS ca_tp,
            SUM(GREATEST(CAST(f.montant_regle AS bigint), 0)) AS regle_tp,
            COUNT(*) AS nb_factures,
            MAX(COALESCE(tp.delai_reglement, gtp.delai_reglement, 30)) AS delai_reglement
          FROM facture_tiers_payant f
          LEFT JOIN tiers_payant tp ON tp.id = f.tiers_payant_id
          LEFT JOIN groupe_tiers_payant gtp ON gtp.id = f.groupe_tiers_payant_id
          WHERE f.groupe_facture_tiers_payant_id IS NULL
            AND f.invoice_date >= :fromDate
            AND f.invoice_date <= :toDate
          GROUP BY COALESCE(tp.name, gtp.name, 'Inconnu')
        ),
        total AS (
          SELECT
            COALESCE(SUM(ca_tp), 0) AS total_ca,
            COALESCE(SUM(regle_tp), 0) AS total_regle
          FROM tp_ca
        )
        SELECT t.organisme, t.ca_tp, t.nb_factures, t.delai_reglement,
          CASE WHEN tt.total_ca > 0
            THEN ROUND(t.ca_tp::numeric / tt.total_ca::numeric * 100, 2)
            ELSE 0 END AS part_pct,
          tt.total_ca,
          tt.total_regle,
          tt.total_ca - tt.total_regle AS total_impaye
        FROM tp_ca t, total tt
        ORDER BY t.ca_tp DESC
        LIMIT :topN
        """;

    // Monthly TP CA by top-N organism + "Autres" — last 12 months rolling
    private static final String EVOLUTION_SQL = """
        WITH monthly_org AS (
          SELECT
            CAST(EXTRACT(YEAR FROM f.invoice_date) AS int) AS yr,
            CAST(EXTRACT(MONTH FROM f.invoice_date) AS int) AS mo,
            COALESCE(tp.name, gtp.name, 'Inconnu') AS organisme,
            SUM(CAST(f.montant_net AS bigint)) AS ca_tp
          FROM facture_tiers_payant f
          LEFT JOIN tiers_payant tp ON tp.id = f.tiers_payant_id
          LEFT JOIN groupe_tiers_payant gtp ON gtp.id = f.groupe_tiers_payant_id
          WHERE f.groupe_facture_tiers_payant_id IS NULL
            AND f.invoice_date >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '11 months'
            AND f.invoice_date <= CURRENT_DATE
          GROUP BY yr, mo, COALESCE(tp.name, gtp.name, 'Inconnu')
        ),
        top_orgs AS (
          SELECT organisme FROM monthly_org
          GROUP BY organisme ORDER BY SUM(ca_tp) DESC LIMIT :topN
        ),
        classified AS (
          SELECT yr, mo,
            CASE WHEN m.organisme IN (SELECT organisme FROM top_orgs)
              THEN m.organisme ELSE 'Autres' END AS organisme_label,
            SUM(ca_tp) AS ca_tp
          FROM monthly_org m
          GROUP BY yr, mo,
            CASE WHEN m.organisme IN (SELECT organisme FROM top_orgs) THEN m.organisme ELSE 'Autres' END
        )
        SELECT yr, mo, organisme_label, ca_tp
        FROM classified
        ORDER BY yr, mo, ca_tp DESC
        """;

    private final EntityManager entityManager;

    public ConcentrationPayersRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findConcentration(LocalDate fromDate, LocalDate toDate, int topN) {
        return entityManager.createNativeQuery(CONCENTRATION_SQL)
            .setParameter("fromDate", fromDate)
            .setParameter("toDate", toDate)
            .setParameter("topN", topN)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findEvolution(int topN) {
        return entityManager.createNativeQuery(EVOLUTION_SQL)
            .setParameter("topN", topN)
            .getResultList();
    }
}
