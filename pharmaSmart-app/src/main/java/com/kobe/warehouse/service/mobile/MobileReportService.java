package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.repository.MobileSalesRepository;
import com.kobe.warehouse.repository.MobileSalesRepository.DailySalesSummaryProjection;
import com.kobe.warehouse.repository.MobileSalesRepository.UserSalesSummaryProjection;
import com.kobe.warehouse.service.dto.mobile.DailyDigestDTO;
import com.kobe.warehouse.service.dto.mobile.MobileDashboardDTO;
import com.kobe.warehouse.service.dto.mobile.UserPerformanceDTO;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for generating mobile report data for notifications and analytics.
 * Uses MobileSalesRepository for data access.
 */
@Service
@Transactional(readOnly = true)
public class MobileReportService {

    private static final Logger LOG = LoggerFactory.getLogger(MobileReportService.class);
    private static final int LOOKBACK_DAYS = 30;

    private final MobileAlertService alertService;
    private final MobileSalesRepository salesRepository;

    public MobileReportService(MobileAlertService alertService, MobileSalesRepository salesRepository) {
        this.alertService = alertService;
        this.salesRepository = salesRepository;
    }

    /**
     * Generate daily digest for managers.
     * Contains overall pharmacy performance for the day.
     *
     * @param date Date for which to generate the digest
     * @return Daily digest DTO
     */
    public DailyDigestDTO generateDailyDigest(LocalDate date) {
        LOG.debug("Generating daily digest for date: {}", date);

        // Get current day summary
        DailySalesSummaryProjection currentDay = salesRepository.getDailySalesSummary(date);

        // Get previous day summary for variation
        DailySalesSummaryProjection previousDay = salesRepository.getDailySalesSummary(date.minusDays(1));

        // Calculate variation
        double variation = calculateVariation(currentDay.caTotal(), previousDay.caTotal());

        // Moyenne glissante des 30 derniers jours — référence contextuelle pour l'officine
        long averageCA30j = salesRepository.getAverageCA(date, LOOKBACK_DAYS);

        // Écart entre le CA du jour et la moyenne glissante
        double trendVs30j = calculateVariation(currentDay.caTotal(), averageCA30j);

        // Get alerts count
        int alertsCount = alertService.getAlertsSummary()
            .stream()
            .mapToInt(MobileDashboardDTO.MobileAlertDTO::count)
            .sum();

        return new DailyDigestDTO(
            currentDay.caTotal(),
            variation,
            currentDay.transactionsCount(),
            alertsCount,
            averageCA30j,
            trendVs30j,
            currentDay.customersCount(),
            currentDay.averageBasket()
        );
    }

    /**
     * Get user performance for a specific user and date.
     * Used for individual seller notifications.
     *
     * @param userId User ID
     * @param date Date for which to get performance
     * @return User performance DTO
     */
    public UserPerformanceDTO getUserPerformance(Integer userId, LocalDate date) {
        LOG.debug("Getting user performance for user {} on date {}", userId, date);

        // Get user sales summary for current day
        UserSalesSummaryProjection currentDay = salesRepository.getUserSalesSummary(userId, date);

        // Get previous day for variation
        UserSalesSummaryProjection previousDay = salesRepository.getUserSalesSummary(userId, date.minusDays(1));

        // Calculate variation
        double variation = calculateVariation(currentDay.totalCA(), previousDay.totalCA());

        // Get user name
        String userName = salesRepository.getUserName(userId);

        UserPerformanceDTO performance = new UserPerformanceDTO(
            userId.longValue(),
            userName,
            currentDay.totalCA(),
            currentDay.salesCount(),
            currentDay.averageBasket(),
            variation
        );

        // Set margin data
        performance.setMarginAmount(currentDay.marginAmount());
        performance.setMarginPercent(currentDay.marginPercent());

        return performance;
    }


    /**
     * Calculate variation percentage between current and previous value.
     */
    private double calculateVariation(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return Math.round(((current - previous) * 100.0) / previous * 100) / 100.0;
    }
}
