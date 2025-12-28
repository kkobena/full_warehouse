package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.StockAlertDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.sql.Date;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StockAlertReportServiceImpl implements StockAlertReportService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Cacheable(value = "stockAlerts", key = "#alertTypes != null ? #alertTypes.toString() : 'all'")
    public List<StockAlertDTO> getStockAlerts(List<StockAlertDTO.StockAlertType> alertTypes) {
        // Use materialized view for better performance
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("produit_id, ");
        sql.append("libelle, ");
        sql.append("code_cip, ");
        sql.append("stock_quantity, ");
        sql.append("seuil_min, ");
        sql.append("expiry_date, ");
        sql.append("alert_type ");
        sql.append("FROM mv_stock_alerts ");
        sql.append("WHERE 1=1 ");

        // Add alert type filter if provided
        if (alertTypes != null && !alertTypes.isEmpty()) {
            sql.append("AND alert_type IN (");
            sql.append(alertTypes.stream().map(t -> "'" + t.name() + "'").collect(Collectors.joining(",")));
            sql.append(") ");
        }

        sql.append("ORDER BY alert_type, libelle");
        var finalSql = sql.toString();
        Query query = entityManager.createNativeQuery(finalSql);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results
            .stream()
            .map(row -> {
                Integer produitId = row[0] != null ? ((Number) row[0]).intValue() : null;
                String libelle = (String) row[1];
                String codeCip = (String) row[2];
                Integer stockQuantity = row[3] != null ? ((Number) row[3]).intValue() : 0;
                Integer seuilMin = row[4] != null ? ((Number) row[4]).intValue() : 0;
                LocalDate expiryDate = row[5] != null ? LocalDate.parse(row[5].toString()): null;
                String alertTypeStr = (String) row[6];

                StockAlertDTO.StockAlertType alertType = alertTypeStr != null ? StockAlertDTO.StockAlertType.valueOf(alertTypeStr) : null;

                return new StockAlertDTO(produitId, libelle, codeCip, stockQuantity, seuilMin, expiryDate, alertType);
            })
            .toList();
    }

    @Override
    public Map<StockAlertDTO.StockAlertType, Long> getStockAlertsCount() {
        // Use materialized view for better performance
        String countQuery = "SELECT alert_type, COUNT(*) as count " + "FROM mv_stock_alerts " + "GROUP BY alert_type";

        Query query = entityManager.createNativeQuery(countQuery);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        Map<StockAlertDTO.StockAlertType, Long> counts = new EnumMap<>(StockAlertDTO.StockAlertType.class);

        // Initialize with zeros
        counts.put(StockAlertDTO.StockAlertType.RUPTURE, 0L);
        counts.put(StockAlertDTO.StockAlertType.ALERTE, 0L);
        counts.put(StockAlertDTO.StockAlertType.PEREMPTION, 0L);

        // Fill with actual counts
        for (Object[] row : results) {
            String alertTypeStr = (String) row[0];
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            try {
                StockAlertDTO.StockAlertType alertType = StockAlertDTO.StockAlertType.valueOf(alertTypeStr);
                counts.put(alertType, count);
            } catch (IllegalArgumentException _) {
                // Ignore invalid alert types
            }
        }

        return counts;
    }
}
