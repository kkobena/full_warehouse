package com.kobe.warehouse.reports.data.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * ABC Pareto analysis model.
 * Maps to backend ABCParetoDTO.
 * Classification based on 80/20 rule (cumulative revenue contribution).
 */
data class AbcPareto(
    @SerializedName("produitId")
    val produitId: Int,
    @SerializedName("libelle")
    val libelle: String,
    @SerializedName("codeCip")
    val codeCip: String?,
    @SerializedName("categorie")
    val categorie: String?,
    @SerializedName("caTotal")
    val caTotal: Int,
    @SerializedName("qteVendue")
    val qteVendue: Int,
    @SerializedName("nbVentes")
    val nbVentes: Int,
    @SerializedName("caGlobal")
    val caGlobal: Long,
    @SerializedName("caCumule")
    val caCumule: Long,
    @SerializedName("contributionPct")
    val contributionPct: BigDecimal?,
    @SerializedName("caCumulePct")
    val caCumulePct: BigDecimal?,
    @SerializedName("classePareto")
    val classePareto: ClassePareto,
    @SerializedName("rang")
    val rang: Int
) {
    /**
     * Pareto class based on 80/20 rule.
     */
    enum class ClassePareto {
        A,  // Top ~20% products generating ~80% revenue
        B,  // Next ~30% products generating ~15% revenue
        C   // Bottom ~50% products generating ~5% revenue
    }

    /**
     * Get Pareto class display name.
     */
    fun getParetoClassLabel(): String {
        return when (classePareto) {
            ClassePareto.A -> "Classe A (80% CA)"
            ClassePareto.B -> "Classe B (15% CA)"
            ClassePareto.C -> "Classe C (5% CA)"
        }
    }

    /**
     * Get Pareto class short label.
     */
    fun getParetoShortLabel(): String {
        return classePareto.name
    }

    /**
     * Get Pareto class color.
     */
    fun getParetoColor(): Int {
        return when (classePareto) {
            ClassePareto.A -> android.graphics.Color.parseColor("#4CAF50")  // Green - Critical
            ClassePareto.B -> android.graphics.Color.parseColor("#2196F3")  // Blue - Important
            ClassePareto.C -> android.graphics.Color.parseColor("#9E9E9E")  // Grey - Low priority
        }
    }

    /**
     * Get contribution percentage formatted.
     */
    fun getContributionFormatted(): String {
        return String.format("%.2f%%", contributionPct?.toFloat() ?: 0f)
    }

    /**
     * Get cumulative percentage formatted.
     */
    fun getCumulativeFormatted(): String {
        return String.format("%.1f%%", caCumulePct?.toFloat() ?: 0f)
    }

    /**
     * Check if product is a top contributor.
     */
    fun isTopContributor(): Boolean {
        return classePareto == ClassePareto.A
    }
}

/**
 * ABC Pareto summary.
 * Maps to backend ABCParetoSummaryDTO.
 */
data class AbcParetoSummary(
    @SerializedName("totalProducts")
    val totalProducts: Int,
    @SerializedName("totalRevenue")
    val totalRevenue: Long,
    @SerializedName("classACount")
    val classACount: Int,
    @SerializedName("classARevenue")
    val classARevenue: Long,
    @SerializedName("classAPercentage")
    val classAPercentage: BigDecimal?,
    @SerializedName("classBCount")
    val classBCount: Int,
    @SerializedName("classBRevenue")
    val classBRevenue: Long,
    @SerializedName("classBPercentage")
    val classBPercentage: BigDecimal?,
    @SerializedName("classCCount")
    val classCCount: Int,
    @SerializedName("classCRevenue")
    val classCRevenue: Long,
    @SerializedName("classCPercentage")
    val classCPercentage: BigDecimal?
) {
    /**
     * Get class A product percentage of total.
     */
    fun getClassAProductPercentage(): Float {
        return if (totalProducts > 0) (classACount.toFloat() / totalProducts) * 100f else 0f
    }

    /**
     * Get class B product percentage of total.
     */
    fun getClassBProductPercentage(): Float {
        return if (totalProducts > 0) (classBCount.toFloat() / totalProducts) * 100f else 0f
    }

    /**
     * Get class C product percentage of total.
     */
    fun getClassCProductPercentage(): Float {
        return if (totalProducts > 0) (classCCount.toFloat() / totalProducts) * 100f else 0f
    }
}
