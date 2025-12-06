package com.kobe.warehouse.service.report;

import com.kobe.warehouse.domain.enumeration.ClassePareto;
import com.kobe.warehouse.service.dto.report.ABCParetoDTO;
import com.kobe.warehouse.service.dto.report.ABCParetoSummaryDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
@Transactional(readOnly = true)
public class ABCParetoReportServiceImpl  implements ABCParetoReportService {


    private final EntityManager entityManager;

    public ABCParetoReportServiceImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    @Override
    @Cacheable(value = "abcPareto", key = "'all'")
    public List<ABCParetoDTO> getAllABCParetoAnalysis() {
        String sql =
            "SELECT " +
            "produit_id, libelle, code_cip, categorie, ca_total, qte_vendue, nb_ventes, " +
            "ca_global, ca_cumule, contribution_pct, ca_cumule_pct, classe_pareto, rang " +
            "FROM mv_abc_pareto_analysis " +
            "ORDER BY rang ";

        Query query = entityManager.createNativeQuery(sql);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    @Cacheable(value = "abcPareto", key = "'category:' + #categorie")
    public List<ABCParetoDTO> getABCParetoByCategory(String categorie) {
        String sql =
            "SELECT " +
            "produit_id, libelle, code_cip, categorie, ca_total, qte_vendue, nb_ventes, " +
            "ca_global, ca_cumule, contribution_pct, ca_cumule_pct, classe_pareto, rang " +
            "FROM mv_abc_pareto_analysis " +
            "WHERE categorie = :categorie " +
            "ORDER BY rang ";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("categorie", categorie);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    @Cacheable(value = "abcPareto", key = "'class:' + #classePareto")
    public List<ABCParetoDTO> getABCParetoByClass(ClassePareto classePareto) {
        String sql =
            "SELECT " +
            "produit_id, libelle, code_cip, categorie, ca_total, qte_vendue, nb_ventes, " +
            "ca_global, ca_cumule, contribution_pct, ca_cumule_pct, classe_pareto, rang " +
            "FROM mv_abc_pareto_analysis " +
            "WHERE classe_pareto = :classePareto " +
            "ORDER BY rang ";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("classePareto", classePareto.name());

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    @Cacheable(value = "abcPareto", key = "'top:' + #limit")
    public List<ABCParetoDTO> getTopRevenueContributors(int limit) {
        String sql =
            "SELECT " +
            "produit_id, libelle, code_cip, categorie, ca_total, qte_vendue, nb_ventes, " +
            "ca_global, ca_cumule, contribution_pct, ca_cumule_pct, classe_pareto, rang " +
            "FROM mv_abc_pareto_analysis " +
            "ORDER BY rang  ";

        Query query = entityManager.createNativeQuery(sql);
        query.setMaxResults(limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return mapResultsToDTO(results);
    }

    @Override
    public ABCParetoSummaryDTO getABCParetoSummary() {
        String sql =
            "SELECT " +
            "total_produits, ca_global, " +
            "nb_produits_a, ca_classe_a, pct_ca_classe_a, " +
            "nb_produits_b, ca_classe_b, pct_ca_classe_b, " +
            "nb_produits_c, ca_classe_c, pct_ca_classe_c " +
            "FROM mv_pareto_summary";

        Query query = entityManager.createNativeQuery(sql);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        if (results.isEmpty()) {
            return new ABCParetoSummaryDTO(
                0,
                0L,
                0,
                0L,
                BigDecimal.ZERO,
                0,
                0L,
                BigDecimal.ZERO,
                0,
                0L,
                BigDecimal.ZERO
            );
        }

        Object[] row = results.getFirst();

        return new ABCParetoSummaryDTO(
            row[0] != null ? ((Number) row[0]).intValue() : 0,
            row[1] != null ? ((Number) row[1]).longValue() : 0L,
            row[2] != null ? ((Number) row[2]).intValue() : 0,
            row[3] != null ? ((Number) row[3]).longValue() : 0L,
            row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO,
            row[5] != null ? ((Number) row[5]).intValue() : 0,
            row[6] != null ? ((Number) row[6]).longValue() : 0L,
            row[7] != null ? new BigDecimal(row[7].toString()) : BigDecimal.ZERO,
            row[8] != null ? ((Number) row[8]).intValue() : 0,
            row[9] != null ? ((Number) row[9]).longValue() : 0L,
            row[10] != null ? new BigDecimal(row[10].toString()) : BigDecimal.ZERO
        );
    }



    private List<ABCParetoDTO> mapResultsToDTO(List<Object[]> results) {
        return results
            .stream()
            .map(row -> {
                Integer produitId = row[0] != null ? ((Number) row[0]).intValue() : null;
                String libelle = (String) row[1];
                String codeCip = (String) row[2];
                String categorie = (String) row[3];
                Integer caTotal = row[4] != null ? ((Number) row[4]).intValue() : 0;
                Integer qteVendue = row[5] != null ? ((Number) row[5]).intValue() : 0;
                Integer nbVentes = row[6] != null ? ((Number) row[6]).intValue() : 0;
                Long caGlobal = row[7] != null ? ((Number) row[7]).longValue() : 0L;
                Long caCumule = row[8] != null ? ((Number) row[8]).longValue() : 0L;
                BigDecimal contributionPct = row[9] != null ? new BigDecimal(row[9].toString()) : BigDecimal.ZERO;
                BigDecimal caCumulePct = row[10] != null ? new BigDecimal(row[10].toString()) : BigDecimal.ZERO;
                String classeParetoStr = (String) row[11];
                Integer rang = row[12] != null ? ((Number) row[12]).intValue() : 0;

                ClassePareto classePareto = classeParetoStr != null ? ClassePareto.valueOf(classeParetoStr) : ClassePareto.C;

                return new ABCParetoDTO(
                    produitId,
                    libelle,
                    codeCip,
                    categorie,
                    caTotal,
                    qteVendue,
                    nbVentes,
                    caGlobal,
                    caCumule,
                    contributionPct,
                    caCumulePct,
                    classePareto,
                    rang
                );
            })
            .toList();
    }
}
