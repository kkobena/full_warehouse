package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.repository.MobileSalesRepository;
import com.kobe.warehouse.repository.MobileSalesRepository.DailyCATrendProjection;
import com.kobe.warehouse.repository.MobileSalesRepository.DailySalesSummaryProjection;
import com.kobe.warehouse.repository.MobileSalesRepository.TopProductProjection;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO.DailyCASummaryDTO;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO.MobileAlertDTO;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO.TopProductDTO;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for mobile dashboard data aggregation.
 * Uses MobileSalesRepository for data access.
 */
@Service
@Transactional(readOnly = true)
public class MobileDashboardService {

    private static final Logger LOG = LoggerFactory.getLogger(MobileDashboardService.class);
    private static final int LOOKBACK_DAYS = 30;

    private final MobileAlertService alertService;
    private final MobileSalesRepository salesRepository;

    public MobileDashboardService(MobileAlertService alertService, MobileSalesRepository salesRepository) {
        this.alertService = alertService;
        this.salesRepository = salesRepository;
    }

    /**
     * Get complete dashboard data for a given date.
     *
     * @param date The date for which to get dashboard data
     * @return Complete dashboard DTO with all widgets data
     */
    public MobileDashboardDTO getDashboard(LocalDate date) {
        LOG.debug("Getting mobile dashboard for date: {}", date);

        // Get daily sales summary
        DailySalesSummaryProjection dailySummary = salesRepository.getDailySalesSummary(date);

        // Get previous day for variation calculation
        DailySalesSummaryProjection previousDaySummary = salesRepository.getDailySalesSummary(date.minusDays(1));

        // Calculate variation
        double variationPercent = calculateVariation(dailySummary.caTotal(), previousDaySummary.caTotal());

        // Moyenne glissante des 30 derniers jours — référence contextuelle
        long averageCA30j = salesRepository.getAverageCA(date, LOOKBACK_DAYS);

        // Écart % entre le CA du jour et la moyenne glissante
        double trendVs30j = calculateVariation(dailySummary.caTotal(), averageCA30j);

        // Get alerts summary
        List<MobileAlertDTO> alerts = alertService.getAlertsSummary();

        // Get top products for the day
        List<TopProductDTO> topProducts = mapTopProducts(salesRepository.getTopProducts(date, 5));

        // Get CA trend for last 7 days
        List<DailyCASummaryDTO> caTrend = mapCATrend(salesRepository.getCATrend(date.minusDays(6), date));

        return new MobileDashboardDTO(
            dailySummary.caTotal(),
            averageCA30j,
            variationPercent,
            trendVs30j,
            dailySummary.transactionsCount(),
            dailySummary.averageBasket(),
            dailySummary.customersCount(),
            dailySummary.amountCollected(),
            dailySummary.amountCredit(),
            dailySummary.marginAmount(),
            dailySummary.marginPercent(),
            alerts,
            alerts.stream().mapToInt(MobileAlertDTO::count).sum(),
            topProducts,
            caTrend,
            date,
            LocalDateTime.now()
        );
    }

    /**
     * Map top product projections to DTOs.
     */
    private List<TopProductDTO> mapTopProducts(List<TopProductProjection> projections) {
        return projections.stream()
            .map(p -> new TopProductDTO(
                p.productId(),
                p.productName(),
                p.codeCip(),
                p.salesAmount(),
                p.quantitySold(),
                p.rank()
            ))
            .toList();
    }

    /**
     * Map CA trend projections to DTOs.
     */
    private List<DailyCASummaryDTO> mapCATrend(List<DailyCATrendProjection> projections) {
        return projections.stream()
            .map(p -> new DailyCASummaryDTO(
                p.date(),
                getDayLabel(p.date().getDayOfWeek()),
                p.caTotal(),
                p.transactionsCount()
            ))
            .toList();
    }

    private double calculateVariation(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return Math.round(((current - previous) * 100.0) / previous * 100) / 100.0;
    }

    private String getDayLabel(DayOfWeek dayOfWeek) {
        return dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.FRENCH);
    }
}
