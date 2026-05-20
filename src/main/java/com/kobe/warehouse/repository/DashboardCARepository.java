package com.kobe.warehouse.repository;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class DashboardCARepository {

    private static final String DAILY_SUMMARY_SQL =
        "SELECT sale_date, nb_transactions, ca_total, ca_net," +
        "  panier_moyen, cout_total, marge_brute, taux_marge_pct, nb_clients," +
        "  montant_encaisse, montant_credit" +
        " FROM mv_dashboard_ca_daily" +
        " WHERE sale_date BETWEEN :startDate AND :endDate" +
        " ORDER BY sale_date DESC";

    private static final String PERIOD_AGGREGATION_SQL =
        "SELECT COALESCE(SUM(ca_net), 0) AS ca," +
        "  COALESCE(SUM(nb_transactions), 0) AS nb_trans," +
        "  COALESCE(AVG(panier_moyen), 0) AS panier_moyen," +
        "  COALESCE(AVG(taux_marge_pct), 0) AS taux_marge" +
        " FROM mv_dashboard_ca_daily" +
        " WHERE sale_date BETWEEN :startDate AND :endDate";

    private static final String PAYMENT_METHODS_SQL =
        "SELECT payment_date, payment_method, payment_code, nb_payments," +
        "  montant_total, montant_moyen" +
        " FROM mv_dashboard_ca_payment_methods" +
        " WHERE payment_date BETWEEN :startDate AND :endDate" +
        " ORDER BY montant_total DESC";

    private static final String PRODUCT_FAMILIES_SQL =
        "SELECT sale_date, famille, quantite_vendue, ca_total, cout_total," +
        "  marge_brute, taux_marge_pct, nb_lignes_vente" +
        " FROM mv_dashboard_ca_product_families" +
        " WHERE sale_date BETWEEN :startDate AND :endDate" +
        " ORDER BY ca_total DESC";

    private static final String TOP_PRODUCTS_SQL =
        "SELECT :period AS mois, p.id, p.libelle," +
        "  COALESCE(fp.code_cip, '') AS code_cip," +
        "  COUNT(DISTINCT sl.sales_id) AS nb_ventes," +
        "  SUM(sl.quantity_requested) AS qty_vendue," +
        "  SUM(sl.sales_amount) AS ca," +
        "  AVG(sl.sales_amount / NULLIF(sl.quantity_requested, 0)) AS prix_moyen" +
        " FROM sales s" +
        " INNER JOIN sales_line sl ON s.id = sl.sales_id" +
        " INNER JOIN produit p ON sl.produit_id = p.id" +
        " LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id" +
        " WHERE s.statut = 'CLOSED'" +
        "   AND s.canceled = false" +
        "   AND s.ca = 'CA'" +
        "   AND s.sale_date BETWEEN :startDate AND :endDate" +
        " GROUP BY p.id, p.libelle, fp.code_cip" +
        " ORDER BY ca DESC";

    private static final String BASKET_EVOLUTION_SQL =
        "SELECT" +
        "  CAST(EXTRACT(YEAR FROM sale_date) AS int) AS yr," +
        "  CAST(EXTRACT(MONTH FROM sale_date) AS int) AS mo," +
        "  CASE WHEN SUM(nb_transactions) > 0" +
        "       THEN ROUND(SUM(ca_net)::numeric / SUM(nb_transactions), 2)" +
        "       ELSE 0 END AS basket" +
        " FROM mv_dashboard_ca_daily" +
        " WHERE sale_date >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '11 months'" +
        "   AND sale_date <= CURRENT_DATE" +
        " GROUP BY 1, 2" +
        " ORDER BY 1, 2";

    private static final String DETTE_FOURNISSEUR_SQL =
        "SELECT COALESCE(SUM(c.final_amount), 0) - COALESCE((" +
        "  SELECT SUM(pt.paid_amount) FROM payment_transaction pt" +
        "  WHERE pt.dtype = 'PaymentFournisseur'" +
        "    AND pt.commande_id IN (" +
        "      SELECT c2.id FROM commande c2" +
        "      WHERE c2.paiment_status != 'PAID' AND c2.order_status IN ('CLOSED')" +
        "    )" +
        "), 0)" +
        " FROM commande c" +
        " WHERE c.paiment_status != 'PAID' AND c.order_status IN ('CLOSED')";

    private static final String CREANCE_TP_SQL =
        "SELECT COALESCE(SUM(tpsl.montant - COALESCE(tpsl.montant_regle, 0)), 0)" +
        " FROM third_party_sale_line tpsl" +
        " WHERE tpsl.statut NOT IN ('PAID', 'DELETE')";

    private static final String NB_ECHEANCES_RETARD_SQL =
        "SELECT COUNT(*) FROM commande c" +
        " JOIN fournisseur f ON c.fournisseur_id = f.id" +
        " LEFT JOIN groupe_fournisseur gf ON f.groupe_pournisseur_id = gf.id" +
        " WHERE c.paiment_status != 'PAID' AND c.order_status = 'CLOSED'" +
        "   AND (COALESCE(c.receipt_date, c.order_date) +" +
        "        (COALESCE(f.jours_credit, gf.jours_credit, 30) || ' days')::interval) < CURRENT_DATE";

    private static final String NB_FACTURES_IMPAYEES_SQL =
        "SELECT COUNT(*) FROM sales s" +
        " WHERE s.payment_status = 'IMPAYE' AND s.statut = 'CLOSED'" +
        "   AND s.canceled = false AND s.ca = 'CA'" +
        "   AND s.sale_date < (CURRENT_DATE - INTERVAL '30 days')";

    private final EntityManager entityManager;

    public DashboardCARepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findDailySummary(LocalDate startDate, LocalDate endDate) {
        return entityManager.createNativeQuery(DAILY_SUMMARY_SQL)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    public Object[] getPeriodAggregation(LocalDate startDate, LocalDate endDate) {
        return (Object[]) entityManager.createNativeQuery(PERIOD_AGGREGATION_SQL)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findPaymentMethodDistribution(LocalDate startDate, LocalDate endDate) {
        return entityManager.createNativeQuery(PAYMENT_METHODS_SQL)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findProductFamilyDistribution(LocalDate startDate, LocalDate endDate) {
        return entityManager.createNativeQuery(PRODUCT_FAMILIES_SQL)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findTopProducts(LocalDate startDate, LocalDate endDate, int limit) {
        return entityManager.createNativeQuery(TOP_PRODUCTS_SQL)
            .setParameter("period", startDate)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .setMaxResults(limit)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findBasketEvolutionMonthly() {
        return entityManager.createNativeQuery(BASKET_EVOLUTION_SQL).getResultList();
    }

    public long getDetteFournisseur() {
        Number result = (Number) entityManager.createNativeQuery(DETTE_FOURNISSEUR_SQL).getSingleResult();
        return result != null ? result.longValue() : 0L;
    }

    public long getCreanceTiersPayant() {
        Number result = (Number) entityManager.createNativeQuery(CREANCE_TP_SQL).getSingleResult();
        return result != null ? result.longValue() : 0L;
    }

    public long getNbEcheancesEnRetard() {
        Number result = (Number) entityManager.createNativeQuery(NB_ECHEANCES_RETARD_SQL).getSingleResult();
        return result != null ? result.longValue() : 0L;
    }

    public long getNbFacturesImpayees() {
        Number result = (Number) entityManager.createNativeQuery(NB_FACTURES_IMPAYEES_SQL).getSingleResult();
        return result != null ? result.longValue() : 0L;
    }

    @Transactional
    public void refreshViews() {
        entityManager.createNativeQuery("SELECT refresh_dashboard_ca_views()").getSingleResult();
    }
}
