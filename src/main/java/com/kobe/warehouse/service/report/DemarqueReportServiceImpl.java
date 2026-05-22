package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.DemarqueByMotifDTO;
import com.kobe.warehouse.service.dto.report.DemarqueKpiDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DemarqueReportServiceImpl implements DemarqueReportService {

    private static final String KPI_SQL =
        "SELECT" +
        "  COUNT(DISTINCT a.id) AS nb_ajustements," +
        "  COALESCE(SUM(ABS(aj.qty_mvt)), 0) AS total_qty_perdue," +
        "  COALESCE(SUM(ABS(aj.qty_mvt) * p.cost_amount), 0) AS valeur_perdue" +
        " FROM ajust a" +
        " JOIN ajustement aj ON aj.ajust_id = a.id" +
        " JOIN stock_produit sp ON aj.stock_produit_id = sp.id" +
        " JOIN produit p ON sp.produit_id = p.id" +
        " WHERE a.statut = 'CLOSED'" +
        "   AND aj.type_ajust = 'AJUSTEMENT_OUT'" +
        "   AND CAST(a.date_mtv AS date) BETWEEN :startDate AND :endDate";

    private static final String BY_MOTIF_SQL =
        "SELECT COALESCE(m.libelle, 'Sans motif') AS motif," +
        "  COUNT(*) AS nb_lignes," +
        "  COALESCE(SUM(ABS(aj.qty_mvt)), 0) AS total_qty," +
        "  COALESCE(SUM(ABS(aj.qty_mvt) * p.cost_amount), 0) AS valeur" +
        " FROM ajust a" +
        " JOIN ajustement aj ON aj.ajust_id = a.id" +
        " JOIN stock_produit sp ON aj.stock_produit_id = sp.id" +
        " JOIN produit p ON sp.produit_id = p.id" +
        " LEFT JOIN motif_ajustement m ON aj.motif_ajustement_id = m.id" +
        " WHERE a.statut = 'CLOSED'" +
        "   AND aj.type_ajust = 'AJUSTEMENT_OUT'" +
        "   AND CAST(a.date_mtv AS date) BETWEEN :startDate AND :endDate" +
        " GROUP BY m.id, m.libelle" +
        " ORDER BY valeur DESC";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public DemarqueKpiDTO getKpi(LocalDate startDate, LocalDate endDate) {
        Object[] row = (Object[]) entityManager.createNativeQuery(KPI_SQL)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getSingleResult();
        return new DemarqueKpiDTO(
            row[0] != null ? ((Number) row[0]).intValue()  : 0,
            row[1] != null ? ((Number) row[1]).longValue() : 0L,
            row[2] != null ? ((Number) row[2]).longValue() : 0L
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DemarqueByMotifDTO> getByMotif(LocalDate startDate, LocalDate endDate) {
        return ((List<Object[]>) entityManager.createNativeQuery(BY_MOTIF_SQL)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getResultList()).stream()
            .map(row -> new DemarqueByMotifDTO(
                (String) row[0],
                row[1] != null ? ((Number) row[1]).intValue()  : 0,
                row[2] != null ? ((Number) row[2]).longValue() : 0L,
                row[3] != null ? ((Number) row[3]).longValue() : 0L
            ))
            .toList();
    }
}
