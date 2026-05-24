package com.kobe.warehouse.service.report;

import com.kobe.warehouse.domain.enumeration.ClassePareto;
import com.kobe.warehouse.service.dto.report.ABCParetoDTO;
import com.kobe.warehouse.service.dto.report.ABCParetoSummaryDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du rapport ABC Pareto depuis {@code v_abc_pareto_analysis}.
 *
 * <p>La vue remplace {@code mv_abc_pareto_analysis} (vue matérialisée supprimée en V1.3.4).
 * Colonnes renvoyées (indices 0-14) :
 * [0] produit_id  [1] libelle  [2] code_cip  [3] famille  [4] classe_actuelle
 * [5] ca_total    [6] qte_vendue  [7] nb_ventes  [8] frequence_mois
 * [9] ca_global   [10] ca_cumule  [11] contribution_pct  [12] ca_cumule_pct
 * [13] rang       [14] classe_pareto
 */
@Service
@Transactional(readOnly = true)
public class ABCParetoReportServiceImpl implements ABCParetoReportService {

    private static final String SELECT_COLS =
        "produit_id, libelle, code_cip, famille, classe_actuelle, " +
        "ca_total, qte_vendue, nb_ventes, frequence_mois, " +
        "ca_global, ca_cumule, contribution_pct, ca_cumule_pct, rang, classe_pareto ";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Cacheable(value = "abcPareto", key = "'all'")
    public List<ABCParetoDTO> getAllABCParetoAnalysis() {
        Query query = entityManager.createNativeQuery(
            "SELECT " + SELECT_COLS + "FROM v_abc_pareto_analysis ORDER BY rang");
        return mapResultsToDTO(query.getResultList());
    }

    @Override
    @Cacheable(value = "abcPareto", key = "'family:' + #famille")
    public List<ABCParetoDTO> getABCParetoByCategory(String famille) {
        Query query = entityManager.createNativeQuery(
            "SELECT " + SELECT_COLS + "FROM v_abc_pareto_analysis WHERE famille = :famille ORDER BY rang");
        query.setParameter("famille", famille);
        return mapResultsToDTO(query.getResultList());
    }

    @Override
    @Cacheable(value = "abcPareto", key = "'class:' + #classePareto")
    public List<ABCParetoDTO> getABCParetoByClass(ClassePareto classePareto) {
        Query query = entityManager.createNativeQuery(
            "SELECT " + SELECT_COLS + "FROM v_abc_pareto_analysis WHERE classe_pareto = :cp ORDER BY rang");
        query.setParameter("cp", classePareto.name());
        return mapResultsToDTO(query.getResultList());
    }

    @Override
    @Cacheable(value = "abcPareto", key = "'top:' + #limit")
    public List<ABCParetoDTO> getTopRevenueContributors(int limit) {
        Query query = entityManager.createNativeQuery(
            "SELECT " + SELECT_COLS + "FROM v_abc_pareto_analysis ORDER BY rang");
        query.setMaxResults(limit);
        return mapResultsToDTO(query.getResultList());
    }

    @Override
    public List<ABCParetoDTO> getABCParetoPaginated(int page, int size) {
        Query query = entityManager.createNativeQuery(
            "SELECT " + SELECT_COLS + "FROM v_abc_pareto_analysis ORDER BY rang LIMIT :size OFFSET :offset");
        query.setParameter("size", size);
        query.setParameter("offset", page * size);
        return mapResultsToDTO(query.getResultList());
    }

    @Override
    @Cacheable(value = "abcPareto", key = "'count'")
    public long getABCParetoCount() {
        return ((Number) entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM v_abc_pareto_analysis").getSingleResult()).longValue();
    }

    @Override
    public List<ABCParetoDTO> getABCParetoByClassPaginated(ClassePareto classePareto, int page, int size) {
        Query query = entityManager.createNativeQuery(
            "SELECT " + SELECT_COLS +
            "FROM v_abc_pareto_analysis WHERE classe_pareto = :cp ORDER BY rang LIMIT :size OFFSET :offset");
        query.setParameter("cp", classePareto.name());
        query.setParameter("size", size);
        query.setParameter("offset", page * size);
        return mapResultsToDTO(query.getResultList());
    }

    @Override
    public long getABCParetoCountByClass(ClassePareto classePareto) {
        Query query = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM v_abc_pareto_analysis WHERE classe_pareto = :cp");
        query.setParameter("cp", classePareto.name());
        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    @Cacheable(value = "abcPareto", key = "'summary'")
    public ABCParetoSummaryDTO getABCParetoSummary() {
        // Calcul inline — mv_pareto_summary supprimée en V1.3.4
        Query query = entityManager.createNativeQuery("""
            SELECT
                COUNT(*)                                                                                        AS total_produits,
                COALESCE(SUM(ca_total), 0)                                                                      AS ca_global,
                COUNT(*)        FILTER (WHERE classe_pareto = 'A_PLUS')                                         AS nb_a_plus,
                COALESCE(SUM(ca_total) FILTER (WHERE classe_pareto = 'A_PLUS'), 0)                              AS ca_a_plus,
                ROUND(COALESCE(SUM(ca_total) FILTER (WHERE classe_pareto = 'A_PLUS'), 0) * 100.0
                      / NULLIF(SUM(ca_total), 0), 2)                                                            AS pct_a_plus,
                COUNT(*)        FILTER (WHERE classe_pareto = 'A')                                              AS nb_a,
                COALESCE(SUM(ca_total) FILTER (WHERE classe_pareto = 'A'), 0)                                   AS ca_a,
                ROUND(COALESCE(SUM(ca_total) FILTER (WHERE classe_pareto = 'A'), 0) * 100.0
                      / NULLIF(SUM(ca_total), 0), 2)                                                            AS pct_a,
                COUNT(*)        FILTER (WHERE classe_pareto = 'B')                                              AS nb_b,
                COALESCE(SUM(ca_total) FILTER (WHERE classe_pareto = 'B'), 0)                                   AS ca_b,
                ROUND(COALESCE(SUM(ca_total) FILTER (WHERE classe_pareto = 'B'), 0) * 100.0
                      / NULLIF(SUM(ca_total), 0), 2)                                                            AS pct_b,
                COUNT(*)        FILTER (WHERE classe_pareto = 'C')                                              AS nb_c,
                COALESCE(SUM(ca_total) FILTER (WHERE classe_pareto = 'C'), 0)                                   AS ca_c,
                ROUND(COALESCE(SUM(ca_total) FILTER (WHERE classe_pareto = 'C'), 0) * 100.0
                      / NULLIF(SUM(ca_total), 0), 2)                                                            AS pct_c,
                COUNT(*)        FILTER (WHERE classe_pareto = 'D')                                              AS nb_d,
                COALESCE(SUM(ca_total) FILTER (WHERE classe_pareto = 'D'), 0)                                   AS ca_d,
                ROUND(COALESCE(SUM(ca_total) FILTER (WHERE classe_pareto = 'D'), 0) * 100.0
                      / NULLIF(SUM(ca_total), 0), 2)                                                            AS pct_d
            FROM v_abc_pareto_analysis
            """);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        if (rows.isEmpty()) {
            return emptySummary();
        }

        Object[] r = rows.getFirst();
        return new ABCParetoSummaryDTO(
            toInt(r[0]),  toLong(r[1]),
            toInt(r[2]),  toLong(r[3]),  toBd(r[4]),
            toInt(r[5]),  toLong(r[6]),  toBd(r[7]),
            toInt(r[8]),  toLong(r[9]),  toBd(r[10]),
            toInt(r[11]), toLong(r[12]), toBd(r[13]),
            toInt(r[14]), toLong(r[15]), toBd(r[16])
        );
    }

    // ── mapping ──

    @SuppressWarnings("unchecked")
    private List<ABCParetoDTO> mapResultsToDTO(List<Object[]> results) {
        return results.stream().map(r -> {
            String cpStr = (String) r[14];
            ClassePareto cp;
            try {
                cp = cpStr != null ? ClassePareto.valueOf(cpStr) : ClassePareto.D;
            } catch (IllegalArgumentException e) {
                cp = ClassePareto.D;
            }
            return new ABCParetoDTO(
                toInt(r[0]),          // produit_id
                (String) r[1],        // libelle
                (String) r[2],        // code_cip
                (String) r[3],        // famille
                (String) r[4],        // classe_actuelle
                toInt(r[5]),          // ca_total
                toInt(r[6]),          // qte_vendue
                toInt(r[7]),          // nb_ventes
                toInt(r[8]),          // frequence_mois
                toLong(r[9]),         // ca_global
                toLong(r[10]),        // ca_cumule
                toBd(r[11]),          // contribution_pct
                toBd(r[12]),          // ca_cumule_pct
                toInt(r[13]),         // rang
                cp                    // classe_pareto
            );
        }).toList();
    }

    private static ABCParetoSummaryDTO emptySummary() {
        return new ABCParetoSummaryDTO(
            0, 0L,
            0, 0L, BigDecimal.ZERO,
            0, 0L, BigDecimal.ZERO,
            0, 0L, BigDecimal.ZERO,
            0, 0L, BigDecimal.ZERO,
            0, 0L, BigDecimal.ZERO
        );
    }

    private static int toInt(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    private static long toLong(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private static BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        return new BigDecimal(o.toString());
    }
}
