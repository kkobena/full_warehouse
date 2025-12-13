package com.kobe.warehouse.reports.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Custom report template created by user
 */
@Parcelize
data class CustomReport(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val selectedMetrics: List<ReportMetric>,
    val period: ReportPeriod = ReportPeriod.WEEK,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
) : Parcelable {

    /**
     * Check if report includes a specific metric
     */
    fun hasMetric(metric: ReportMetric): Boolean {
        return selectedMetrics.contains(metric)
    }

    /**
     * Get display name for period
     */
    fun getPeriodName(): String {
        return period.displayName
    }
}

/**
 * Available metrics for custom reports
 */
enum class ReportMetric(val id: String, val displayName: String, val icon: String) {
    CA("ca", "Chiffre d'affaires", "💰"),
    TRANSACTIONS("transactions", "Nombre de ventes", "🛒"),
    AVERAGE_BASKET("basket", "Panier moyen", "🛍️"),
    MARGIN("margin", "Marge brute", "📊"),
    TOP_PRODUCTS("top_products", "Top produits", "⭐"),
    PAYMENT_METHODS("payment_methods", "Modes de paiement", "💳"),
    SALES_BY_CATEGORY("sales_category", "Ventes par catégorie", "📦"),
    CUSTOMER_STATS("customer_stats", "Statistiques clients", "👥"),
    ALERTS("alerts", "Alertes", "🔔"),
    STOCK_STATUS("stock_status", "État du stock", "📋")
}

/**
 * Report time period
 */
enum class ReportPeriod(val displayName: String, val days: Int) {
    DAY("Jour", 1),
    WEEK("Semaine", 7),
    MONTH("Mois", 30),
    QUARTER("Trimestre", 90),
    YEAR("Année", 365)
}

/**
 * Generated custom report data
 */
@Parcelize
data class CustomReportData(
    val report: CustomReport,
    val metrics: Map<ReportMetric, MetricData>,
    val generatedAt: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Metric data result
 */
@Parcelize
data class MetricData(
    val metric: ReportMetric,
    val value: String,
    val trend: Double? = null,          // % change vs previous period
    val chartData: List<ChartDataPoint>? = null,
    val details: String? = null
) : Parcelable

/**
 * Chart data point
 */
@Parcelize
data class ChartDataPoint(
    val label: String,
    val value: Double,
    val color: Int? = null
) : Parcelable

/**
 * Report export format
 */
enum class ExportFormat(val displayName: String, val extension: String) {
    PDF("PDF", "pdf"),
    EXCEL("Excel", "xlsx"),
    CSV("CSV", "csv"),
    IMAGE("Image", "png")
}

/**
 * Report template (preset configurations)
 */
@Parcelize
data class ReportTemplate(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val metrics: List<ReportMetric>,
    val period: ReportPeriod,
    val isBuiltIn: Boolean = true
) : Parcelable {

    /**
     * Convert template to custom report
     */
    fun toCustomReport(): CustomReport {
        return CustomReport(
            name = name,
            description = description,
            selectedMetrics = metrics,
            period = period
        )
    }

    companion object {
        /**
         * Built-in report templates
         */
        val BUILT_IN_TEMPLATES = listOf(
            ReportTemplate(
                id = "daily_summary",
                name = "Résumé Quotidien",
                description = "Vue d'ensemble des ventes du jour",
                icon = "📅",
                metrics = listOf(
                    ReportMetric.CA,
                    ReportMetric.TRANSACTIONS,
                    ReportMetric.AVERAGE_BASKET,
                    ReportMetric.ALERTS
                ),
                period = ReportPeriod.DAY
            ),
            ReportTemplate(
                id = "weekly_performance",
                name = "Performance Hebdomadaire",
                description = "Analyse détaillée de la semaine",
                icon = "📊",
                metrics = listOf(
                    ReportMetric.CA,
                    ReportMetric.TRANSACTIONS,
                    ReportMetric.MARGIN,
                    ReportMetric.TOP_PRODUCTS,
                    ReportMetric.PAYMENT_METHODS
                ),
                period = ReportPeriod.WEEK
            ),
            ReportTemplate(
                id = "monthly_analysis",
                name = "Analyse Mensuelle",
                description = "Bilan complet du mois",
                icon = "📈",
                metrics = listOf(
                    ReportMetric.CA,
                    ReportMetric.TRANSACTIONS,
                    ReportMetric.MARGIN,
                    ReportMetric.TOP_PRODUCTS,
                    ReportMetric.SALES_BY_CATEGORY,
                    ReportMetric.CUSTOMER_STATS
                ),
                period = ReportPeriod.MONTH
            ),
            ReportTemplate(
                id = "stock_report",
                name = "Rapport Stock",
                description = "État des stocks et alertes",
                icon = "📦",
                metrics = listOf(
                    ReportMetric.STOCK_STATUS,
                    ReportMetric.ALERTS,
                    ReportMetric.TOP_PRODUCTS
                ),
                period = ReportPeriod.WEEK
            ),
            ReportTemplate(
                id = "financial_summary",
                name = "Bilan Financier",
                description = "Vue financière complète",
                icon = "💰",
                metrics = listOf(
                    ReportMetric.CA,
                    ReportMetric.MARGIN,
                    ReportMetric.PAYMENT_METHODS,
                    ReportMetric.CUSTOMER_STATS
                ),
                period = ReportPeriod.MONTH
            )
        )
    }
}
