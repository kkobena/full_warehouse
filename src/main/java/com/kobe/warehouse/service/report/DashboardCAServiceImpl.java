package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.DailyCADTO;
import com.kobe.warehouse.service.dto.report.DashboardCAEvolutionDTO;
import com.kobe.warehouse.service.dto.report.DashboardCASummaryDTO;
import com.kobe.warehouse.service.dto.report.PaymentMethodCADTO;
import com.kobe.warehouse.service.dto.report.ProductFamilyCADTO;
import com.kobe.warehouse.service.dto.report.TopProductDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service implementation for Dashboard Chiffre d'Affaires (CA)
 */
@Service
@Transactional(readOnly = true)
public class DashboardCAServiceImpl implements DashboardCAService {

    private final EntityManager entityManager;

    public DashboardCAServiceImpl(EntityManager entityManager) {
        this.entityManager = entityManager;

    }

    @Override
    @Cacheable(value = "dashboardCA", key = "'daily_' + #startDate + '_' + #endDate")
    public List<DailyCADTO> getDailySummary(LocalDate startDate, LocalDate endDate) {
        String sql =
            "SELECT sale_date, nb_transactions,  ca_total,  ca_net, " +
                "panier_moyen, cout_total, marge_brute, taux_marge_pct, nb_clients, " +
                "montant_encaisse, montant_credit " +
                "FROM mv_dashboard_ca_daily " +
                "WHERE sale_date BETWEEN :startDate AND :endDate " +
                "ORDER BY sale_date DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        List<Object[]> results = query.getResultList();
        return results
            .stream()
            .map(row ->
                new DailyCADTO(
                    LocalDate.parse(row[0].toString().substring(0, 10)),
                    ((Number) row[1]).intValue(),
                    ((Number) row[2]).longValue(),
                    ((Number) row[3]).longValue(),
                    (BigDecimal) row[4],
                    ((Number) row[5]).longValue(),
                    ((Number) row[6]).longValue(),
                    (BigDecimal) row[7],
                    ((Number) row[8]).intValue(),
                    ((Number) row[9]).longValue(),
                    ((Number) row[10]).longValue()
                )
            )
            .toList();
    }

    @Override
    @Cacheable(value = "dashboardCA", key = "'summary'")
    public DashboardCASummaryDTO getOverallSummary() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate lastWeekStart = weekStart.minusDays(7);
        LocalDate lastWeekEnd = weekStart.minusDays(1);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = monthStart.minusMonths(1);
        LocalDate lastMonthEnd = monthStart.minusDays(1);
        LocalDate yearStart = today.withDayOfYear(1);
        LocalDate lastYearStart = yearStart.minusYears(1);
        LocalDate lastYearEnd = yearStart.minusDays(1);

        // Today
        Map<String, Object> todayData = getPeriodData(today, today);
        Map<String, Object> yesterdayData = getPeriodData(yesterday, yesterday);

        // Week
        Map<String, Object> weekData = getPeriodData(weekStart, today);
        Map<String, Object> lastWeekData = getPeriodData(lastWeekStart, lastWeekEnd);

        // Month
        Map<String, Object> monthData = getPeriodData(monthStart, today);
        Map<String, Object> lastMonthData = getPeriodData(lastMonthStart, lastMonthEnd);

        // Year
        Map<String, Object> yearData = getPeriodData(yearStart, today);
        Map<String, Object> lastYearData = getPeriodData(lastYearStart, lastYearEnd);

        return new DashboardCASummaryDTO(
            // Today
            (Long) todayData.get("ca"),
            (Long) yesterdayData.get("ca"),
            calculateEvolution((Long) todayData.get("ca"), (Long) yesterdayData.get("ca")),
            // Week
            (Long) weekData.get("ca"),
            (Long) lastWeekData.get("ca"),
            calculateEvolution((Long) weekData.get("ca"), (Long) lastWeekData.get("ca")),
            // Month
            (Long) monthData.get("ca"),
            (Long) lastMonthData.get("ca"),
            calculateEvolution((Long) monthData.get("ca"), (Long) lastMonthData.get("ca")),
            // Year
            (Long) yearData.get("ca"),
            (Long) lastYearData.get("ca"),
            calculateEvolution((Long) yearData.get("ca"), (Long) lastYearData.get("ca")),
            // Transaction counts
            (Integer) todayData.get("nbTransactions"),
            (Integer) weekData.get("nbTransactions"),
            (Integer) monthData.get("nbTransactions"),
            (Integer) yearData.get("nbTransactions"),
            // Average basket
            (BigDecimal) todayData.get("panierMoyen"),
            (BigDecimal) weekData.get("panierMoyen"),
            (BigDecimal) monthData.get("panierMoyen"),
            (BigDecimal) yearData.get("panierMoyen"),
            // Margin rate
            (BigDecimal) todayData.get("tauxMarge"),
            (BigDecimal) weekData.get("tauxMarge"),
            (BigDecimal) monthData.get("tauxMarge"),
            (BigDecimal) yearData.get("tauxMarge")
        );
    }

    @Override
    @Cacheable(value = "dashboardCA", key = "'evolution_' + #period + '_' + #startDate + '_' + #endDate")
    public DashboardCAEvolutionDTO getEvolutionData(String period, LocalDate startDate, LocalDate endDate) {
        List<DailyCADTO> dailyData = getDailySummary(startDate, endDate);

        if ("monthly".equals(period)) {
            // Group by month
            Map<String, List<DailyCADTO>> byMonth = dailyData
                .stream()
                .collect(Collectors.groupingBy(d -> d.saleDate().getYear() + "-" + String.format("%02d", d.saleDate().getMonthValue())));

            List<String> labels = new ArrayList<>(byMonth.keySet());
            Collections.sort(labels);

            List<Long> caValues = labels
                .stream()
                .map(month -> byMonth.get(month).stream().mapToLong(DailyCADTO::caNet).sum())
                .toList();

            List<Integer> transactionCounts = labels
                .stream()
                .map(month -> byMonth.get(month).stream().mapToInt(DailyCADTO::nbTransactions).sum())
                .toList();

            return new DashboardCAEvolutionDTO(labels, caValues, Collections.emptyList(), transactionCounts, "monthly");
        } else if ("weekly".equals(period)) {
            // Group by week
            Map<String, List<DailyCADTO>> byWeek = dailyData
                .stream()
                .collect(
                    Collectors.groupingBy(d -> d.saleDate().getYear() + "-W" + String.format("%02d", getWeekNumber(d.saleDate())))
                );

            List<String> labels = new ArrayList<>(byWeek.keySet());
            Collections.sort(labels);

            List<Long> caValues = labels
                .stream()
                .map(week -> byWeek.get(week).stream().mapToLong(DailyCADTO::caNet).sum())
                .toList();

            List<Integer> transactionCounts = labels
                .stream()
                .map(week -> byWeek.get(week).stream().mapToInt(DailyCADTO::nbTransactions).sum())
                .toList();

            return new DashboardCAEvolutionDTO(labels, caValues, Collections.emptyList(), transactionCounts, "weekly");
        } else {
            // Daily
            List<String> labels = dailyData.stream().map(d -> d.saleDate().toString()).toList();

            List<Long> caValues = dailyData.stream().map(DailyCADTO::caNet).toList();

            List<Integer> transactionCounts = dailyData.stream().map(DailyCADTO::nbTransactions).toList();

            return new DashboardCAEvolutionDTO(labels, caValues, Collections.emptyList(), transactionCounts, "daily");
        }
    }

    @Override
    @Cacheable(value = "dashboardCA", key = "'payment_' + #startDate + '_' + #endDate")
    public List<PaymentMethodCADTO> getPaymentMethodDistribution(LocalDate startDate, LocalDate endDate) {
        String sql =
            "SELECT payment_date, payment_method, payment_code, nb_payments, " +
                "montant_total,  montant_moyen " +
                "FROM mv_dashboard_ca_payment_methods " +
                "WHERE payment_date BETWEEN :startDate AND :endDate " +
                "ORDER BY montant_total DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        List<Object[]> results = query.getResultList();
        return results
            .stream()
            .map(row ->
                new PaymentMethodCADTO(
                    LocalDate.parse(row[0].toString().substring(0, 10)),
                    (String) row[1],
                    (String) row[2],
                    ((Number) row[3]).intValue(),
                    ((Number) row[4]).longValue(),
                    (BigDecimal) row[5]
                )
            )
            .toList();
    }

    @Override
    @Cacheable(value = "dashboardCA", key = "'families_' + #startDate + '_' + #endDate")
    public List<ProductFamilyCADTO> getProductFamilyDistribution(LocalDate startDate, LocalDate endDate) {
        String sql =
            "SELECT sale_date, famille, quantite_vendue, ca_total, cout_total, " +
                "marge_brute, taux_marge_pct, nb_lignes_vente " +
                "FROM mv_dashboard_ca_product_families " +
                "WHERE sale_date BETWEEN :startDate AND :endDate " +
                "ORDER BY ca_total DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        List<Object[]> results = query.getResultList();
        return results
            .stream()
            .map(row ->
                new ProductFamilyCADTO(
                    LocalDate.parse(row[0].toString().substring(0, 10)),
                    (String) row[1],
                    ((Number) row[2]).intValue(),
                    ((Number) row[3]).longValue(),
                    ((Number) row[4]).longValue(),
                    ((Number) row[5]).longValue(),
                    (BigDecimal) row[6],
                    ((Number)  row[7]).intValue()
                )
            )
            .toList();
    }

    @Override
    @Cacheable(value = "dashboardCA", key = "'top_products_' + #startDate + '_' + #endDate + '_' + #limit")
    public List<TopProductDTO> getTopProducts(LocalDate startDate, LocalDate endDate, Integer limit) {
        String sql =
            "SELECT :period as mois, p.id, p.libelle, " +
                "COALESCE(fp.code_cip, '') as code_cip, " +
                "COUNT(DISTINCT sl.sales_id) as nb_ventes, " +
                "SUM(sl.quantity_sold) as qty_vendue, " +
                "SUM(sl.sales_amount) as ca, " +
                "AVG(sl.sales_amount / NULLIF(sl.quantity_sold, 0)) as prix_moyen " +
                "FROM sales s " +
                "INNER JOIN sales_line sl ON s.id = sl.sales_id " +
                "INNER JOIN produit p ON sl.produit_id = p.id " +
                "LEFT JOIN fournisseur_produit fp ON p.fournisseur_produit_principal_id = fp.id " +
                "WHERE s.statut='CLOSED' " +
                "AND s.canceled = false " +
                "AND s.ca = 'CA' " +
                "AND s.sale_date BETWEEN :startDate AND :endDate " +
                "GROUP BY p.id, p.libelle, fp.code_cip " +
                "ORDER BY ca DESC ";


        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("period", startDate);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setMaxResults(Objects.requireNonNullElse(limit, 10));


        List<Object[]> results = query.getResultList();
        return results
            .stream()
            .map(row ->
                new TopProductDTO(
                    LocalDate.parse(row[0].toString().substring(0, 10)),
                    ((Number) row[1]).intValue(),
                    (String) row[2],
                    (String) row[3],
                    ((Number) row[4]).longValue(),
                    ((Number) row[5]).intValue(),
                    ((Number) row[6]).intValue(),
                    (BigDecimal) row[7]
                )
            )
            .toList();
    }


    @Override
    @Transactional
    @CacheEvict(value = "dashboardCA", allEntries = true)
    public void refreshViews() {
        entityManager.createNativeQuery("SELECT refresh_dashboard_ca_views()").executeUpdate();
    }

    // Helper methods

    private Map<String, Object> getPeriodData(LocalDate startDate, LocalDate endDate) {
        String sql =
            "SELECT COALESCE(SUM(ca_net), 0) as ca, " +
                "COALESCE(SUM(nb_transactions), 0) as nb_trans, " +
                "COALESCE(AVG(panier_moyen), 0) as panier_moyen, " +
                "COALESCE(AVG(taux_marge_pct), 0) as taux_marge " +
                "FROM mv_dashboard_ca_daily " +
                "WHERE sale_date BETWEEN :startDate AND :endDate";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        Object[] result = (Object[]) query.getSingleResult();

        Map<String, Object> data = new HashMap<>();
        data.put("ca", ((Number) result[0]).longValue());
        data.put("nbTransactions", ((Number) result[1]).intValue());
        data.put("panierMoyen", result[2]);
        data.put("tauxMarge", result[3]);

        return data;
    }

    private BigDecimal calculateEvolution(Long current, Long previous) {
        if (previous == null || previous == 0) {
            return BigDecimal.ZERO;
        }
        if (current == null) {
            return BigDecimal.valueOf(-100);
        }
        return BigDecimal
            .valueOf((current - previous) * 100.0 / previous)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private int getWeekNumber(LocalDate date) {
        // Simple week number calculation (ISO week)
        int dayOfYear = date.getDayOfYear();
        int dayOfWeek = date.getDayOfWeek().getValue();
        return (dayOfYear - dayOfWeek + 10) / 7;
    }

}
