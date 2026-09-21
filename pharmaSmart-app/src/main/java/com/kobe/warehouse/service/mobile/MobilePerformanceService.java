package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.repository.MobilePerformanceRepository;
import com.kobe.warehouse.repository.MobilePerformanceRepository.DataPointProjection;
import com.kobe.warehouse.repository.MobilePerformanceRepository.PaymentMethodProjection;
import com.kobe.warehouse.repository.MobilePerformanceRepository.PeriodSummaryProjection;
import com.kobe.warehouse.repository.MobilePerformanceRepository.TopProductProjection;
import com.kobe.warehouse.service.dto.mobile.MobilePerformanceDTO;
import com.kobe.warehouse.service.dto.mobile.MobilePerformanceDTO.PaymentMethodSummaryDTO;
import com.kobe.warehouse.service.dto.mobile.MobilePerformanceDTO.PeriodDataPointDTO;
import com.kobe.warehouse.service.dto.mobile.MobilePerformanceDTO.TopProductPerformanceDTO;
import com.kobe.warehouse.service.dto.mobile.PaymentMethodColor;
import com.kobe.warehouse.service.dto.mobile.PerformancePeriod;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for mobile performance reports (Phase 2).
 * Uses MobilePerformanceRepository for data access.
 */
@Service
@Transactional(readOnly = true)
public class MobilePerformanceService {

    private static final Logger LOG = LoggerFactory.getLogger(MobilePerformanceService.class);
    private static final int TOP_PRODUCTS_LIMIT = 10;

    private final MobilePerformanceRepository performanceRepository;

    public MobilePerformanceService(MobilePerformanceRepository performanceRepository) {
        this.performanceRepository = performanceRepository;
    }

    /**
     * Get performance data for a period.
     *
     * @param period WEEK, MONTH, or YEAR
     * @param referenceDate Reference date for period calculation
     * @return Performance data DTO
     */
    public MobilePerformanceDTO getPerformance(String period, LocalDate referenceDate) {
        LOG.debug("Getting performance for period: {} from date: {}", period, referenceDate);

        PerformancePeriod performancePeriod = PerformancePeriod.fromString(period);

        LocalDate startDate = performancePeriod.getStartDate(referenceDate);
        LocalDate endDate = performancePeriod.getEndDate(referenceDate);
        LocalDate previousStartDate = performancePeriod.getPreviousStartDate(referenceDate);
        LocalDate previousEndDate = performancePeriod.getPreviousEndDate(referenceDate);
        LocalDate lastYearStartDate = performancePeriod.getSamePeriodLastYearStartDate(referenceDate);
        LocalDate lastYearEndDate = performancePeriod.getSamePeriodLastYearEndDate(referenceDate);

        // Get current and previous period summaries from repository
        PeriodSummaryProjection currentSummary = performanceRepository.getPeriodSummary(startDate, endDate);
        PeriodSummaryProjection previousSummary = performanceRepository.getPeriodSummary(previousStartDate, previousEndDate);
        PeriodSummaryProjection lastYearSummary = performanceRepository.getPeriodSummary(lastYearStartDate, lastYearEndDate);

        // Calculate derived values
        long averageBasket = currentSummary.transactionsCount() > 0
            ? currentSummary.caTotal() / currentSummary.transactionsCount()
            : 0;

        double marginPercent = currentSummary.caTotal() > 0
            ? BigDecimal.valueOf((currentSummary.marginTotal() * 100.0) / currentSummary.caTotal())
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue()
            : 0;

        Double variationPercent = calculateVariation(currentSummary.caTotal(), previousSummary.caTotal());
        Double variationVsLastYearPercent = calculateVariation(currentSummary.caTotal(), lastYearSummary.caTotal());

        // Get payment methods breakdown from repository
        List<PaymentMethodProjection> paymentProjections = performanceRepository.getPaymentMethodsSummary(startDate, endDate);
        List<PaymentMethodSummaryDTO> paymentMethods = mapPaymentMethods(paymentProjections, currentSummary.caTotal());

        // Get top products from repository
        List<TopProductProjection> topProductProjections = performanceRepository.getTopProducts(
            startDate, endDate, previousStartDate, previousEndDate, TOP_PRODUCTS_LIMIT
        );
        List<TopProductPerformanceDTO> topProducts = mapTopProducts(topProductProjections);

        // Get data points for chart from repository
        List<DataPointProjection> dataPointProjections = performanceRepository.getDataPoints(startDate, endDate, performancePeriod);
        List<PeriodDataPointDTO> dataPoints = mapDataPoints(dataPointProjections, performancePeriod);

        return MobilePerformanceDTO.builder()
            .period(performancePeriod.getCode())
            .startDate(startDate)
            .endDate(endDate)
            .caTotal(currentSummary.caTotal())
            .caPreviousPeriod(previousSummary.caTotal())
            .variationPercent(variationPercent)
            .previousStartDate(previousStartDate)
            .previousEndDate(previousEndDate)
            .caSamePeriodLastYear(lastYearSummary.caTotal())
            .variationVsLastYearPercent(variationVsLastYearPercent)
            .transactionsCount(currentSummary.transactionsCount())
            .averageBasket(averageBasket)
            .customersCount(currentSummary.customersCount())
            .marginTotal(currentSummary.marginTotal())
            .marginPercent(marginPercent)
            .paymentMethods(paymentMethods)
            .topProducts(topProducts)
            .dataPoints(dataPoints)
            .build();
    }

    /**
     * Get performance data for a period using enum directly.
     *
     * @param period PerformancePeriod enum
     * @param referenceDate Reference date for period calculation
     * @return Performance data DTO
     */
    public MobilePerformanceDTO getPerformance(PerformancePeriod period, LocalDate referenceDate) {
        return getPerformance(period.getCode(), referenceDate);
    }

    /**
     * Map payment method projections to DTOs.
     */
    private List<PaymentMethodSummaryDTO> mapPaymentMethods(List<PaymentMethodProjection> projections, long totalCA) {
        return projections.stream()
            .map(p -> {
                double percent = totalCA > 0
                    ? BigDecimal.valueOf((p.amount() * 100.0) / totalCA)
                        .setScale(1, RoundingMode.HALF_UP)
                        .doubleValue()
                    : 0;
                String color = PaymentMethodColor.getColorForCode(p.code());
                return new PaymentMethodSummaryDTO(p.code(), p.libelle(), p.amount(), percent, p.transactionsCount(), color);
            })
            .toList();
    }

    /**
     * Map top product projections to DTOs.
     */
    private List<TopProductPerformanceDTO> mapTopProducts(List<TopProductProjection> projections) {
        AtomicInteger rank = new AtomicInteger(1);
        return projections.stream()
            .map(p -> new TopProductPerformanceDTO(
                rank.getAndIncrement(),
                p.productId(),
                p.productName(),
                p.codeCip(),
                p.salesAmount(),
                p.quantitySold(),
                BigDecimal.valueOf(p.percentOfTotal()).setScale(1, RoundingMode.HALF_UP).doubleValue(),
                BigDecimal.valueOf(p.variationPercent()).setScale(1, RoundingMode.HALF_UP).doubleValue()
            ))
            .toList();
    }

    /**
     * Map data point projections to DTOs.
     */
    private List<PeriodDataPointDTO> mapDataPoints(List<DataPointProjection> projections, PerformancePeriod period) {
        return projections.stream()
            .map(p -> new PeriodDataPointDTO(
                p.date(),
                getLabel(p.date(), period),
                p.caAmount(),
                p.transactionsCount(),
                p.marginAmount()
            ))
            .toList();
    }

    /**
     * Get label for a date based on period type.
     */
    private String getLabel(LocalDate date, PerformancePeriod period) {
        return switch (period.getLabelFormat()) {
            case "day" -> date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
            case "month" -> date.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
            default -> date.toString();
        };
    }

    /**
     * Variation, en pourcentage, entre le chiffre courant et celui de la période de référence.
     *
     * <p>Rend {@code null} quand la période de référence est vide : il n'y a alors pas de variation à
     * calculer, et le pourcentage n'existe pas. Le code rendait {@code 100} dès que la référence
     * était à zéro — une officine ouverte depuis six mois lisait donc « +100 % vs l'an passé » tout
     * au long de sa première année, au même endroit et de la même couleur qu'une vraie croissance.
     */
    private Double calculateVariation(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? Double.valueOf(0) : null;
        }
        return BigDecimal.valueOf(((current - previous) * 100.0) / previous)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }
}
