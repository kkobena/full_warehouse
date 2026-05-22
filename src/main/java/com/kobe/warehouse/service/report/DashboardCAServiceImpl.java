package com.kobe.warehouse.service.report;

import com.kobe.warehouse.repository.DashboardCARepository;
import com.kobe.warehouse.service.dto.FinancesSummaryDTO;
import com.kobe.warehouse.service.dto.dashboard.PerformanceVendeurDTO;
import com.kobe.warehouse.service.dto.report.BasketEvolutionDTO;
import com.kobe.warehouse.service.dto.report.DailyCADTO;
import com.kobe.warehouse.service.dto.report.DashboardCAEvolutionDTO;
import com.kobe.warehouse.service.dto.report.DashboardCASummaryDTO;
import com.kobe.warehouse.service.dto.report.GenericsSubstitutionDTO;
import com.kobe.warehouse.service.dto.report.RemisesAnalysisKpiDTO;
import com.kobe.warehouse.service.dto.report.TopRemiseProduitDTO;
import com.kobe.warehouse.service.dto.report.PaymentMethodCADTO;
import com.kobe.warehouse.service.dto.report.ProductFamilyCADTO;
import com.kobe.warehouse.service.dto.report.TopProductDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardCAServiceImpl implements DashboardCAService {

    private final DashboardCARepository dashboardCARepository;

    public DashboardCAServiceImpl(DashboardCARepository dashboardCARepository) {
        this.dashboardCARepository = dashboardCARepository;
    }

    @Override
    @Cacheable(value = "dashboardCA", key = "'daily_' + #startDate + '_' + #endDate")
    public List<DailyCADTO> getDailySummary(LocalDate startDate, LocalDate endDate) {
        return dashboardCARepository.findDailySummary(startDate, endDate)
            .stream()
            .map(row -> new DailyCADTO(
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
            ))
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

        Object[] todayRow      = dashboardCARepository.getPeriodAggregation(today, today);
        Object[] yesterdayRow  = dashboardCARepository.getPeriodAggregation(yesterday, yesterday);
        Object[] weekRow       = dashboardCARepository.getPeriodAggregation(weekStart, today);
        Object[] lastWeekRow   = dashboardCARepository.getPeriodAggregation(lastWeekStart, lastWeekEnd);
        Object[] monthRow      = dashboardCARepository.getPeriodAggregation(monthStart, today);
        Object[] lastMonthRow  = dashboardCARepository.getPeriodAggregation(lastMonthStart, lastMonthEnd);
        Object[] yearRow       = dashboardCARepository.getPeriodAggregation(yearStart, today);
        Object[] lastYearRow   = dashboardCARepository.getPeriodAggregation(lastYearStart, lastYearEnd);

        return new DashboardCASummaryDTO(
            toL(todayRow[0]),    toL(yesterdayRow[0]),  evo(toL(todayRow[0]),   toL(yesterdayRow[0])),
            toL(weekRow[0]),     toL(lastWeekRow[0]),   evo(toL(weekRow[0]),    toL(lastWeekRow[0])),
            toL(monthRow[0]),    toL(lastMonthRow[0]),  evo(toL(monthRow[0]),   toL(lastMonthRow[0])),
            toL(yearRow[0]),     toL(lastYearRow[0]),   evo(toL(yearRow[0]),    toL(lastYearRow[0])),
            toI(todayRow[1]),    toI(weekRow[1]),        toI(monthRow[1]),       toI(yearRow[1]),
            toBD(todayRow[2]),   toBD(weekRow[2]),       toBD(monthRow[2]),      toBD(yearRow[2]),
            toBD(todayRow[3]),   toBD(weekRow[3]),       toBD(monthRow[3]),      toBD(yearRow[3])
        );
    }

    @Override
    @Cacheable(value = "dashboardCA", key = "'evolution_' + #period + '_' + #startDate + '_' + #endDate")
    public DashboardCAEvolutionDTO getEvolutionData(String period, LocalDate startDate, LocalDate endDate) {
        List<DailyCADTO> dailyData = getDailySummary(startDate, endDate);

        if ("monthly".equals(period)) {
            var byMonth = dailyData.stream().collect(Collectors.groupingBy(
                d -> d.saleDate().getYear() + "-" + String.format("%02d", d.saleDate().getMonthValue())));
            List<String> labels = new ArrayList<>(byMonth.keySet());
            Collections.sort(labels);
            return new DashboardCAEvolutionDTO(
                labels,
                labels.stream().map(m -> byMonth.get(m).stream().mapToLong(DailyCADTO::caNet).sum()).toList(),
                Collections.emptyList(),
                labels.stream().map(m -> byMonth.get(m).stream().mapToInt(DailyCADTO::nbTransactions).sum()).toList(),
                "monthly"
            );
        } else if ("weekly".equals(period)) {
            var byWeek = dailyData.stream().collect(Collectors.groupingBy(
                d -> d.saleDate().getYear() + "-W" + String.format("%02d", getWeekNumber(d.saleDate()))));
            List<String> labels = new ArrayList<>(byWeek.keySet());
            Collections.sort(labels);
            return new DashboardCAEvolutionDTO(
                labels,
                labels.stream().map(w -> byWeek.get(w).stream().mapToLong(DailyCADTO::caNet).sum()).toList(),
                Collections.emptyList(),
                labels.stream().map(w -> byWeek.get(w).stream().mapToInt(DailyCADTO::nbTransactions).sum()).toList(),
                "weekly"
            );
        } else {
            return new DashboardCAEvolutionDTO(
                dailyData.stream().map(d -> d.saleDate().toString()).toList(),
                dailyData.stream().map(DailyCADTO::caNet).toList(),
                Collections.emptyList(),
                dailyData.stream().map(DailyCADTO::nbTransactions).toList(),
                "daily"
            );
        }
    }

    @Override
    @Cacheable(value = "dashboardCA", key = "'payment_' + #startDate + '_' + #endDate")
    public List<PaymentMethodCADTO> getPaymentMethodDistribution(LocalDate startDate, LocalDate endDate) {
        return dashboardCARepository.findPaymentMethodDistribution(startDate, endDate)
            .stream()
            .map(row -> new PaymentMethodCADTO(
                LocalDate.parse(row[0].toString().substring(0, 10)),
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).intValue(),
                ((Number) row[4]).longValue(),
                (BigDecimal) row[5]
            ))
            .toList();
    }

    @Override
    @Cacheable(value = "dashboardCA", key = "'families_' + #startDate + '_' + #endDate")
    public List<ProductFamilyCADTO> getProductFamilyDistribution(LocalDate startDate, LocalDate endDate) {
        return dashboardCARepository.findProductFamilyDistribution(startDate, endDate)
            .stream()
            .map(row -> new ProductFamilyCADTO(
                LocalDate.parse(row[0].toString().substring(0, 10)),
                (String) row[1],
                ((Number) row[2]).intValue(),
                ((Number) row[3]).longValue(),
                ((Number) row[4]).longValue(),
                ((Number) row[5]).longValue(),
                (BigDecimal) row[6],
                ((Number) row[7]).intValue()
            ))
            .toList();
    }

    @Override
    @Cacheable(value = "dashboardCA", key = "'top_products_' + #startDate + '_' + #endDate + '_' + #limit")
    public List<TopProductDTO> getTopProducts(LocalDate startDate, LocalDate endDate, Integer limit) {
        return dashboardCARepository.findTopProducts(startDate, endDate, Objects.requireNonNullElse(limit, 10))
            .stream()
            .map(row -> new TopProductDTO(
                LocalDate.parse(row[0].toString().substring(0, 10)),
                ((Number) row[1]).intValue(),
                (String) row[2],
                (String) row[3],
                ((Number) row[4]).longValue(),
                ((Number) row[5]).intValue(),
                ((Number) row[6]).intValue(),
                (BigDecimal) row[7]
            ))
            .toList();
    }

    @Override
    @Cacheable(value = "dashboardCA", key = "'basket_evolution'")
    public BasketEvolutionDTO getBasketEvolution() {
        List<Object[]> rows = dashboardCARepository.findBasketEvolutionMonthly();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();

        for (Object[] row : rows) {
            int yr = ((Number) row[0]).intValue();
            int mo = ((Number) row[1]).intValue();
            BigDecimal basket = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            labels.add(YearMonth.of(yr, mo).atDay(1).format(fmt));
            values.add(basket);
        }

        BigDecimal currentValue  = values.isEmpty() ? BigDecimal.ZERO : values.get(values.size() - 1);
        BigDecimal previousValue = values.size() > 1 ? values.get(values.size() - 2) : BigDecimal.ZERO;
        BigDecimal evolutionAmount = currentValue.subtract(previousValue);
        BigDecimal evolutionPct = BigDecimal.ZERO;
        if (previousValue.compareTo(BigDecimal.ZERO) != 0) {
            evolutionPct = evolutionAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(previousValue, 2, RoundingMode.HALF_UP);
        }

        String bestLabel    = "";
        BigDecimal bestValue = BigDecimal.ZERO;
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).compareTo(bestValue) > 0) {
                bestValue = values.get(i);
                bestLabel = labels.get(i);
            }
        }

        BigDecimal trend6MPct = BigDecimal.ZERO;
        if (values.size() >= 6) {
            int n = values.size();
            BigDecimal recentAvg = values.subList(n - 3, n).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            BigDecimal olderAvg = values.subList(n - 6, n - 3).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            if (olderAvg.compareTo(BigDecimal.ZERO) != 0) {
                trend6MPct = recentAvg.subtract(olderAvg)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(olderAvg, 2, RoundingMode.HALF_UP);
            }
        }

        return new BasketEvolutionDTO(
            labels, values,
            currentValue, previousValue,
            evolutionPct, evolutionAmount,
            bestLabel, bestValue,
            trend6MPct
        );
    }

    @Override
    public FinancesSummaryDTO getSummaryFinances() {
        return new FinancesSummaryDTO(
            dashboardCARepository.getDetteFournisseur(),
            dashboardCARepository.getCreanceTiersPayant(),
            dashboardCARepository.getNbEcheancesEnRetard(),
            dashboardCARepository.getNbFacturesImpayees()
        );
    }

    @Override
    @Transactional
    @CacheEvict(value = "dashboardCA", allEntries = true)
    public void refreshViews() {
        dashboardCARepository.refreshViews();
    }

    @Override
    public List<PerformanceVendeurDTO> getSalesByStaff(LocalDate startDate, LocalDate endDate) {
        return dashboardCARepository.findSalesByStaff(startDate, endDate).stream()
            .map(row -> new PerformanceVendeurDTO(
                ((Number) row[0]).longValue(),
                (String) row[1],
                ((Number) row[2]).intValue(),
                ((Number) row[3]).longValue(),
                ((Number) row[4]).longValue(),
                row[5] != null ? ((Number) row[5]).doubleValue() : 0.0
            ))
            .toList();
    }

    @Override
    public GenericsSubstitutionDTO getGenericsSubstitution(LocalDate startDate, LocalDate endDate) {
        Object[] row = dashboardCARepository.findGenericsSubstitutionStats(startDate, endDate);
        return new GenericsSubstitutionDTO(
            toL(row[0]),
            toL(row[1]),
            toL(row[2]),
            toL(row[3]),
            toL(row[4]),
            toL(row[5])
        );
    }

    @Override
    public RemisesAnalysisKpiDTO getRemisesKpi(LocalDate startDate, LocalDate endDate) {
        Object[] row = dashboardCARepository.findRemisesKpi(startDate, endDate);
        return new RemisesAnalysisKpiDTO(
            toL(row[0]),
            toL(row[1]),
            row[2] != null ? ((Number) row[2]).doubleValue() : 0.0,
            toI(row[3]),
            toI(row[4])
        );
    }

    @Override
    public List<TopRemiseProduitDTO> getRemisesTopProducts(LocalDate startDate, LocalDate endDate, int limit) {
        return dashboardCARepository.findRemisesTopProducts(startDate, endDate, limit).stream()
            .map(row -> new TopRemiseProduitDTO(
                (String) row[0],
                toL(row[1]),
                toI(row[2])
            ))
            .toList();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private BigDecimal evo(Long current, Long previous) {
        if (previous == null || previous == 0) return BigDecimal.ZERO;
        if (current == null) return BigDecimal.valueOf(-100);
        return BigDecimal.valueOf((current - previous) * 100.0 / previous).setScale(2, RoundingMode.HALF_UP);
    }

    private static Long     toL(Object o)  { return o != null ? ((Number) o).longValue()    : 0L; }
    private static Integer  toI(Object o)  { return o != null ? ((Number) o).intValue()     : 0;  }
    private static BigDecimal toBD(Object o) { return o instanceof BigDecimal bd ? bd : BigDecimal.ZERO; }

    private int getWeekNumber(LocalDate date) {
        int dayOfYear = date.getDayOfYear();
        int dayOfWeek = date.getDayOfWeek().getValue();
        return (dayOfYear - dayOfWeek + 10) / 7;
    }
}
