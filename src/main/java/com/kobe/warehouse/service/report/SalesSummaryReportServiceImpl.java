package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.DailySalesSummaryDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SalesSummaryReportServiceImpl implements SalesSummaryReportService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Cacheable(value = "dailySalesReport", key = "#startDate.toString() + '-' + #endDate.toString()")
    public List<DailySalesSummaryDTO> getDailySalesSummary(LocalDate startDate, LocalDate endDate) {
        String sql =
            "SELECT " +
            "sale_date, " +
            "type_vente, " +
            "nb_ventes, " +
            "ca_total, " +
            "ca_net, " +
            "panier_moyen, " +
            "total_remises " +
            "FROM mv_daily_sales_summary " +
            "WHERE sale_date >= :startDate AND sale_date <= :endDate " +
            "ORDER BY sale_date DESC, type_vente";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results
            .stream()
            .map(row -> {
                LocalDate saleDate = row[0] != null ? ((Date) row[0]).toLocalDate() : null;
                String typeVente = (String) row[1];
                Long nbVentes = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                Integer caTotal = row[3] != null ? ((Number) row[3]).intValue() : 0;
                Integer caNet = row[4] != null ? ((Number) row[4]).intValue() : 0;
                BigDecimal panierMoyen = row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO;
                Integer totalRemises = row[6] != null ? ((Number) row[6]).intValue() : 0;

                return new DailySalesSummaryDTO(saleDate, typeVente, nbVentes, caTotal, caNet, panierMoyen, totalRemises);
            })
            .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "dailySalesReport", key = "#date.toString()")
    public List<DailySalesSummaryDTO> getDailySalesSummaryByDate(LocalDate date) {
        String sql =
            "SELECT " +
            "sale_date, " +
            "type_vente, " +
            "nb_ventes, " +
            "ca_total, " +
            "ca_net, " +
            "panier_moyen, " +
            "total_remises " +
            "FROM mv_daily_sales_summary " +
            "WHERE sale_date = :date " +
            "ORDER BY type_vente";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("date", date);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results
            .stream()
            .map(row -> {
                LocalDate saleDate = row[0] != null ? ((Date) row[0]).toLocalDate() : null;
                String typeVente = (String) row[1];
                Long nbVentes = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                Integer caTotal = row[3] != null ? ((Number) row[3]).intValue() : 0;
                Integer caNet = row[4] != null ? ((Number) row[4]).intValue() : 0;
                BigDecimal panierMoyen = row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO;
                Integer totalRemises = row[6] != null ? ((Number) row[6]).intValue() : 0;

                return new DailySalesSummaryDTO(saleDate, typeVente, nbVentes, caTotal, caNet, panierMoyen, totalRemises);
            })
            .collect(Collectors.toList());
    }

    @Override
    @Cacheable(
        value = "dailySalesReport",
        key = "#startDate.toString() + '-' + #endDate.toString() + '-' + #typeVente"
    )
    public List<DailySalesSummaryDTO> getDailySalesSummaryByType(
        LocalDate startDate,
        LocalDate endDate,
        String typeVente
    ) {
        String sql =
            "SELECT " +
            "sale_date, " +
            "type_vente, " +
            "nb_ventes, " +
            "ca_total, " +
            "ca_net, " +
            "panier_moyen, " +
            "total_remises " +
            "FROM mv_daily_sales_summary " +
            "WHERE sale_date >= :startDate AND sale_date <= :endDate " +
            "AND type_vente = :typeVente " +
            "ORDER BY sale_date DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("typeVente", typeVente);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results
            .stream()
            .map(row -> {
                LocalDate saleDate = row[0] != null ? ((Date) row[0]).toLocalDate() : null;
                String typeVenteStr = (String) row[1];
                Long nbVentes = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                Integer caTotal = row[3] != null ? ((Number) row[3]).intValue() : 0;
                Integer caNet = row[4] != null ? ((Number) row[4]).intValue() : 0;
                BigDecimal panierMoyen = row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO;
                Integer totalRemises = row[6] != null ? ((Number) row[6]).intValue() : 0;

                return new DailySalesSummaryDTO(saleDate, typeVenteStr, nbVentes, caTotal, caNet, panierMoyen, totalRemises);
            })
            .collect(Collectors.toList());
    }
}
