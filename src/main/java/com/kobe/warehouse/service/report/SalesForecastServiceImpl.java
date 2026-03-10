package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.ForecastSummaryDTO;
import com.kobe.warehouse.service.dto.report.SalesForecastDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SalesForecastServiceImpl implements SalesForecastService {

    /**
     * Seuil d'horizon au-delà duquel la fiabilité de la prévision diminue fortement.
     * Un avertissement est inclus dans le DTO retourné.
     */
    private static final int MAX_RELIABLE_MONTHS_AHEAD = 6;

    private final EntityManager entityManager;

    public SalesForecastServiceImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Cacheable(value = "salesForecast", key = "'forecast_' + #monthsAhead + '_' + #method")
    public List<SalesForecastDTO> getForecast(Integer monthsAhead, String method) {
        // P1 : données historiques triées explicitement par YearMonth
        Map<YearMonth, Long> historicalData = getHistoricalMonthlyCA(24);

        return switch (method.toUpperCase()) {
            case "MOVING_AVERAGE" -> forecastMovingAverage(historicalData, monthsAhead);
            case "SEASONAL"       -> forecastSeasonal(historicalData, monthsAhead);
            default               -> forecastLinearRegression(historicalData, monthsAhead);
        };
    }

    @Override
    @Cacheable(value = "salesForecast", key = "'summary'")
    public ForecastSummaryDTO getForecastSummary() {
        Map<YearMonth, Long> historicalData = getHistoricalMonthlyCA(24);
        int n = historicalData.size();

        // Qualification de la qualité des données
        String dataQuality = resolveDataQuality(n);

        // Prévisions — retournent vide si n < 2 (confiance réduite si n < 6)
        List<SalesForecastDTO> forecast3M  = forecastLinearRegression(historicalData, 3);
        List<SalesForecastDTO> forecast6M  = forecastLinearRegression(historicalData, 6);
        List<SalesForecastDTO> forecast12M = forecastLinearRegression(historicalData, 12);

        long totalForecast3M  = forecast3M.stream().mapToLong(SalesForecastDTO::forecastedCA).sum();
        long totalForecast6M  = forecast6M.stream().mapToLong(SalesForecastDTO::forecastedCA).sum();
        long totalForecast12M = forecast12M.stream().mapToLong(SalesForecastDTO::forecastedCA).sum();

        // Taux de croissance : null si INSUFFICIENT (n<4) — trop peu de points pour être fiable
        // Le résultat serait mathématiquement calculable mais statistiquement trompeur
        BigDecimal avgMonthlyGrowth;
        BigDecimal annualGrowth;
        if (n >= 4) {
            avgMonthlyGrowth = calculateAverageGrowth(new ArrayList<>(historicalData.values()));
            annualGrowth     = calculateCompoundAnnualGrowth(avgMonthlyGrowth);
        } else {
            avgMonthlyGrowth = null;
            annualGrowth     = null;
        }

        // Précision + MAE : null si non calculable (< 4 points)
        BigDecimal accuracy = null;
        BigDecimal mae      = null;
        if (n >= 4) {
            BigDecimal[] accuracyAndMae = calculateModelAccuracyAndMae(historicalData);
            accuracy = accuracyAndMae[0];
            mae      = accuracyAndMae[1];
        }

        // Saisonnalité : non significative si < 12 mois (déjà géré dans detectSeasonality)
        Boolean seasonalityDetected = n >= 12 && detectSeasonality();

        Map.Entry<YearMonth, Long> peakEntry = historicalData.entrySet().stream()
            .max(Map.Entry.comparingByValue()).orElse(null);
        Map.Entry<YearMonth, Long> lowEntry = historicalData.entrySet().stream()
            .min(Map.Entry.comparingByValue()).orElse(null);

        // peakMonth et lowMonth n'ont de sens que si on a au moins 2 mois différents
        String peakMonth = (peakEntry != null && n >= 2)
            ? peakEntry.getKey().getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) : null;
        String lowMonth  = (lowEntry  != null && n >= 2)
            ? lowEntry.getKey().getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH)  : null;

        return new ForecastSummaryDTO(
            totalForecast3M,
            totalForecast6M,
            totalForecast12M,
            avgMonthlyGrowth,
            annualGrowth,
            accuracy,
            mae,
            seasonalityDetected,
            peakMonth,
            lowMonth,
            "LINEAR_REGRESSION",
            n,
            dataQuality
        );
    }

    /**
     * Qualifie la fiabilité des prévisions selon le nombre de mois historiques disponibles.
     *   INSUFFICIENT → < 4  : résultats statistiquement non fiables
     *   LOW          → 4–11 : tendance indicative uniquement
     *   MEDIUM       → 12–17
     *   HIGH         → ≥ 18 : modèle statistiquement exploitable
     */
    private String resolveDataQuality(int n) {
        if (n < 4)  return "INSUFFICIENT";
        if (n < 12) return "LOW";
        if (n < 18) return "MEDIUM";
        return "HIGH";
    }

    @Override
    public List<SalesForecastDTO> getHistoricalVsForecast(LocalDate startDate, LocalDate endDate, Integer monthsAhead) {
        Map<YearMonth, Long> historicalData = getHistoricalMonthlyCA(24);
        List<SalesForecastDTO> forecast = forecastLinearRegression(historicalData, monthsAhead);

        List<SalesForecastDTO> combined = new ArrayList<>();

        YearMonth start   = YearMonth.from(startDate);
        YearMonth end     = YearMonth.from(endDate);
        YearMonth current = start;

        while (!current.isAfter(end)) {
            Long ca = historicalData.get(current);
            if (ca != null) {
                combined.add(new SalesForecastDTO(
                    current.atDay(1),
                    current.toString(),
                    ca,
                    ca,
                    BigDecimal.valueOf(100),
                    ca,
                    ca,
                    "HISTORICAL"
                ));
            }
            current = current.plusMonths(1);
        }

        combined.addAll(forecast);
        return combined;
    }

    @Override
    public Boolean detectSeasonality() {
        Map<YearMonth, Long> historicalData = getHistoricalMonthlyCA(24);

        if (historicalData.size() < 12) {
            return false;
        }

        // Grouper par numéro de mois (janvier=1 … décembre=12)
        Map<Integer, List<Long>> byMonth = new HashMap<>();
        historicalData.forEach((ym, ca) ->
            byMonth.computeIfAbsent(ym.getMonthValue(), k -> new ArrayList<>()).add(ca));

        // P2 : ratio variance inter-mois / variance totale (η² simplifié)
        // Si η² > 0.5 → la variation mensuelle explique > 50% de la variance totale → saisonnalité confirmée
        double grandMean = historicalData.values().stream()
            .mapToLong(Long::longValue).average().orElse(0);

        double ssBetween = 0; // somme des carrés entre groupes
        double ssTotal   = 0; // somme des carrés totale

        for (Map.Entry<Integer, List<Long>> e : byMonth.entrySet()) {
            List<Long> monthData = e.getValue();
            double groupMean = monthData.stream().mapToLong(Long::longValue).average().orElse(0);
            ssBetween += monthData.size() * Math.pow(groupMean - grandMean, 2);
            for (Long v : monthData) {
                ssTotal += Math.pow(v - grandMean, 2);
            }
        }

        double etaSquared = ssTotal > 0 ? ssBetween / ssTotal : 0;
        // Seuil : η² > 0.5 indique une saisonnalité significative
        return etaSquared > 0.5;
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Charge les données historiques mensuelles de CA.
     * - endDate : dernier jour du mois courant → inclut les ventes du mois en cours
     * - startDate : premier jour du mois, monthsBack mois en arrière
     * - Agrégat : COALESCE(SUM(s.sales_amount), 0) — cohérent avec le reste du projet
     * - COALESCE(s.canceled, false) pour gérer les NULL
     * - TreeMap → tri garanti par YearMonth indépendamment du tri SQL
     */
    private Map<YearMonth, Long> getHistoricalMonthlyCA(int monthsBack) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate endDate   = currentMonth.atEndOfMonth();
        LocalDate startDate = currentMonth.minusMonths(monthsBack).atDay(1);

        String sql = """
            SELECT
                DATE_TRUNC('month', s.sale_date)::date AS month,
                COALESCE(SUM(s.sales_amount), 0)       AS ca
            FROM sales s
            WHERE s.statut                    = 'CLOSED'
              AND COALESCE(s.canceled, false) = false
              AND s.ca                        = 'CA'
              AND s.sale_date BETWEEN :startDate AND :endDate
            GROUP BY DATE_TRUNC('month', s.sale_date)
            ORDER BY 1
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate",   endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        Map<YearMonth, Long> data = new TreeMap<>();
        for (Object[] row : results) {
            LocalDate monthDate;
            if (row[0] instanceof LocalDate ld) {
                monthDate = ld;
            } else if (row[0] instanceof java.sql.Date sd) {
                monthDate = sd.toLocalDate();
            } else {
                monthDate = LocalDate.parse(row[0].toString().substring(0, 10));
            }
            Long ca = ((Number) row[1]).longValue();
            data.put(YearMonth.from(monthDate), ca);
        }
        return data;
    }

    /**
     * Régression linéaire simple y = mx + b.
     * P3 : si monthsAhead > MAX_RELIABLE_MONTHS_AHEAD, le niveau de confiance
     *      est réduit pour signaler la baisse de fiabilité.
     */
    private List<SalesForecastDTO> forecastLinearRegression(Map<YearMonth, Long> historicalData, Integer monthsAhead) {
        List<YearMonth> months = new ArrayList<>(historicalData.keySet());
        List<Long>      values = new ArrayList<>(historicalData.values());

        int n = months.size();
        // Minimum 2 points pour une régression linéaire (pente définie)
        // Avec moins de 6 points, la précision est faible → confiance plafonnée à 60%
        if (n < 2) {
            return Collections.emptyList();
        }

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = values.get(i);
            sumX  += x;
            sumY  += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double slope     = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        // P2 : erreur standard avec n-2 degrés de liberté
        double stdError = calculateStandardError(values, slope, intercept);

        List<SalesForecastDTO> forecasts  = new ArrayList<>();
        YearMonth lastMonth = months.get(n - 1);

        for (int i = 1; i <= monthsAhead; i++) {
            YearMonth forecastMonth = lastMonth.plusMonths(i);
            double x        = n + i - 1;
            long   forecast = Math.round(slope * x + intercept);
            long   lower    = Math.round(forecast - 1.96 * stdError);
            long   upper    = Math.round(forecast + 1.96 * stdError);

            // Niveau de confiance :
            // - données insuffisantes (< 6 mois) → plafonné à 60%
            // - horizon > MAX_RELIABLE → dégradé progressivement
            // - sinon 95% (données suffisantes, horizon court)
            int maxConfidence = n < 6 ? 60 : (n < 12 ? 80 : 95);
            BigDecimal confidence = i > MAX_RELIABLE_MONTHS_AHEAD
                ? BigDecimal.valueOf(Math.max(50, maxConfidence - (i - MAX_RELIABLE_MONTHS_AHEAD) * 5L))
                : BigDecimal.valueOf(maxConfidence);

            forecasts.add(new SalesForecastDTO(
                forecastMonth.atDay(1),
                forecastMonth.toString(),
                Math.max(0, forecast),
                null,
                confidence,
                Math.max(0, lower),
                Math.max(0, upper),
                "LINEAR_REGRESSION"
            ));
        }

        return forecasts;
    }

    /**
     * Moyenne mobile.
     * P2 : rolling — chaque prévision est calculée en intégrant les prévisions précédentes
     *      dans la fenêtre glissante, au lieu d'une moyenne figée.
     */
    private List<SalesForecastDTO> forecastMovingAverage(Map<YearMonth, Long> historicalData, Integer monthsAhead) {
        List<YearMonth> months = new ArrayList<>(historicalData.keySet());
        int windowSize = Math.min(6, historicalData.size());

        // Fenêtre glissante initialisée sur les derniers mois historiques
        Deque<Long> window = new ArrayDeque<>(
            new ArrayList<>(historicalData.values())
                .subList(historicalData.size() - windowSize, historicalData.size())
        );

        List<SalesForecastDTO> forecasts = new ArrayList<>();
        YearMonth lastMonth = months.getLast();

        for (int i = 1; i <= monthsAhead; i++) {
            YearMonth forecastMonth = lastMonth.plusMonths(i);
            long movingAvg = window.stream().mapToLong(Long::longValue).sum() / window.size();
            long margin    = Math.round(movingAvg * 0.10);

            forecasts.add(new SalesForecastDTO(
                forecastMonth.atDay(1),
                forecastMonth.toString(),
                movingAvg,
                null,
                BigDecimal.valueOf(80),
                Math.max(0, movingAvg - margin),
                movingAvg + margin,
                "MOVING_AVERAGE"
            ));

            // Rolling : intégrer la prévision dans la fenêtre pour le prochain tour
            window.pollFirst();
            window.addLast(movingAvg);
        }

        return forecasts;
    }

    private List<SalesForecastDTO> forecastSeasonal(Map<YearMonth, Long> historicalData, Integer monthsAhead) {
        Map<Integer, List<Long>> byMonth = new HashMap<>();
        historicalData.forEach((ym, ca) ->
            byMonth.computeIfAbsent(ym.getMonthValue(), k -> new ArrayList<>()).add(ca));

        Map<Integer, Long> monthlyAverages = byMonth.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> (long) e.getValue().stream().mapToLong(Long::longValue).average().orElse(0)
            ));

        List<SalesForecastDTO> forecasts = new ArrayList<>();
        YearMonth lastMonth = new ArrayList<>(historicalData.keySet()).getLast();

        for (int i = 1; i <= monthsAhead; i++) {
            YearMonth forecastMonth = lastMonth.plusMonths(i);
            long forecast = monthlyAverages.getOrDefault(forecastMonth.getMonthValue(), 0L);
            long margin   = Math.round(forecast * 0.15);

            forecasts.add(new SalesForecastDTO(
                forecastMonth.atDay(1),
                forecastMonth.toString(),
                forecast,
                null,
                BigDecimal.valueOf(85),
                Math.max(0, forecast - margin),
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
        int growthCount    = 0;
        for (int i = 1; i < values.size(); i++) {
            long prev = values.get(i - 1);
            long curr = values.get(i);
            if (prev > 0) {
                double rawGrowth = ((curr - prev) * 100.0) / prev;
                // Plafonner à ±50%/mois pour éviter les outliers sur petits échantillons
                // (ex: janv creux → fév fort peut donner +200% non représentatif)
                double clampedGrowth = Math.max(-50.0, Math.min(50.0, rawGrowth));
                totalGrowth += clampedGrowth;
                growthCount++;
            }
        }
        return growthCount > 0
            ? BigDecimal.valueOf(totalGrowth / growthCount).setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
    }

    /**
     * Taux de croissance annuel composé : ((1 + r_mensuel/100)^12 - 1) * 100
     * Plafonné à ±999% — au-delà la valeur n'est plus indicative
     * (instabilité statistique sur petits échantillons).
     */
    private BigDecimal calculateCompoundAnnualGrowth(BigDecimal avgMonthlyGrowthPct) {
        if (avgMonthlyGrowthPct.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        double r      = avgMonthlyGrowthPct.doubleValue() / 100.0;
        double annual = (Math.pow(1 + r, 12) - 1) * 100;
        // Plafonner à ±999% : au-delà le chiffre est trompeur
        double capped = Math.max(-999.0, Math.min(999.0, annual));
        return BigDecimal.valueOf(capped).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcule simultanément la précision MAPE et le MAE en FCFA.
     * - Si >= 12 points : split (n-6) entraînement / 6 test
     * - Si >= 4 points  : split 50/50
     * - Si < 4 points   : précision non calculable → 0%/0 FCFA avec flag
     */
    private BigDecimal[] calculateModelAccuracyAndMae(Map<YearMonth, Long> historicalData) {
        List<Long> values = new ArrayList<>(historicalData.values());
        int n = values.size();

        if (n < 4) {
            // Pas assez de données pour un split fiable
            return new BigDecimal[]{ BigDecimal.ZERO, BigDecimal.ZERO };
        }

        // Taille de l'ensemble de test : 6 mois si n >= 12, sinon la moitié
        int testSize = n >= 12 ? 6 : n / 2;
        int trainSize = n - testSize;

        Map<YearMonth, Long> trainingData = historicalData.entrySet().stream()
            .limit(trainSize)
            .collect(Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue,
                (a, b) -> a, LinkedHashMap::new
            ));

        List<SalesForecastDTO> predictions = forecastLinearRegression(trainingData, testSize);
        if (predictions.isEmpty()) {
            return new BigDecimal[]{ BigDecimal.ZERO, BigDecimal.ZERO };
        }

        List<Long> actualTest = values.subList(trainSize, n);
        double totalMape = 0;
        double totalMae  = 0;
        int    count     = Math.min(testSize, predictions.size());

        for (int i = 0; i < count; i++) {
            long predicted = predictions.get(i).forecastedCA();
            long actual    = actualTest.get(i);
            if (actual > 0) {
                totalMape += Math.abs(predicted - actual) / (double) actual;
            }
            totalMae += Math.abs(predicted - actual);
        }

        double avgMape     = totalMape / count;
        double avgMae      = totalMae  / count;
        double accuracyPct = Math.max(0, (1 - avgMape) * 100);

        return new BigDecimal[]{
            BigDecimal.valueOf(accuracyPct).setScale(2, RoundingMode.HALF_UP),
            BigDecimal.valueOf(avgMae).setScale(0, RoundingMode.HALF_UP)
        };
    }

    /**
     * P2 : erreur standard de régression avec n-2 degrés de liberté (correct statistiquement).
     */
    private double calculateStandardError(List<Long> values, double slope, double intercept) {
        int n = values.size();
        if (n <= 2) {
            return 0;
        }
        double sumSquaredErrors = 0;
        for (int i = 0; i < n; i++) {
            double predicted = slope * i + intercept;
            double error     = values.get(i) - predicted;
            sumSquaredErrors += error * error;
        }
        // n-2 : degrés de liberté d'une régression linéaire simple
        return Math.sqrt(sumSquaredErrors / (n - 2));
    }

    private double calculateStdDev(List<Long> values, double mean) {
        double sumSquaredDiff = 0;
        for (Long value : values) {
            double diff = value - mean;
            sumSquaredDiff += diff * diff;
        }
        // Population std dev (n) — utilisé pour le CV mensuel
        return values.size() > 1 ? Math.sqrt(sumSquaredDiff / values.size()) : 0;
    }
}
