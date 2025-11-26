package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
@Transactional(readOnly = true)
public class ComparativeReportServiceImpl implements ComparativeReportService {

    private final EntityManager entityManager;

    public ComparativeReportServiceImpl(EntityManager entityManager) {
        this.entityManager = entityManager;

    }

    @Override
    @Cacheable(value = "comparativeReports", key = "'monthly_' + #year")
    public List<ComparativeCADTO> getMonthlyComparison(Integer year) {
        String sql =
            "WITH current_year AS (" +
            "  SELECT " +
            "    EXTRACT(MONTH FROM DATE(s.updated_at)) as month, " +
            "    SUM(s.sales_amount - s.discount_amount) as ca, " +
            "    COUNT(DISTINCT s.id) as nb_trans " +
            "  FROM sales s " +
            "  WHERE s.statut = 'CLOSED' " +
            "    AND s.canceled = false " +
            "    AND s.ca = 'CA' " +
            "    AND EXTRACT(YEAR FROM DATE(s.updated_at)) = :currentYear " +
            "  GROUP BY month " +
            "), " +
            "previous_year AS (" +
            "  SELECT " +
            "    EXTRACT(MONTH FROM DATE(s.updated_at)) as month, " +
            "    SUM(s.sales_amount - s.discount_amount) as ca, " +
            "    COUNT(DISTINCT s.id) as nb_trans " +
            "  FROM sales s " +
            "  WHERE s.statut = 'CLOSED' " +
            "    AND s.canceled = false " +
            "    AND s.ca = 'CA' " +
            "    AND EXTRACT(YEAR FROM DATE(s.updated_at)) = :previousYear " +
            "  GROUP BY month " +
            ") " +
            "SELECT " +
            "  cy.month, " +
            "  COALESCE(cy.ca, 0) as current_ca, " +
            "  COALESCE(py.ca, 0) as previous_ca, " +
            "  COALESCE(cy.nb_trans, 0) as current_trans, " +
            "  COALESCE(py.nb_trans, 0) as previous_trans " +
            "FROM current_year cy " +
            "LEFT JOIN previous_year py ON cy.month = py.month " +
            "ORDER BY cy.month";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("currentYear", year);
        query.setParameter("previousYear", year - 1);

        List<Object[]> results = query.getResultList();
        return results
            .stream()
            .map(row -> {
                Integer month = ((Number) row[0]).intValue();
                Long currentCA = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                Long previousCA = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                Integer currentTrans = row[3] != null ? ((Number) row[3]).intValue() : 0;
                Integer previousTrans = row[4] != null ? ((Number) row[4]).intValue() : 0;

                LocalDate period = LocalDate.of(year, month, 1);
                String periodLabel = period.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " + year;

                return new ComparativeCADTO(
                    period,
                    periodLabel,
                    currentCA,
                    previousCA,
                    calculateEvolution(currentCA, previousCA),
                    BigDecimal.valueOf(currentCA - previousCA),
                    currentTrans,
                    previousTrans,
                    "MONTHLY"
                );
            })
            .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "comparativeReports", key = "'quarterly_' + #year")
    public List<ComparativeCADTO> getQuarterlyComparison(Integer year) {
        String sql =
            "WITH current_year AS (" +
            "  SELECT " +
            "    EXTRACT(QUARTER FROM DATE(s.updated_at)) as quarter, " +
            "    SUM(s.sales_amount - s.discount_amount) as ca, " +
            "    COUNT(DISTINCT s.id) as nb_trans " +
            "  FROM sales s " +
            "  WHERE s.statut = 'CLOSED' " +
            "    AND s.canceled = false " +
            "    AND s.ca = 'CA' " +
            "    AND EXTRACT(YEAR FROM DATE(s.updated_at)) = :currentYear " +
            "  GROUP BY quarter " +
            "), " +
            "previous_year AS (" +
            "  SELECT " +
            "    EXTRACT(QUARTER FROM DATE(s.updated_at)) as quarter, " +
            "    SUM(s.sales_amount - s.discount_amount) as ca, " +
            "    COUNT(DISTINCT s.id) as nb_trans " +
            "  FROM sales s " +
            "  WHERE s.statut = 'CLOSED' " +
            "    AND s.canceled = false " +
            "    AND s.ca = 'CA' " +
            "    AND EXTRACT(YEAR FROM DATE(s.updated_at)) = :previousYear " +
            "  GROUP BY quarter " +
            ") " +
            "SELECT " +
            "  cy.quarter, " +
            "  COALESCE(cy.ca, 0) as current_ca, " +
            "  COALESCE(py.ca, 0) as previous_ca, " +
            "  COALESCE(cy.nb_trans, 0) as current_trans, " +
            "  COALESCE(py.nb_trans, 0) as previous_trans " +
            "FROM current_year cy " +
            "LEFT JOIN previous_year py ON cy.quarter = py.quarter " +
            "ORDER BY cy.quarter";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("currentYear", year);
        query.setParameter("previousYear", year - 1);

        List<Object[]> results = query.getResultList();
        return results
            .stream()
            .map(row -> {
                Integer quarter = ((Number) row[0]).intValue();
                Long currentCA = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                Long previousCA = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                Integer currentTrans = row[3] != null ? ((Number) row[3]).intValue() : 0;
                Integer previousTrans = row[4] != null ? ((Number) row[4]).intValue() : 0;

                // First month of the quarter
                int firstMonthOfQuarter = (quarter - 1) * 3 + 1;
                LocalDate period = LocalDate.of(year, firstMonthOfQuarter, 1);
                String periodLabel = "T" + quarter + " " + year;

                return new ComparativeCADTO(
                    period,
                    periodLabel,
                    currentCA,
                    previousCA,
                    calculateEvolution(currentCA, previousCA),
                    BigDecimal.valueOf(currentCA - previousCA),
                    currentTrans,
                    previousTrans,
                    "QUARTERLY"
                );
            })
            .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "comparativeReports", key = "'yearly_' + #startDate + '_' + #endDate")
    public List<ComparativeCADTO> getYearlyComparison(LocalDate startDate, LocalDate endDate) {
        String sql =
            "SELECT " +
            "  EXTRACT(YEAR FROM DATE(s.sale_date)) as year, " +
            "  SUM(s.sales_amount - s.discount_amount) as ca, " +
            "  COUNT(DISTINCT s.id) as nb_trans " +
            "FROM sales s " +
            "WHERE s.statut = 'CLOSED' " +
            "  AND s.canceled = false " +
            "  AND s.ca = 'CA' " +
            "  AND DATE(s.sale_date) BETWEEN :startDate AND :endDate " +
            "GROUP BY year " +
            "ORDER BY year";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        List<Object[]> results = query.getResultList();

        // Pair consecutive years for comparison
        List<ComparativeCADTO> comparisons = new ArrayList<>();
        for (int i = 1; i < results.size(); i++) {
            Object[] current = results.get(i);
            Object[] previous = results.get(i - 1);

            Integer year = ((Number) current[0]).intValue();
            Long currentCA = ((Number) current[1]).longValue();
            Integer currentTrans = ((Number) current[2]).intValue();

            Long previousCA = ((Number) previous[1]).longValue();
            Integer previousTrans = ((Number) previous[2]).intValue();

            LocalDate period = LocalDate.of(year, 1, 1);
            String periodLabel = String.valueOf(year);

            comparisons.add(
                new ComparativeCADTO(
                    period,
                    periodLabel,
                    currentCA,
                    previousCA,
                    calculateEvolution(currentCA, previousCA),
                    BigDecimal.valueOf(currentCA - previousCA),
                    currentTrans,
                    previousTrans,
                    "YEARLY"
                )
            );
        }
        return comparisons;
    }

    @Override
    @Cacheable(value = "comparativeReports", key = "'by_type_' + #currentYear + '_' + #previousYear")
    public List<ComparativeByTypeDTO> getComparisonBySalesType(Integer currentYear, Integer previousYear) {
        String sql =
            "WITH current_year AS (" +
            "  SELECT " +
            "    s.nature_vente as type, " +
            "    SUM(s.sales_amount - s.discount_amount) as ca, " +
            "    COUNT(DISTINCT s.id) as count " +
            "  FROM sales s " +
            "  WHERE s.statut = 'CLOSED' " +
            "    AND s.canceled = false " +
            "    AND s.ca = 'CA' " +
            "    AND EXTRACT(YEAR FROM DATE(s.sale_date)) = :currentYear " +
            "  GROUP BY s.nature_vente " +
            "), " +
            "previous_year AS (" +
            "  SELECT " +
            "    s.nature_vente as type, " +
            "    SUM(s.sales_amount - s.discount_amount) as ca, " +
            "    COUNT(DISTINCT s.id) as count " +
            "  FROM sales s " +
            "  WHERE s.statut = 'CLOSED' " +
            "    AND s.canceled = false " +
            "    AND s.ca = 'CA' " +
            "    AND EXTRACT(YEAR FROM DATE(s.sale_date)) = :previousYear " +
            "  GROUP BY s.nature_vente " +
            ") " +
            "SELECT " +
            "  COALESCE(cy.type, py.type) as type, " +
            "  COALESCE(cy.ca, 0) as current_ca, " +
            "  COALESCE(py.ca, 0) as previous_ca, " +
            "  COALESCE(cy.count, 0) as current_count, " +
            "  COALESCE(py.count, 0) as previous_count " +
            "FROM current_year cy " +
            "FULL OUTER JOIN previous_year py ON cy.type = py.type " +
            "ORDER BY COALESCE(cy.ca, 0) DESC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("currentYear", currentYear);
        query.setParameter("previousYear", previousYear);

        List<Object[]> results = query.getResultList();
        return results
            .stream()
            .map(row -> {
                String type = (String) row[0];
                Long currentCA = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                Long previousCA = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                Integer currentCount = row[3] != null ? ((Number) row[3]).intValue() : 0;
                Integer previousCount = row[4] != null ? ((Number) row[4]).intValue() : 0;

                return new ComparativeByTypeDTO(
                    type,
                    getSalesTypeLabel(type),
                    currentCA,
                    previousCA,
                    calculateEvolution(currentCA, previousCA),
                    currentCount,
                    previousCount
                );
            })
            .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "comparativeReports", key = "'summary'")
    public ComparativeSummaryDTO getComparativeSummary() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();

        // Year to date
        LocalDate ytdStart = LocalDate.of(currentYear, 1, 1);
        LocalDate previousYtdStart = LocalDate.of(currentYear - 1, 1, 1);
        LocalDate previousYtdEnd = LocalDate.of(currentYear - 1, today.getMonthValue(), today.getDayOfMonth());

        long ytdCurrent = getPeriodCA(ytdStart, today);
        long ytdPrevious = getPeriodCA(previousYtdStart, previousYtdEnd);

        // Last 12 months
        LocalDate last12Start = today.minusMonths(12);
        LocalDate previous12Start = last12Start.minusYears(1);
        LocalDate previous12End = last12Start.minusDays(1);

        long last12CA = getPeriodCA(last12Start, today);
        long previous12CA = getPeriodCA(previous12Start, previous12End);

        // Best and worst months in current year
        List<ComparativeCADTO> monthlyData = getMonthlyComparison(currentYear);
        ComparativeCADTO bestMonth = monthlyData
            .stream()
            .max(Comparator.comparingLong(ComparativeCADTO::currentCA))
            .orElse(null);
        ComparativeCADTO worstMonth = monthlyData
            .stream()
            .filter(m -> m.currentCA() > 0)
            .min(Comparator.comparingLong(ComparativeCADTO::currentCA))
            .orElse(null);

        // Average monthly CA
        BigDecimal avgMonthlyCA = monthlyData.isEmpty()
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(monthlyData.stream().mapToLong(ComparativeCADTO::currentCA).average().orElse(0));

        BigDecimal avgMonthlyEvolution = monthlyData.isEmpty()
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(
                monthlyData.stream().mapToDouble(m -> m.evolutionPct().doubleValue()).average().orElse(0)
            );

        return new ComparativeSummaryDTO(
            ytdCurrent,
            ytdPrevious,
            calculateEvolution(ytdCurrent, ytdPrevious),
            last12CA,
            previous12CA,
            calculateEvolution(last12CA, previous12CA),
            bestMonth != null ? bestMonth.periodLabel() : "-",
            bestMonth != null ? bestMonth.currentCA() : 0L,
            worstMonth != null ? worstMonth.periodLabel() : "-",
            worstMonth != null ? worstMonth.currentCA() : 0L,
            avgMonthlyCA,
            avgMonthlyEvolution
        );
    }


    // Helper methods

    private long getPeriodCA(LocalDate start, LocalDate end) {
        String sql =
            "SELECT COALESCE(SUM(s.sales_amount - s.discount_amount), 0) " +
            "FROM sales s " +
            "WHERE s.statut = 'CLOSED' " +
            "  AND s.canceled = false " +
            "  AND s.ca = 'CA' " +
            "  AND DATE(s.updated_at) BETWEEN :start AND :end";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("start", start);
        query.setParameter("end", end);

        return ((Number) query.getSingleResult()).longValue();
    }

    private BigDecimal calculateEvolution(Long current, Long previous) {
        if (previous == null || previous == 0) {
            return current != null && current > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        if (current == null) {
            return BigDecimal.valueOf(-100);
        }
        return BigDecimal.valueOf((current - previous) * 100.0 / previous).setScale(2, RoundingMode.HALF_UP);
    }

    private String getSalesTypeLabel(String type) {
        return switch (type) {
            case "VNO" -> "Vente Normale Officine";
            case "VO" -> "Vente Ordonnance";
            case "VA" -> "Vente Assurance";
            case "VE" -> "Vente Entrepôt";
            default -> type;
        };
    }
}
