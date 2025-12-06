package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.TopProductDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TopProductsReportServiceImpl implements TopProductsReportService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Cacheable(value = "topProducts", key = "#month.toString() + '-revenue-' + #limit")
    public List<TopProductDTO> getTopProductsByRevenue(LocalDate month, int limit) {
        // Ensure we're at the first day of the month
        LocalDate firstDayOfMonth = month.with(TemporalAdjusters.firstDayOfMonth());

        String sql = """
    SELECT
        mois,
        produit_id,
        libelle,
        code_cip,
        nb_ventes,
        qte_vendue,
        ca_genere,
        prix_moyen
    FROM mv_monthly_top_products
    WHERE  mois =?1
    ORDER BY ca_genere DESC
    """;



        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, firstDayOfMonth.toString());
        query.setMaxResults(limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapToTopProductDTOs(results);
    }

    @Override
    @Cacheable(value = "topProducts", key = "#month.toString() + '-quantity-' + #limit")
    public List<TopProductDTO> getTopProductsByQuantity(LocalDate month, int limit) {
        // Ensure we're at the first day of the month
        LocalDate firstDayOfMonth = month.with(TemporalAdjusters.firstDayOfMonth());


        String sql = """
    SELECT
        mois,
        produit_id,
        libelle,
        code_cip,
        nb_ventes,
        qte_vendue,
        ca_genere,
        prix_moyen
    FROM mv_monthly_top_products
    WHERE  mois =?1
    ORDER BY qte_vendue DESC
    """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, firstDayOfMonth.toString());
        query.setMaxResults(limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapToTopProductDTOs(results);
    }

    @Override
    @Cacheable(value = "topProducts", key = "#month.toString() + '-all'")
    public List<TopProductDTO> getAllProductsForMonth(LocalDate month) {
        // Ensure we're at the first day of the month
        LocalDate firstDayOfMonth = month.with(TemporalAdjusters.firstDayOfMonth());

        String sql =
            "SELECT " +
                "mois, " +
                "produit_id, " +
                "libelle, " +
                "code_cip, " +
                "nb_ventes, " +
                "qte_vendue, " +
                "ca_genere, " +
                "prix_moyen " +
                "FROM mv_monthly_top_products " +
                "WHERE  mois =?1 " +
                "ORDER BY ca_genere DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, firstDayOfMonth.toString());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapToTopProductDTOs(results);
    }

    @Override
    @Cacheable(value = "topProducts", key = "#produitId + '-evolution-' + #nbMonths")
    public List<TopProductDTO> getProductMonthlyEvolution(Integer produitId, int nbMonths) {
        // Limit to 6 months as per materialized view definition
        int months = Math.min(nbMonths, 6);

        String sql =
            "SELECT " +
                "mois, " +
                "produit_id, " +
                "libelle, " +
                "code_cip, " +
                "nb_ventes, " +
                "qte_vendue, " +
                "ca_genere, " +
                "prix_moyen " +
                "FROM mv_monthly_top_products " +
                "WHERE produit_id = :produitId " +
                "AND mois >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '" +
                months +
                " months' " +
                "ORDER BY mois DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("produitId", produitId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapToTopProductDTOs(results);
    }

    private List<TopProductDTO> mapToTopProductDTOs(List<Object[]> results) {
        return results
            .stream()
            .map(row -> {
                LocalDate mois = row[0] != null ? LocalDate.parse(row[0]+""): null;
                Integer produitId = row[1] != null ? ((Number) row[1]).intValue() : null;
                String libelle = (String) row[2];
                String codeCip = (String) row[3];
                Long nbVentes = row[4] != null ? ((Number) row[4]).longValue() : 0L;
                Integer qteVendue = row[5] != null ? ((Number) row[5]).intValue() : 0;
                Integer caGenere = row[6] != null ? ((Number) row[6]).intValue() : 0;
                BigDecimal prixMoyen = row[7] != null ? new BigDecimal(row[7].toString()) : BigDecimal.ZERO;

                return new TopProductDTO(mois, produitId, libelle, codeCip, nbVentes, qteVendue, caGenere, prixMoyen);
            })
            .toList();
    }
}
