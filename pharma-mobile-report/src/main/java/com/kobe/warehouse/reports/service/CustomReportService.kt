package com.kobe.warehouse.reports.service

import android.content.Context
import android.graphics.Color
import com.kobe.warehouse.reports.data.api.CustomReportGenerateRequest
import com.kobe.warehouse.reports.data.api.ReportApiService
import com.kobe.warehouse.reports.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Service for generating custom reports
 * Collects data for selected metrics and builds report
 */
class CustomReportService(
    private val context: Context,
    private val apiService: ReportApiService
) {

    /**
     * Generate custom report data
     *
     * @param report Custom report configuration
     * @return Generated report data with all metrics
     */
    suspend fun generateReport(report: CustomReport): Result<CustomReportData> = withContext(Dispatchers.IO) {
        try {
            // Calculate date range based on period
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(report.period.days.toLong())

            // First, try to use backend API
            val backendMetrics = try {
                fetchMetricsFromBackend(report, startDate, endDate)
            } catch (e: Exception) {
                null
            }

            val metrics = mutableMapOf<ReportMetric, MetricData>()

            if (backendMetrics != null) {
                // Convert backend response to MetricData
                report.selectedMetrics.forEach { metric ->
                    backendMetrics[metric.name]?.let { apiMetric ->
                        metrics[metric] = MetricData(
                            metric = metric,
                            value = apiMetric.value,
                            trend = apiMetric.trend,
                            chartData = apiMetric.chartData?.map { point ->
                                ChartDataPoint(
                                    label = point.label,
                                    value = point.value,
                                    color = point.color
                                )
                            },
                            details = apiMetric.details
                        )
                    }
                }
            } else {
                // Fallback to local metric generation (mock)
                report.selectedMetrics.forEach { metric ->
                    val metricData = when (metric) {
                        ReportMetric.CA -> generateCAMetric(startDate, endDate, report.period)
                        ReportMetric.TRANSACTIONS -> generateTransactionsMetric(startDate, endDate)
                        ReportMetric.AVERAGE_BASKET -> generateAverageBasketMetric(startDate, endDate)
                        ReportMetric.MARGIN -> generateMarginMetric(startDate, endDate)
                        ReportMetric.TOP_PRODUCTS -> generateTopProductsMetric(startDate, endDate)
                        ReportMetric.PAYMENT_METHODS -> generatePaymentMethodsMetric(startDate, endDate)
                        ReportMetric.SALES_BY_CATEGORY -> generateSalesByCategoryMetric(startDate, endDate)
                        ReportMetric.CUSTOMER_STATS -> generateCustomerStatsMetric(startDate, endDate)
                        ReportMetric.ALERTS -> generateAlertsMetric()
                        ReportMetric.STOCK_STATUS -> generateStockStatusMetric()
                    }
                    metrics[metric] = metricData
                }
            }

            Result.success(
                CustomReportData(
                    report = report,
                    metrics = metrics
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch metrics from backend API
     */
    private suspend fun fetchMetricsFromBackend(
        report: CustomReport,
        startDate: LocalDate,
        endDate: LocalDate
    ): Map<String, com.kobe.warehouse.reports.data.api.CustomReportMetric>? {
        return try {
            val request = CustomReportGenerateRequest(
                metricCodes = report.selectedMetrics.map { it.name },
                startDate = startDate.format(DateTimeFormatter.ISO_DATE),
                endDate = endDate.format(DateTimeFormatter.ISO_DATE)
            )

            val response = apiService.generateCustomReport(request)

            if (response.isSuccessful && response.body() != null) {
                response.body()!!.metrics
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.w("CustomReportService", "Failed to fetch from backend, using fallback", e)
            null
        }
    }

    /**
     * Generate CA (Revenue) metric
     */
    private suspend fun generateCAMetric(
        startDate: LocalDate,
        endDate: LocalDate,
        period: ReportPeriod
    ): MetricData {
        // Fetch CA data from API
        val totalCA = fetchTotalCA(startDate, endDate)
        val previousCA = fetchTotalCA(
            startDate.minusDays(period.days.toLong()),
            endDate.minusDays(period.days.toLong())
        )

        val trend = if (previousCA > 0) {
            ((totalCA - previousCA) / previousCA) * 100
        } else 0.0

        // Generate chart data (daily breakdown)
        val chartData = generateDailyCAChart(startDate, endDate)

        return MetricData(
            metric = ReportMetric.CA,
            value = formatAmount(totalCA),
            trend = trend,
            chartData = chartData,
            details = "Période: ${formatDate(startDate)} - ${formatDate(endDate)}"
        )
    }

    /**
     * Generate Transactions metric
     */
    private suspend fun generateTransactionsMetric(
        startDate: LocalDate,
        endDate: LocalDate
    ): MetricData {
        val totalTransactions = fetchTotalTransactions(startDate, endDate)
        val chartData = generateDailyTransactionsChart(startDate, endDate)

        return MetricData(
            metric = ReportMetric.TRANSACTIONS,
            value = totalTransactions.toString(),
            chartData = chartData,
            details = "Transactions enregistrées"
        )
    }

    /**
     * Generate Average Basket metric
     */
    private suspend fun generateAverageBasketMetric(
        startDate: LocalDate,
        endDate: LocalDate
    ): MetricData {
        val totalCA = fetchTotalCA(startDate, endDate)
        val totalTransactions = fetchTotalTransactions(startDate, endDate)

        val averageBasket = if (totalTransactions > 0) {
            totalCA / totalTransactions
        } else 0.0

        return MetricData(
            metric = ReportMetric.AVERAGE_BASKET,
            value = formatAmount(averageBasket),
            details = "Valeur moyenne par transaction"
        )
    }

    /**
     * Generate Margin metric
     */
    private suspend fun generateMarginMetric(
        startDate: LocalDate,
        endDate: LocalDate
    ): MetricData {
        val totalCA = fetchTotalCA(startDate, endDate)
        val totalCost = fetchTotalCost(startDate, endDate)
        val margin = totalCA - totalCost
        val marginPercent = if (totalCA > 0) (margin / totalCA) * 100 else 0.0

        return MetricData(
            metric = ReportMetric.MARGIN,
            value = formatAmount(margin),
            trend = marginPercent,
            details = String.format("%.1f%% du CA", marginPercent)
        )
    }

    /**
     * Generate Top Products metric
     */
    private suspend fun generateTopProductsMetric(
        startDate: LocalDate,
        endDate: LocalDate
    ): MetricData {
        val topProducts = fetchTopProducts(startDate, endDate, limit = 10)

        val chartData = topProducts.mapIndexed { index, product ->
            ChartDataPoint(
                label = product.name,
                value = product.salesAmount,
                color = getColorForIndex(index)
            )
        }

        val details = topProducts.take(5).joinToString("\n") { product ->
            "${product.name}: ${formatAmount(product.salesAmount)}"
        }

        return MetricData(
            metric = ReportMetric.TOP_PRODUCTS,
            value = topProducts.size.toString(),
            chartData = chartData,
            details = details
        )
    }

    /**
     * Generate Payment Methods metric
     */
    private suspend fun generatePaymentMethodsMetric(
        startDate: LocalDate,
        endDate: LocalDate
    ): MetricData {
        val paymentStats = fetchPaymentMethodStats(startDate, endDate)

        val chartData = paymentStats.map { (method, amount) ->
            ChartDataPoint(
                label = method,
                value = amount,
                color = getColorForPaymentMethod(method)
            )
        }

        val details = paymentStats.entries.joinToString("\n") { (method, amount) ->
            "$method: ${formatAmount(amount)}"
        }

        return MetricData(
            metric = ReportMetric.PAYMENT_METHODS,
            value = paymentStats.size.toString(),
            chartData = chartData,
            details = details
        )
    }

    /**
     * Generate Sales by Category metric
     */
    private suspend fun generateSalesByCategoryMetric(
        startDate: LocalDate,
        endDate: LocalDate
    ): MetricData {
        val categoryStats = fetchCategoryStats(startDate, endDate)

        val chartData = categoryStats.map { (category, amount) ->
            ChartDataPoint(
                label = category,
                value = amount
            )
        }

        return MetricData(
            metric = ReportMetric.SALES_BY_CATEGORY,
            value = categoryStats.size.toString(),
            chartData = chartData,
            details = "Catégories actives"
        )
    }

    /**
     * Generate Customer Stats metric
     */
    private suspend fun generateCustomerStatsMetric(
        startDate: LocalDate,
        endDate: LocalDate
    ): MetricData {
        val totalCustomers = fetchTotalCustomers(startDate, endDate)
        val newCustomers = fetchNewCustomers(startDate, endDate)

        return MetricData(
            metric = ReportMetric.CUSTOMER_STATS,
            value = totalCustomers.toString(),
            details = "Nouveaux clients: $newCustomers"
        )
    }

    /**
     * Generate Alerts metric
     */
    private suspend fun generateAlertsMetric(): MetricData {
        val alerts = fetchActiveAlerts()

        val details = buildString {
            append("Stock: ${alerts.filter { it.type == "STOCK" }.size}\n")
            append("Péremptions: ${alerts.filter { it.type == "EXPIRY" }.size}\n")
            append("Caisse: ${alerts.filter { it.type == "CASH" }.size}")
        }

        return MetricData(
            metric = ReportMetric.ALERTS,
            value = alerts.size.toString(),
            details = details
        )
    }

    /**
     * Generate Stock Status metric
     */
    private suspend fun generateStockStatusMetric(): MetricData {
        val stockStats = fetchStockStats()

        val details = buildString {
            append("En stock: ${stockStats["inStock"]}\n")
            append("Stock faible: ${stockStats["lowStock"]}\n")
            append("Rupture: ${stockStats["outOfStock"]}")
        }

        return MetricData(
            metric = ReportMetric.STOCK_STATUS,
            value = stockStats["total"].toString(),
            details = details
        )
    }

    // Helper methods for data fetching (mock implementations)
    // In production, these would call actual API endpoints

    private suspend fun fetchTotalCA(startDate: LocalDate, endDate: LocalDate): Double {
        // Mock implementation
        return 15_000_000.0 + (Math.random() * 5_000_000)
    }

    private suspend fun fetchTotalCost(startDate: LocalDate, endDate: LocalDate): Double {
        val ca = fetchTotalCA(startDate, endDate)
        return ca * 0.65 // 35% margin
    }

    private suspend fun fetchTotalTransactions(startDate: LocalDate, endDate: LocalDate): Int {
        return (800 + (Math.random() * 200)).toInt()
    }

    private suspend fun fetchTopProducts(startDate: LocalDate, endDate: LocalDate, limit: Int): List<ProductSales> {
        return listOf(
            ProductSales(1, "Paracétamol 500mg", 1_850_000.0),
            ProductSales(2, "Amoxicilline 500mg", 1_250_000.0),
            ProductSales(3, "Ibuprofène 400mg", 980_000.0),
            ProductSales(4, "Aspirine 500mg", 750_000.0),
            ProductSales(5, "Doliprane 1000mg", 650_000.0)
        ).take(limit)
    }

    private suspend fun fetchPaymentMethodStats(startDate: LocalDate, endDate: LocalDate): Map<String, Double> {
        return mapOf(
            "Espèces" to 7_000_000.0,
            "Mobile Money" to 4_500_000.0,
            "Carte Bancaire" to 3_000_000.0,
            "Crédit" to 500_000.0
        )
    }

    private suspend fun fetchCategoryStats(startDate: LocalDate, endDate: LocalDate): Map<String, Double> {
        return mapOf(
            "Médicaments" to 10_000_000.0,
            "Parapharmacie" to 3_000_000.0,
            "Matériel médical" to 2_000_000.0
        )
    }

    private suspend fun fetchTotalCustomers(startDate: LocalDate, endDate: LocalDate): Int {
        return 1250
    }

    private suspend fun fetchNewCustomers(startDate: LocalDate, endDate: LocalDate): Int {
        return 85
    }

    private suspend fun fetchActiveAlerts(): List<MockAlert> {
        return listOf(
            MockAlert("STOCK", "Rupture"),
            MockAlert("EXPIRY", "Péremption"),
            MockAlert("CASH", "Écart caisse")
        )
    }

    private suspend fun fetchStockStats(): Map<String, Int> {
        return mapOf(
            "total" to 5000,
            "inStock" to 4500,
            "lowStock" to 450,
            "outOfStock" to 50
        )
    }

    private fun generateDailyCAChart(startDate: LocalDate, endDate: LocalDate): List<ChartDataPoint> {
        val days = (endDate.toEpochDay() - startDate.toEpochDay()).toInt()
        return (0..days).map { dayOffset ->
            val date = startDate.plusDays(dayOffset.toLong())
            ChartDataPoint(
                label = date.format(DateTimeFormatter.ofPattern("dd/MM")),
                value = 2_000_000.0 + (Math.random() * 500_000)
            )
        }
    }

    private fun generateDailyTransactionsChart(startDate: LocalDate, endDate: LocalDate): List<ChartDataPoint> {
        val days = (endDate.toEpochDay() - startDate.toEpochDay()).toInt()
        return (0..days).map { dayOffset ->
            val date = startDate.plusDays(dayOffset.toLong())
            ChartDataPoint(
                label = date.format(DateTimeFormatter.ofPattern("dd/MM")),
                value = (100 + (Math.random() * 50)).toDouble()
            )
        }
    }

    private fun formatAmount(amount: Double): String {
        return String.format("%,.0f FCFA", amount)
    }

    private fun formatDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    private fun getColorForIndex(index: Int): Int {
        val colors = listOf(
            Color.parseColor("#2196F3"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#FFC107"),
            Color.parseColor("#FF5722"),
            Color.parseColor("#9C27B0")
        )
        return colors[index % colors.size]
    }

    private fun getColorForPaymentMethod(method: String): Int {
        return when (method) {
            "Espèces" -> Color.parseColor("#4CAF50")
            "Mobile Money" -> Color.parseColor("#2196F3")
            "Carte Bancaire" -> Color.parseColor("#FFC107")
            "Crédit" -> Color.parseColor("#FF5722")
            else -> Color.GRAY
        }
    }

    // Data classes for mock data
    data class ProductSales(val id: Long, val name: String, val salesAmount: Double)
    data class MockAlert(val type: String, val message: String)
}
