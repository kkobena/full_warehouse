package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.ForecastSummaryDTO;
import com.kobe.warehouse.service.dto.report.SalesForecastDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SalesForecastServiceImpl implements SalesForecastService {

    private final EntityManager entityManager;

    public SalesForecastServiceImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Cacheable(value = "salesForecast", key = "'forecast_' + #monthsAhead + '_' + #method")
    public List<SalesForecastDTO> getForecast(Integer monthsAhead, String method) {
        // Get historical data (last 24 months)
        Map<YearMonth, Long> historicalData = getHistoricalMonthlyCA(24);

        List<SalesForecastDTO> forecasts;

        switch (method.toUpperCase()) {
            case "MOVING_AVERAGE":
                forecasts = forecastMovingAverage(historicalData, monthsAhead);
                break;
            case "SEASONAL":
                forecasts = forecastSeasonal(historicalData, monthsAhead);
                break;
            case "LINEAR_REGRESSION":
            default:
                forecasts = forecastLinearRegression(historicalData, monthsAhead);
                break;
        }

        return forecasts;
    }

    @Override
    @Cacheable(value = "salesForecast", key = "'summary'")
    public ForecastSummaryDTO getForecastSummary() {
        Map<YearMonth, Long> historicalData = getHistoricalMonthlyCA(24);

        // Get forecasts for different periods
        List<SalesForecastDTO> forecast3M = forecastLinearRegression(historicalData, 3);
        List<SalesForecastDTO> forecast6M = forecastLinearRegression(historicalData, 6);
        List<SalesForecastDTO> forecast12M = forecastLinearRegression(historicalData, 12);

        long totalForecast3M = forecast3M.stream().mapToLong(SalesForecastDTO::forecastedCA).sum();
        long totalForecast6M = forecast6M.stream().mapToLong(SalesForecastDTO::forecastedCA).sum();
        long totalForecast12M = forecast12M.stream().mapToLong(SalesForecastDTO::forecastedCA).sum();

        // Calculate growth trends
        List<Long> monthlyValues = new ArrayList<>(historicalData.values());
        BigDecimal avgMonthlyGrowth = calculateAverageGrowth(monthlyValues);

        // Model accuracy (based on last 6 months)
        BigDecimal accuracy = calculateModelAccuracy(historicalData);

        // Seasonality detection
        Boolean seasonalityDetected = detectSeasonality();
        Map.Entry<YearMonth, Long> peakMonth = historicalData.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);
        Map.Entry<YearMonth, Long> lowMonth = historicalData.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .orElse(null);

        return new ForecastSummaryDTO(
            totalForecast3M,
            totalForecast6M,
            totalForecast12M,
            avgMonthlyGrowth,
            avgMonthlyGrowth.multiply(BigDecimal.valueOf(12)),
            accuracy,
            BigDecimal.ZERO, // MAE placeholder
            seasonalityDetected,
            peakMonth != null ? peakMonth.getKey().getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) : "",
            lowMonth != null ? lowMonth.getKey().getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) : "",
            "LINEAR_REGRESSION",
            historicalData.size()
        );
    }

    @Override
    public List<SalesForecastDTO> getHistoricalVsForecast(LocalDate startDate, LocalDate endDate, Integer monthsAhead) {
        Map<YearMonth, Long> historicalData = getHistoricalMonthlyCA(24);
        List<SalesForecastDTO> forecast = forecastLinearRegression(historicalData, monthsAhead);

        // Combine historical and forecast
        List<SalesForecastDTO> combined = new ArrayList<>();

        // Add historical data
        YearMonth start = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);
        YearMonth current = start;

        while (!current.isAfter(end)) {
            Long ca = historicalData.get(current);
            if (ca != null) {
                combined.add(new SalesForecastDTO(
                    current.atDay(1),
                    current.toString(),
                    ca, // forecast = actual for historical
                    ca,
                    BigDecimal.valueOf(100),
                    ca,
                    ca,
                    "HISTORICAL"
                ));
            }
            current = current.plusMonths(1);
        }

        // Add forecast
        combined.addAll(forecast);

        return combined;
    }

    @Override
    public Boolean detectSeasonality() {
        Map<YearMonth, Long> historicalData = getHistoricalMonthlyCA(24);

        if (historicalData.size() < 12) {
            return false;
        }

        // Group by month (across years)
        Map<Integer, List<Long>> byMonth = new HashMap<>();
        historicalData.forEach((yearMonth, ca) -> {
            byMonth.computeIfAbsent(yearMonth.getMonthValue(), k -> new ArrayList<>()).add(ca);
        });

        // Calculate coefficient of variation for each month
        double totalCV = 0;
        int monthsWithData = 0;

        for (List<Long> monthData : byMonth.values()) {
            if (monthData.size() >= 2) {
                double avg = monthData.stream().mapToLong(Long::longValue).average().orElse(0);
                double stdDev = calculateStdDev(monthData, avg);
                double cv = avg > 0 ? (stdDev / avg) : 0;
                totalCV += cv;
                monthsWithData++;
            }
        }

        // If average CV is low, there's seasonality (similar patterns each year)
        double avgCV = monthsWithData > 0 ? totalCV / monthsWithData : 1.0;
        return avgCV < 0.3; // Threshold for seasonality
    }

    // ========== PRIVATE HELPER METHODS ==========

    private Map<YearMonth, Long> getHistoricalMonthlyCA(int monthsBack) {
        LocalDate endDate = LocalDate.now().minusMonths(1); // Last complete month
        LocalDate startDate = endDate.minusMonths(monthsBack);

        String sql =
            "SELECT " +
            "  DATE_TRUNC('month', DATE(s.updated_at)) as month, " +
            "  SUM(s.sales_amount - s.discount_amount) as ca " +
            "FROM sales s " +
            "WHERE s.statut = 'CLOSED' " +
            "  AND s.canceled = false " +
            "  AND s.ca = 'CA' " +
            "  AND DATE(s.updated_at) BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE_TRUNC('month', DATE(s.updated_at)) " +
            "ORDER BY month";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        Map<YearMonth, Long> data = new LinkedHashMap<>();
        for (Object[] row : results) {
            LocalDate monthDate = ((Date) row[0]).toLocalDate();
            Long ca = ((Number) row[1]).longValue();
            data.put(YearMonth.from(monthDate), ca);
        }

        return data;
    }

    private List<SalesForecastDTO> forecastLinearRegression(Map<YearMonth, Long> historicalData, Integer monthsAhead) {
        List<YearMonth> months = new ArrayList<>(historicalData.keySet());
        List<Long> values = new ArrayList<>(historicalData.values());

        int n = months.size();
        if (n < 3) {
            return Collections.emptyList(); // Not enough data
        }

        // Simple linear regression: y = mx + b
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = values.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // Calculate standard error for confidence intervals
        double stdError = calculateStandardError(values, slope, intercept);

        // Generate forecasts
        List<SalesForecastDTO> forecasts = new ArrayList<>();
        YearMonth lastMonth = months.get(n - 1);

        for (int i = 1; i <= monthsAhead; i++) {
            YearMonth forecastMonth = lastMonth.plusMonths(i);
            double x = n + i - 1;
            long forecast = Math.round(slope * x + intercept);
            long lowerBound = Math.round(forecast - 1.96 * stdError); // 95% confidence
            long upperBound = Math.round(forecast + 1.96 * stdError);

            forecasts.add(new SalesForecastDTO(
                forecastMonth.atDay(1),
                forecastMonth.toString(),
                Math.max(0, forecast),
                null,
                BigDecimal.valueOf(95),
                Math.max(0, lowerBound),
                Math.max(0, upperBound),
                "LINEAR_REGRESSION"
            ));
        }

        return forecasts;
    }

    private List<SalesForecastDTO> forecastMovingAverage(Map<YearMonth, Long> historicalData, Integer monthsAhead) {
        List<YearMonth> months = new ArrayList<>(historicalData.keySet());
        List<Long> values = new ArrayList<>(historicalData.values());

        int windowSize = Math.min(6, values.size()); // 6-month moving average
        if (values.size() < windowSize) {
            return Collections.emptyList();
        }

        // Calculate moving average of last N months
        long sum = 0;
        for (int i = values.size() - windowSize; i < values.size(); i++) {
            sum += values.get(i);
        }
        long movingAvg = sum / windowSize;

        // Use moving average as forecast
        List<SalesForecastDTO> forecasts = new ArrayList<>();
        YearMonth lastMonth = months.get(months.size() - 1);

        for (int i = 1; i <= monthsAhead; i++) {
            YearMonth forecastMonth = lastMonth.plusMonths(i);
            long forecast = movingAvg;
            long margin = Math.round(forecast * 0.1); // 10% margin

            forecasts.add(new SalesForecastDTO(
                forecastMonth.atDay(1),
                forecastMonth.toString(),
                forecast,
                null,
                BigDecimal.valueOf(80),
                forecast - margin,
                forecast + margin,
                "MOVING_AVERAGE"
            ));
        }

        return forecasts;
    }

    private List<SalesForecastDTO> forecastSeasonal(Map<YearMonth, Long> historicalData, Integer monthsAhead) {
        // Group by month to find seasonal pattern
        Map<Integer, List<Long>> byMonth = new HashMap<>();
        historicalData.forEach((yearMonth, ca) -> {
            byMonth.computeIfAbsent(yearMonth.getMonthValue(), k -> new ArrayList<>()).add(ca);
        });

        // Calculate average for each month
        Map<Integer, Long> monthlyAverages = byMonth.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> (long) e.getValue().stream().mapToLong(Long::longValue).average().orElse(0)
            ));

        // Generate forecasts using seasonal averages
        List<SalesForecastDTO> forecasts = new ArrayList<>();
        YearMonth lastMonth = new ArrayList<>(historicalData.keySet()).get(historicalData.size() - 1);

        for (int i = 1; i <= monthsAhead; i++) {
            YearMonth forecastMonth = lastMonth.plusMonths(i);
            int monthValue = forecastMonth.getMonthValue();
            long forecast = monthlyAverages.getOrDefault(monthValue, 0L);
            long margin = Math.round(forecast * 0.15);

            forecasts.add(new SalesForecastDTO(
                forecastMonth.atDay(1),
                forecastMonth.toString(),
                forecast,
                null,
                BigDecimal.valueOf(85),
                forecast - margin,
                forecast + margin,
                "SEASONAL"
            ));
        }

        return forecasts;
    }

    private BigDecimal calculateAverageGrowth(List<Long> values) {
        if (values.size() < 2) {
            return BigDecimal.ZERO;
        }

        double totalGrowth = 0;
        int growthCount = 0;

        for (int i = 1; i < values.size(); i++) {
            long prev = values.get(i - 1);
            long curr = values.get(i);
            if (prev > 0) {
                double growth = ((curr - prev) * 100.0) / prev;
                totalGrowth += growth;
                growthCount++;
            }
        }

        return growthCount > 0
            ? BigDecimal.valueOf(totalGrowth / growthCount).setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
    }

    private BigDecimal calculateModelAccuracy(Map<YearMonth, Long> historicalData) {
        // Simple accuracy: compare last 6 months actual vs predicted
        List<Long> values = new ArrayList<>(historicalData.values());
        if (values.size() < 12) {
            return BigDecimal.valueOf(70); // Default accuracy
        }

        // Use first 18 months to predict last 6 months
        Map<YearMonth, Long> trainingData = historicalData.entrySet().stream()
            .limit(historicalData.size() - 6)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));

        List<SalesForecastDTO> predictions = forecastLinearRegression(trainingData, 6);

        // Compare predictions with actual
        double totalError = 0;
        List<Long> actualLast6 = values.subList(values.size() - 6, values.size());

        for (int i = 0; i < Math.min(6, predictions.size()); i++) {
            long predicted = predictions.get(i).forecastedCA();
            long actual = actualLast6.get(i);
            double error = Math.abs(predicted - actual) / (double) actual;
            totalError += error;
        }

        double avgError = totalError / 6;
        double accuracy = Math.max(0, (1 - avgError) * 100);

        return BigDecimal.valueOf(accuracy).setScale(2, RoundingMode.HALF_UP);
    }

    private double calculateStandardError(List<Long> values, double slope, double intercept) {
        double sumSquaredErrors = 0;
        for (int i = 0; i < values.size(); i++) {
            double predicted = slope * i + intercept;
            double actual = values.get(i);
            double error = actual - predicted;
            sumSquaredErrors += error * error;
        }
        return Math.sqrt(sumSquaredErrors / values.size());
    }

    private double calculateStdDev(List<Long> values, double mean) {
        double sumSquaredDiff = 0;
        for (Long value : values) {
            double diff = value - mean;
            sumSquaredDiff += diff * diff;
        }
        return Math.sqrt(sumSquaredDiff / values.size());
    }
}
