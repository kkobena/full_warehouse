package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.CashMovementDTO;
import com.kobe.warehouse.service.dto.report.DailyCashRegisterReportDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CashRegisterReportServiceImpl implements CashRegisterReportService {

    private final EntityManager entityManager;

    public CashRegisterReportServiceImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    @Override
    @Cacheable(value = "cashRegisterReport", key = "#date.toString()")
    public List<DailyCashRegisterReportDTO> getDailyReport(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        String sql =
            "SELECT " +
                "cr.id as cash_register_id, " +
                "CONCAT('Caisse ', cr.id) as caisse_libelle, " +
                "cr.begin_time as opening_date, " +
                "cr.end_time as closing_date, " +
                "cr.init_amount as opening_balance, " +
                "cr.final_amount as closing_balance, " +
                "cr.statut, " +
                "u.first_name || ' ' || u.last_name as user_name, " +
                "COALESCE(SUM(cri.amount), 0) as total_sales, " +
                "COUNT(DISTINCT s.id) as number_of_transactions " +
                "FROM cash_register cr " +
                "INNER JOIN app_user u ON cr.user_id = u.id " +
                "LEFT JOIN cash_register_item cri ON cr.id = cri.cash_register_id " +
                "LEFT JOIN sales s ON s.sale_date = :date AND s.user_id = cr.user_id " +
                "WHERE cr.begin_time >= :startOfDay AND cr.begin_time <= :endOfDay " +
                "GROUP BY cr.id, cr.begin_time, cr.end_time, cr.init_amount, cr.final_amount, cr.statut, u.first_name, u.last_name " +
                "ORDER BY cr.begin_time";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("date", date);
        query.setParameter("startOfDay", startOfDay);
        query.setParameter("endOfDay", endOfDay);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<DailyCashRegisterReportDTO> reports = new ArrayList<>();

        for (Object[] row : results) {
            Integer cashRegisterId = (Integer) row[0];
            String caisseLibelle = (String) row[1];
            LocalDateTime openingDate = row[2] != null ? LocalDateTime.parse(row[2]+""): null;
            LocalDateTime closingDate = row[3] != null ?  LocalDateTime.parse(row[2]+"") : null;
            long openingBalance = row[4] != null ? ((Number) row[4]).longValue() : 0L;
            Long closingBalance = row[5] != null ? ((Number) row[5]).longValue() : null;
            String statutStr = (String) row[6];
            String userName = (String) row[7];
            long totalSales = row[8] != null ? ((Number) row[8]).longValue() : 0L;
            long numberOfTransactions = row[9] != null ? ((Number) row[9]).longValue() : 0L;

            boolean isClosed = "CLOSED".equals(statutStr);

            // Get payment mode breakdowns
            List<DailyCashRegisterReportDTO.PaymentModeBreakdown> paymentModeBreakdowns = getPaymentModeBreakdowns(
                cashRegisterId
            );

            // Calculate expected balance and discrepancy
            Integer expectedBalance = (int) openingBalance + (int) totalSales;
            Integer actualClosingBalance = closingBalance != null ? closingBalance.intValue() : 0;
            Integer discrepancy = isClosed ? (actualClosingBalance - expectedBalance) : 0;

            reports.add(
                new DailyCashRegisterReportDTO(
                    cashRegisterId,
                    caisseLibelle,
                    date,
                    openingDate,
                    closingDate,
                    (int) openingBalance,
                    actualClosingBalance,
                    expectedBalance,
                    discrepancy,
                    (int) totalSales,
                    (int) numberOfTransactions,
                    paymentModeBreakdowns,
                    userName,
                    isClosed
                )
            );
        }

        return reports;
    }

    private List<DailyCashRegisterReportDTO.PaymentModeBreakdown> getPaymentModeBreakdowns(Integer cashRegisterId) {
        String sql =
            "SELECT " +
                "pm.libelle as mode_paiement, " +
                "SUM(cri.amount) as amount, " +
                "COUNT(*) as count " +
                "FROM cash_register_item cri " +
                "INNER JOIN payment_mode pm ON cri.payment_mode_code = pm.code " +
                "WHERE cri.cash_register_id = :cashRegisterId " +
                "GROUP BY pm.libelle " +
                "ORDER BY amount DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("cashRegisterId", cashRegisterId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results
            .stream()
            .map(row -> {
                String modePaiement = (String) row[0];
                Integer amount = row[1] != null ? ((Number) row[1]).intValue() : 0;
                Integer count = row[2] != null ? ((Number) row[2]).intValue() : 0;
                return new DailyCashRegisterReportDTO.PaymentModeBreakdown(modePaiement, amount, count);
            })
            .toList();
    }

    @Override
    public List<CashMovementDTO> getCashMovements(LocalDate startDate, LocalDate endDate, Long userId, Long cashRegisterId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("s.id, ");
        sql.append("s.created as transaction_date, ");
        sql.append("CONCAT('Caisse ', cr.id) as cash_register_name, ");
        sql.append("u.first_name || ' ' || u.last_name as user_name, ");
        sql.append("'VENTE' as movement_type, ");
        sql.append("s.sales_amount as amount, ");
        sql.append("pm.libelle as payment_mode, ");
        sql.append("s.number_transaction as sale_number, ");
        sql.append("COALESCE(c.first_name || ' ' || c.last_name, 'Client comptant') as customer_name ");
        sql.append("FROM sales s ");
        sql.append("INNER JOIN app_user u ON s.user_id = u.id ");
        sql.append("LEFT JOIN cash_register cr ON s.sale_date = DATE(cr.begin_time) AND s.user_id = cr.user_id ");
        sql.append("LEFT JOIN payment p ON s.id = p.sales_id ");
        sql.append("LEFT JOIN payment_mode pm ON p.payment_mode_code = pm.code ");
        sql.append("LEFT JOIN customer c ON s.customer_id = c.id ");
        sql.append("WHERE s.sale_date BETWEEN :startDate AND :endDate ");
        sql.append("AND s.statut = 'CLOSED' ");

        if (userId != null) {
            sql.append("AND s.user_id = :userId ");
        }

        if (cashRegisterId != null) {
            sql.append("AND cr.id = :cashRegisterId ");
        }

        sql.append("ORDER BY s.created DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        if (userId != null) {
            query.setParameter("userId", userId);
        }

        if (cashRegisterId != null) {
            query.setParameter("cashRegisterId", cashRegisterId);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results
            .stream()
            .map(row -> {
                Long id = row[0] != null ? ((Number) row[0]).longValue() : null;
                LocalDate transactionDate = row[1] != null ? LocalDate.parse(row[1]+""): null;
                String cashRegisterName = (String) row[2];
                String userName = (String) row[3];
                String movementType = (String) row[4];
                Integer amount = row[5] != null ? ((Number) row[5]).intValue() : 0;
                String paymentMode = (String) row[6];
                String saleNumber = (String) row[7];
                String customerName = (String) row[8];

                return new CashMovementDTO(
                    id,
                    transactionDate,
                    cashRegisterName,
                    userName,
                    movementType,
                    amount,
                    paymentMode,
                    saleNumber,
                    customerName
                );
            })
            .toList();
    }

    @Override
    public List<DailyCashRegisterReportDTO> getCashRegisterSummary(LocalDate startDate, LocalDate endDate) {
        List<DailyCashRegisterReportDTO> summaries = new ArrayList<>();

        // Get reports for each day in the range
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            summaries.addAll(getDailyReport(currentDate));
            currentDate = currentDate.plusDays(1);
        }

        return summaries;
    }
}
