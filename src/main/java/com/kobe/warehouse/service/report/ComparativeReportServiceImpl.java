package com.kobe.warehouse.service.report;

import com.kobe.warehouse.repository.ComparativeReportRepository;
import com.kobe.warehouse.service.dto.report.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ComparativeReportServiceImpl implements ComparativeReportService {

    private final ComparativeReportRepository comparativeReportRepository;

    public ComparativeReportServiceImpl(ComparativeReportRepository comparativeReportRepository) {
        this.comparativeReportRepository = comparativeReportRepository;
    }

    @Override
    @Cacheable(value = "comparativeReports", key = "'monthly_' + #year")
    public List<ComparativeCADTO> getMonthlyComparison(Integer year) {
        return comparativeReportRepository
            .findMonthlyComparison(year, year - 1)
            .stream()
            .map(row -> {
                int month = ((Number) row[0]).intValue();
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
            .toList();
    }

    @Override
    @Cacheable(value = "comparativeReports", key = "'quarterly_' + #year")
    public List<ComparativeCADTO> getQuarterlyComparison(Integer year) {
        return comparativeReportRepository
            .findQuarterlyComparison(year, year - 1)
            .stream()
            .map(row -> {
                Integer quarter = ((Number) row[0]).intValue();
                Long currentCA = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                Long previousCA = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                Integer currentTrans = row[3] != null ? ((Number) row[3]).intValue() : 0;
                Integer previousTrans = row[4] != null ? ((Number) row[4]).intValue() : 0;

                int firstMonthOfQuarter = (quarter - 1) * 3 + 1;
                LocalDate period = LocalDate.of(year, firstMonthOfQuarter, 1);

                return new ComparativeCADTO(
                    period,
                    "T" + quarter + " " + year,
                    currentCA,
                    previousCA,
                    calculateEvolution(currentCA, previousCA),
                    BigDecimal.valueOf(currentCA - previousCA),
                    currentTrans,
                    previousTrans,
                    "QUARTERLY"
                );
            })
            .toList();
    }

    @Override
    @Cacheable(value = "comparativeReports", key = "'yearly_' + #startDate + '_' + #endDate")
    public List<ComparativeCADTO> getYearlyComparison(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = comparativeReportRepository.findYearlyComparison(startDate, endDate);

        List<ComparativeCADTO> comparisons = new ArrayList<>();
        for (int i = 1; i < results.size(); i++) {
            Object[] current = results.get(i);
            Object[] previous = results.get(i - 1);

            Integer year = ((Number) current[0]).intValue();
            Long currentCA = ((Number) current[1]).longValue();
            Integer currentTrans = ((Number) current[2]).intValue();
            Long previousCA = ((Number) previous[1]).longValue();
            Integer previousTrans = ((Number) previous[2]).intValue();

            comparisons.add(new ComparativeCADTO(
                LocalDate.of(year, 1, 1),
                String.valueOf(year),
                currentCA,
                previousCA,
                calculateEvolution(currentCA, previousCA),
                BigDecimal.valueOf(currentCA - previousCA),
                currentTrans,
                previousTrans,
                "YEARLY"
            ));
        }
        return comparisons;
    }

    @Override
    @Cacheable(value = "comparativeReports", key = "'by_type_' + #currentYear + '_' + #previousYear")
    public List<ComparativeByTypeDTO> getComparisonBySalesType(Integer currentYear, Integer previousYear) {
        return comparativeReportRepository
            .findComparisonBySalesType(currentYear, previousYear)
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
            .toList();
    }

    @Override
    @Cacheable(value = "comparativeReports", key = "'summary'")
    public ComparativeSummaryDTO getComparativeSummary() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();

        LocalDate ytdStart = LocalDate.of(currentYear, 1, 1);
        LocalDate previousYtdStart = LocalDate.of(currentYear - 1, 1, 1);
        LocalDate previousYtdEnd = LocalDate.of(currentYear - 1, today.getMonthValue(), today.getDayOfMonth());

        long ytdCurrent = comparativeReportRepository.getPeriodCA(ytdStart, today);
        long ytdPrevious = comparativeReportRepository.getPeriodCA(previousYtdStart, previousYtdEnd);

        LocalDate last12Start = today.minusMonths(12);
        LocalDate previous12Start = last12Start.minusYears(1);
        LocalDate previous12End = last12Start.minusDays(1);

        long last12CA = comparativeReportRepository.getPeriodCA(last12Start, today);
        long previous12CA = comparativeReportRepository.getPeriodCA(previous12Start, previous12End);

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

    @Override
    @Cacheable(value = "comparativeReports", key = "'by_family_' + #currentYear + '_' + #previousYear")
    public List<ComparativeByFamilyDTO> getComparisonByFamily(Integer currentYear, Integer previousYear) {
        return comparativeReportRepository
            .findComparisonByFamily(currentYear, previousYear)
            .stream()
            .map(row -> {
                Integer familleId     = row[0] != null ? ((Number) row[0]).intValue() : null;
                String familleLibelle = (String) row[1];
                Long currentCA        = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                Long previousCA       = row[3] != null ? ((Number) row[3]).longValue() : 0L;
                Integer currentCount  = row[4] != null ? ((Number) row[4]).intValue() : 0;
                Integer previousCount = row[5] != null ? ((Number) row[5]).intValue() : 0;
                return new ComparativeByFamilyDTO(
                    familleId,
                    familleLibelle,
                    currentCA,
                    previousCA,
                    calculateEvolution(currentCA, previousCA),
                    BigDecimal.valueOf(currentCA - previousCA),
                    currentCount,
                    previousCount
                );
            })
            .toList();
    }

    @Override
    @Cacheable(value = "comparativeReports", key = "'by_fournisseur_' + #currentYear + '_' + #previousYear")
    public List<ComparativeByFournisseurDTO> getComparisonByFournisseur(Integer currentYear, Integer previousYear) {
        return comparativeReportRepository
            .findComparisonByFournisseur(currentYear, previousYear)
            .stream()
            .map(row -> {
                Integer fournisseurId      = row[0] != null ? ((Number) row[0]).intValue() : null;
                String fournisseurLibelle  = (String) row[1];
                Long currentCA             = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                Long previousCA            = row[3] != null ? ((Number) row[3]).longValue() : 0L;
                Integer currentCount       = row[4] != null ? ((Number) row[4]).intValue() : 0;
                Integer previousCount      = row[5] != null ? ((Number) row[5]).intValue() : 0;
                return new ComparativeByFournisseurDTO(
                    fournisseurId,
                    fournisseurLibelle,
                    currentCA,
                    previousCA,
                    calculateEvolution(currentCA, previousCA),
                    BigDecimal.valueOf(currentCA - previousCA),
                    currentCount,
                    previousCount
                );
            })
            .toList();
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
            case "VNO" -> "VNO";
            case "VO" -> "VO";
            default -> type;
        };
    }
}
