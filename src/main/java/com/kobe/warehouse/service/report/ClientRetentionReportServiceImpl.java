package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.ClientRetentionKpiDTO;
import com.kobe.warehouse.service.dto.report.ClientRetentionRowDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClientRetentionReportServiceImpl implements ClientRetentionReportService {

    private static final String KPI_SQL =
        "WITH last_purchase AS (" +
        "  SELECT s.customer_id," +
        "    MAX(s.sale_date) AS derniere_visite" +
        "  FROM sales s" +
        "  WHERE s.statut = 'CLOSED' AND s.canceled = false AND s.ca = 'CA'" +
        "    AND s.customer_id IS NOT NULL" +
        "  GROUP BY s.customer_id" +
        ")," +
        "client_ca AS (" +
        "  SELECT s.customer_id," +
        "    COALESCE(SUM(sl.sales_amount), 0) AS ca_total" +
        "  FROM sales s" +
        "  JOIN sales_line sl ON sl.sales_id = s.id" +
        "  WHERE s.statut = 'CLOSED' AND s.canceled = false AND s.ca = 'CA'" +
        "    AND s.customer_id IS NOT NULL" +
        "  GROUP BY s.customer_id" +
        ")" +
        "SELECT" +
        "  COUNT(*)                                                                        AS total_clients," +
        "  COUNT(CASE WHEN CURRENT_DATE - derniere_visite <= 30  THEN 1 END)              AS clients_actifs," +
        "  COUNT(CASE WHEN CURRENT_DATE - derniere_visite BETWEEN 31 AND 90 THEN 1 END)   AS clients_risque," +
        "  COUNT(CASE WHEN CURRENT_DATE - derniere_visite > 90   THEN 1 END)              AS clients_perdus," +
        "  COALESCE(ROUND(AVG(cc.ca_total)), 0)                                           AS ca_moyen" +
        " FROM last_purchase lp" +
        " JOIN client_ca cc ON cc.customer_id = lp.customer_id";

    private static final String LIST_SQL =
        "SELECT c.id," +
        "  COALESCE(NULLIF(TRIM(CONCAT(c.first_name, ' ', c.last_name)), ''), c.first_name) AS nom," +
        "  MIN(s.sale_date)                                   AS premiere_visite," +
        "  MAX(s.sale_date)                                   AS derniere_visite," +
        "  COUNT(DISTINCT s.id)                               AS nb_achats," +
        "  COALESCE(SUM(sl.sales_amount), 0)                  AS ca_total," +
        "  (CURRENT_DATE - MAX(s.sale_date))                  AS jours_absence" +
        " FROM customer c" +
        " JOIN sales s ON s.customer_id = c.id" +
        " JOIN sales_line sl ON sl.sales_id = s.id" +
        " WHERE s.statut = 'CLOSED' AND s.canceled = false AND s.ca = 'CA'" +
        " GROUP BY c.id, c.first_name, c.last_name" +
        " ORDER BY jours_absence DESC";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ClientRetentionKpiDTO getKpi() {
        Object[] row = (Object[]) entityManager.createNativeQuery(KPI_SQL).getSingleResult();
        return new ClientRetentionKpiDTO(
            toL(row[0]),
            toL(row[1]),
            toL(row[2]),
            toL(row[3]),
            row[4] instanceof BigDecimal bd ? bd.longValue() : toL(row[4])
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ClientRetentionRowDTO> getClientList(int limit) {
        return ((List<Object[]>) entityManager.createNativeQuery(LIST_SQL)
            .setMaxResults(limit)
            .getResultList()).stream()
            .map(row -> new ClientRetentionRowDTO(
                toL(row[0]),
                (String) row[1],
                toDate(row[2]),
                toDate(row[3]),
                toI(row[4]),
                toL(row[5]),
                toI(row[6])
            ))
            .toList();
    }

    private static long toL(Object o)        { return o != null ? ((Number) o).longValue()  : 0L; }
    private static int  toI(Object o)        { return o != null ? ((Number) o).intValue()   : 0;  }
    private static LocalDate toDate(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate ld) return ld;
        return LocalDate.parse(o.toString().substring(0, 10));
    }
}
