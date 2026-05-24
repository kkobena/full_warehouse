package com.kobe.warehouse.repository;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class VieillissementCreancesRepository {

    // Outstanding TP invoices aggregated by aging bucket
    private static final String AGING_GLOBAL_SQL =
        "SELECT" +
        "  COALESCE(SUM(CASE WHEN (CURRENT_DATE - f.invoice_date) <= 30" +
        "    THEN GREATEST((f.montant_net - f.montant_regle)::bigint, 0) ELSE 0 END), 0) AS tranche_0_30," +
        "  COALESCE(SUM(CASE WHEN (CURRENT_DATE - f.invoice_date) BETWEEN 31 AND 60" +
        "    THEN GREATEST((f.montant_net - f.montant_regle)::bigint, 0) ELSE 0 END), 0) AS tranche_31_60," +
        "  COALESCE(SUM(CASE WHEN (CURRENT_DATE - f.invoice_date) BETWEEN 61 AND 90" +
        "    THEN GREATEST((f.montant_net - f.montant_regle)::bigint, 0) ELSE 0 END), 0) AS tranche_61_90," +
        "  COALESCE(SUM(CASE WHEN (CURRENT_DATE - f.invoice_date) > 90" +
        "    THEN GREATEST((f.montant_net - f.montant_regle)::bigint, 0) ELSE 0 END), 0) AS tranche_90_plus," +
        "  COALESCE(SUM(GREATEST((f.montant_net - f.montant_regle)::bigint, 0)), 0) AS total_encours," +
        "  COUNT(*) AS nb_factures," +
        "  COALESCE(SUM(CASE WHEN (CURRENT_DATE - f.invoice_date)" +
        "    > COALESCE(tp.delai_reglement, gtp.delai_reglement, 30) THEN 1 ELSE 0 END), 0) AS nb_en_retard" +
        " FROM facture_tiers_payant f" +
        " LEFT JOIN tiers_payant tp ON tp.id = f.tiers_payant_id" +
        " LEFT JOIN groupe_tiers_payant gtp ON gtp.id = f.groupe_tiers_payant_id" +
        " WHERE f.statut IN ('NOT_PAID', 'PARTIALLY_PAID')" +
        "   AND f.groupe_facture_tiers_payant_id IS NULL";

    // Outstanding TP invoices grouped by payer organism with DSO estimate
    private static final String AGING_BY_ORGANISME_SQL =
        "SELECT" +
        "  COALESCE(tp.name, gtp.name, 'Inconnu') AS organisme," +
        "  COALESCE(tp.delai_reglement, gtp.delai_reglement, 30) AS delai_reglement," +
        "  SUM(GREATEST((f.montant_net - f.montant_regle)::bigint, 0)) AS encours," +
        "  SUM(CASE WHEN (CURRENT_DATE - f.invoice_date) <= 30" +
        "    THEN GREATEST((f.montant_net - f.montant_regle)::bigint, 0) ELSE 0 END) AS tranche_0_30," +
        "  SUM(CASE WHEN (CURRENT_DATE - f.invoice_date) BETWEEN 31 AND 60" +
        "    THEN GREATEST((f.montant_net - f.montant_regle)::bigint, 0) ELSE 0 END) AS tranche_31_60," +
        "  SUM(CASE WHEN (CURRENT_DATE - f.invoice_date) BETWEEN 61 AND 90" +
        "    THEN GREATEST((f.montant_net - f.montant_regle)::bigint, 0) ELSE 0 END) AS tranche_61_90," +
        "  SUM(CASE WHEN (CURRENT_DATE - f.invoice_date) > 90" +
        "    THEN GREATEST((f.montant_net - f.montant_regle)::bigint, 0) ELSE 0 END) AS tranche_90_plus," +
        "  COUNT(*) AS nb_factures," +
        "  SUM(CASE WHEN (CURRENT_DATE - f.invoice_date)" +
        "    > COALESCE(tp.delai_reglement, gtp.delai_reglement, 30) THEN 1 ELSE 0 END) AS nb_en_retard," +
        "  CASE WHEN SUM(GREATEST((f.montant_net - f.montant_regle)::bigint, 0)) > 0" +
        "    THEN ROUND(" +
        "      SUM(GREATEST(CURRENT_DATE - f.invoice_date, 0)::numeric" +
        "          * GREATEST((f.montant_net - f.montant_regle)::bigint, 0)::numeric)" +
        "      / SUM(GREATEST((f.montant_net - f.montant_regle)::bigint, 0))::numeric" +
        "    )::int" +
        "    ELSE 0 END AS dso_jours" +
        " FROM facture_tiers_payant f" +
        " LEFT JOIN tiers_payant tp ON tp.id = f.tiers_payant_id" +
        " LEFT JOIN groupe_tiers_payant gtp ON gtp.id = f.groupe_tiers_payant_id" +
        " WHERE f.statut IN ('NOT_PAID', 'PARTIALLY_PAID')" +
        "   AND f.groupe_facture_tiers_payant_id IS NULL" +
        " GROUP BY COALESCE(tp.name, gtp.name, 'Inconnu'), COALESCE(tp.delai_reglement, gtp.delai_reglement, 30)" +
        " ORDER BY encours DESC";

    // Monthly TP invoices: total billed vs outstanding balance — last 12 months
    private static final String ENCOURS_MENSUEL_SQL =
        "SELECT" +
        "  CAST(EXTRACT(YEAR  FROM f.invoice_date) AS int)                           AS yr," +
        "  CAST(EXTRACT(MONTH FROM f.invoice_date) AS int)                           AS mo," +
        "  SUM(f.montant_net)                                                         AS montant_facture," +
        "  SUM(GREATEST(f.montant_net - COALESCE(f.montant_regle, 0), 0))             AS encours_restant" +
        " FROM facture_tiers_payant f" +
        " WHERE f.invoice_date >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '11 months'" +
        "   AND f.invoice_date <= CURRENT_DATE" +
        "   AND f.groupe_facture_tiers_payant_id IS NULL" +
        " GROUP BY yr, mo" +
        " ORDER BY yr, mo";

    private final EntityManager entityManager;

    public VieillissementCreancesRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public Object[] findAgingGlobal() {
        return (Object[]) entityManager.createNativeQuery(AGING_GLOBAL_SQL).getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findAgingByOrganisme(int offset, int limit) {
        return entityManager.createNativeQuery(AGING_BY_ORGANISME_SQL)
            .setFirstResult(offset)
            .setMaxResults(limit)
            .getResultList();
    }

    public long countAgingByOrganisme() {
        String countSql =
            "SELECT COUNT(*) FROM (" +
            "  SELECT COALESCE(tp.name, gtp.name, 'Inconnu')" +
            "  FROM facture_tiers_payant f" +
            "  LEFT JOIN tiers_payant tp ON tp.id = f.tiers_payant_id" +
            "  LEFT JOIN groupe_tiers_payant gtp ON gtp.id = f.groupe_tiers_payant_id" +
            "  WHERE f.statut IN ('NOT_PAID', 'PARTIALLY_PAID')" +
            "    AND f.groupe_facture_tiers_payant_id IS NULL" +
            "  GROUP BY COALESCE(tp.name, gtp.name, 'Inconnu')" +
            ") sub";
        return ((Number) entityManager.createNativeQuery(countSql).getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findEncoursMensuelEvolution() {
        return entityManager.createNativeQuery(ENCOURS_MENSUEL_SQL).getResultList();
    }
}
