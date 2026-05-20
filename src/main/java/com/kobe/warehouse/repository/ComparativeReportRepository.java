package com.kobe.warehouse.repository;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class ComparativeReportRepository {

    private static final String MONTHLY_SQL =
        "WITH current_year AS (" +
        "  SELECT" +
        "    EXTRACT(MONTH FROM s.sale_date) AS month," +
        "    SUM(s.sales_amount - s.discount_amount) AS ca," +
        "    COUNT(DISTINCT s.id) AS nb_trans" +
        "  FROM sales s" +
        "  WHERE s.statut = 'CLOSED'" +
        "    AND s.canceled = false" +
        "    AND s.ca = 'CA'" +
        "    AND EXTRACT(YEAR FROM s.sale_date) = :currentYear" +
        "  GROUP BY month" +
        ")," +
        "previous_year AS (" +
        "  SELECT" +
        "    EXTRACT(MONTH FROM s.sale_date) AS month," +
        "    SUM(s.sales_amount - s.discount_amount) AS ca," +
        "    COUNT(DISTINCT s.id) AS nb_trans" +
        "  FROM sales s" +
        "  WHERE s.statut = 'CLOSED'" +
        "    AND s.canceled = false" +
        "    AND s.ca = 'CA'" +
        "    AND EXTRACT(YEAR FROM s.sale_date) = :previousYear" +
        "  GROUP BY month" +
        ") " +
        "SELECT" +
        "  cy.month," +
        "  COALESCE(cy.ca, 0) AS current_ca," +
        "  COALESCE(py.ca, 0) AS previous_ca," +
        "  COALESCE(cy.nb_trans, 0) AS current_trans," +
        "  COALESCE(py.nb_trans, 0) AS previous_trans" +
        " FROM current_year cy" +
        " LEFT JOIN previous_year py ON cy.month = py.month" +
        " ORDER BY cy.month";

    private static final String QUARTERLY_SQL =
        "WITH current_year AS (" +
        "  SELECT" +
        "    EXTRACT(QUARTER FROM s.sale_date) AS quarter," +
        "    SUM(s.sales_amount - s.discount_amount) AS ca," +
        "    COUNT(DISTINCT s.id) AS nb_trans" +
        "  FROM sales s" +
        "  WHERE s.statut = 'CLOSED'" +
        "    AND s.canceled = false" +
        "    AND s.ca = 'CA'" +
        "    AND EXTRACT(YEAR FROM s.sale_date) = :currentYear" +
        "  GROUP BY quarter" +
        ")," +
        "previous_year AS (" +
        "  SELECT" +
        "    EXTRACT(QUARTER FROM s.sale_date) AS quarter," +
        "    SUM(s.sales_amount - s.discount_amount) AS ca," +
        "    COUNT(DISTINCT s.id) AS nb_trans" +
        "  FROM sales s" +
        "  WHERE s.statut = 'CLOSED'" +
        "    AND s.canceled = false" +
        "    AND s.ca = 'CA'" +
        "    AND EXTRACT(YEAR FROM s.sale_date) = :previousYear" +
        "  GROUP BY quarter" +
        ") " +
        "SELECT" +
        "  cy.quarter," +
        "  COALESCE(cy.ca, 0) AS current_ca," +
        "  COALESCE(py.ca, 0) AS previous_ca," +
        "  COALESCE(cy.nb_trans, 0) AS current_trans," +
        "  COALESCE(py.nb_trans, 0) AS previous_trans" +
        " FROM current_year cy" +
        " LEFT JOIN previous_year py ON cy.quarter = py.quarter" +
        " ORDER BY cy.quarter";

    private static final String YEARLY_SQL =
        "SELECT" +
        "  EXTRACT(YEAR FROM s.sale_date) AS year," +
        "  SUM(s.sales_amount - s.discount_amount) AS ca," +
        "  COUNT(DISTINCT s.id) AS nb_trans" +
        " FROM sales s" +
        " WHERE s.statut = 'CLOSED'" +
        "   AND s.canceled = false" +
        "   AND s.ca = 'CA'" +
        "   AND s.sale_date BETWEEN :startDate AND :endDate" +
        " GROUP BY year" +
        " ORDER BY year";

    private static final String BY_TYPE_SQL =
        "WITH current_year AS (" +
        "  SELECT" +
        "    s.nature_vente AS type," +
        "    SUM(s.sales_amount - s.discount_amount) AS ca," +
        "    COUNT(DISTINCT s.id) AS count" +
        "  FROM sales s" +
        "  WHERE s.statut = 'CLOSED'" +
        "    AND s.canceled = false" +
        "    AND s.ca = 'CA'" +
        "    AND EXTRACT(YEAR FROM s.sale_date) = :currentYear" +
        "  GROUP BY s.nature_vente" +
        ")," +
        "previous_year AS (" +
        "  SELECT" +
        "    s.nature_vente AS type," +
        "    SUM(s.sales_amount - s.discount_amount) AS ca," +
        "    COUNT(DISTINCT s.id) AS count" +
        "  FROM sales s" +
        "  WHERE s.statut = 'CLOSED'" +
        "    AND s.canceled = false" +
        "    AND s.ca = 'CA'" +
        "    AND EXTRACT(YEAR FROM s.sale_date) = :previousYear" +
        "  GROUP BY s.nature_vente" +
        ") " +
        "SELECT" +
        "  COALESCE(cy.type, py.type) AS type," +
        "  COALESCE(cy.ca, 0) AS current_ca," +
        "  COALESCE(py.ca, 0) AS previous_ca," +
        "  COALESCE(cy.count, 0) AS current_count," +
        "  COALESCE(py.count, 0) AS previous_count" +
        " FROM current_year cy" +
        " FULL OUTER JOIN previous_year py ON cy.type = py.type" +
        " ORDER BY COALESCE(cy.ca, 0) DESC";

    private static final String BY_FAMILY_SQL =
        "WITH current_year AS (" +
        "  SELECT" +
        "    fp.id       AS famille_id," +
        "    fp.libelle  AS famille_libelle," +
        "    SUM(sl.sales_amount - sl.discount_amount) AS ca," +
        "    COUNT(DISTINCT s.id)                      AS nb_trans" +
        "  FROM sales s" +
        "  INNER JOIN sales_line sl ON s.id = sl.sales_id" +
        "  INNER JOIN produit p     ON sl.produit_id = p.id" +
        "  INNER JOIN famille_produit fp ON p.famille_id = fp.id" +
        "  WHERE s.statut   = 'CLOSED'" +
        "    AND s.canceled = false" +
        "    AND s.ca       = 'CA'" +
        "    AND EXTRACT(YEAR FROM s.sale_date) = :currentYear" +
        "  GROUP BY fp.id, fp.libelle" +
        ")," +
        "previous_year AS (" +
        "  SELECT" +
        "    fp.id       AS famille_id," +
        "    fp.libelle  AS famille_libelle," +
        "    SUM(sl.sales_amount - sl.discount_amount) AS ca," +
        "    COUNT(DISTINCT s.id)                      AS nb_trans" +
        "  FROM sales s" +
        "  INNER JOIN sales_line sl ON s.id = sl.sales_id" +
        "  INNER JOIN produit p     ON sl.produit_id = p.id" +
        "  INNER JOIN famille_produit fp ON p.famille_id = fp.id" +
        "  WHERE s.statut   = 'CLOSED'" +
        "    AND s.canceled = false" +
        "    AND s.ca       = 'CA'" +
        "    AND EXTRACT(YEAR FROM s.sale_date) = :previousYear" +
        "  GROUP BY fp.id, fp.libelle" +
        ") " +
        "SELECT" +
        "  COALESCE(cy.famille_id,      py.famille_id)      AS famille_id," +
        "  COALESCE(cy.famille_libelle, py.famille_libelle) AS famille_libelle," +
        "  COALESCE(cy.ca,       0) AS current_ca," +
        "  COALESCE(py.ca,       0) AS previous_ca," +
        "  COALESCE(cy.nb_trans, 0) AS current_trans," +
        "  COALESCE(py.nb_trans, 0) AS previous_trans" +
        " FROM current_year cy" +
        " FULL OUTER JOIN previous_year py ON cy.famille_id = py.famille_id" +
        " ORDER BY COALESCE(cy.ca, 0) DESC";

    private static final String BY_FOURNISSEUR_SQL =
        "WITH current_year AS (" +
        "  SELECT" +
        "    f.id        AS fournisseur_id," +
        "    f.libelle   AS fournisseur_libelle," +
        "    SUM(sl.sales_amount - sl.discount_amount) AS ca," +
        "    COUNT(DISTINCT s.id)                      AS nb_trans" +
        "  FROM sales s" +
        "  INNER JOIN sales_line sl ON s.id = sl.sales_id" +
        "  INNER JOIN produit p     ON sl.produit_id = p.id" +
        "  INNER JOIN fournisseur f ON p.fournisseur_id = f.id" +
        "  WHERE s.statut   = 'CLOSED'" +
        "    AND s.canceled = false" +
        "    AND s.ca       = 'CA'" +
        "    AND EXTRACT(YEAR FROM s.sale_date) = :currentYear" +
        "  GROUP BY f.id, f.libelle" +
        ")," +
        "previous_year AS (" +
        "  SELECT" +
        "    f.id        AS fournisseur_id," +
        "    f.libelle   AS fournisseur_libelle," +
        "    SUM(sl.sales_amount - sl.discount_amount) AS ca," +
        "    COUNT(DISTINCT s.id)                      AS nb_trans" +
        "  FROM sales s" +
        "  INNER JOIN sales_line sl ON s.id = sl.sales_id" +
        "  INNER JOIN produit p     ON sl.produit_id = p.id" +
        "  INNER JOIN fournisseur f ON p.fournisseur_id = f.id" +
        "  WHERE s.statut   = 'CLOSED'" +
        "    AND s.canceled = false" +
        "    AND s.ca       = 'CA'" +
        "    AND EXTRACT(YEAR FROM s.sale_date) = :previousYear" +
        "  GROUP BY f.id, f.libelle" +
        ") " +
        "SELECT" +
        "  COALESCE(cy.fournisseur_id,      py.fournisseur_id)      AS fournisseur_id," +
        "  COALESCE(cy.fournisseur_libelle, py.fournisseur_libelle) AS fournisseur_libelle," +
        "  COALESCE(cy.ca,       0) AS current_ca," +
        "  COALESCE(py.ca,       0) AS previous_ca," +
        "  COALESCE(cy.nb_trans, 0) AS current_trans," +
        "  COALESCE(py.nb_trans, 0) AS previous_trans" +
        " FROM current_year cy" +
        " FULL OUTER JOIN previous_year py ON cy.fournisseur_id = py.fournisseur_id" +
        " ORDER BY COALESCE(cy.ca, 0) DESC";

    private static final String PERIOD_CA_SQL =
        "SELECT COALESCE(SUM(s.sales_amount - s.discount_amount), 0)" +
        " FROM sales s" +
        " WHERE s.statut = 'CLOSED'" +
        "   AND s.canceled = false" +
        "   AND s.ca = 'CA'" +
        "   AND s.sale_date BETWEEN :start AND :end";

    private final EntityManager entityManager;

    public ComparativeReportRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findMonthlyComparison(int year, int previousYear) {
        return entityManager
            .createNativeQuery(MONTHLY_SQL)
            .setParameter("currentYear", year)
            .setParameter("previousYear", previousYear)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findQuarterlyComparison(int year, int previousYear) {
        return entityManager
            .createNativeQuery(QUARTERLY_SQL)
            .setParameter("currentYear", year)
            .setParameter("previousYear", previousYear)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findYearlyComparison(LocalDate startDate, LocalDate endDate) {
        return entityManager
            .createNativeQuery(YEARLY_SQL)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findComparisonBySalesType(int currentYear, int previousYear) {
        return entityManager
            .createNativeQuery(BY_TYPE_SQL)
            .setParameter("currentYear", currentYear)
            .setParameter("previousYear", previousYear)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findComparisonByFamily(int currentYear, int previousYear) {
        return entityManager
            .createNativeQuery(BY_FAMILY_SQL)
            .setParameter("currentYear", currentYear)
            .setParameter("previousYear", previousYear)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findComparisonByFournisseur(int currentYear, int previousYear) {
        return entityManager
            .createNativeQuery(BY_FOURNISSEUR_SQL)
            .setParameter("currentYear", currentYear)
            .setParameter("previousYear", previousYear)
            .getResultList();
    }

    public long getPeriodCA(LocalDate start, LocalDate end) {
        return ((Number) entityManager
            .createNativeQuery(PERIOD_CA_SQL)
            .setParameter("start", start)
            .setParameter("end", end)
            .getSingleResult()).longValue();
    }
}
