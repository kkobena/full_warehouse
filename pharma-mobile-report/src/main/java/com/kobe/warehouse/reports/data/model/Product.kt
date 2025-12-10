package com.kobe.warehouse.reports.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Stock status enumeration.
 */
enum class StockStatusEnum(val code: String, val libelle: String, val color: String) {
    RUPTURE("RUPTURE", "Rupture de stock", "red"),
    LOW("LOW", "Stock faible", "orange"),
    OK("OK", "Stock suffisant", "green");

    companion object {
        fun fromCode(code: String): StockStatusEnum {
            return entries.find { it.code == code } ?: OK
        }

        fun fromQuantity(totalQuantity: Int, minThreshold: Int): StockStatusEnum {
            return when {
                totalQuantity == 0 -> RUPTURE
                totalQuantity <= minThreshold -> LOW
                else -> OK
            }
        }
    }

    fun getEmoji(): String {
        return when (this) {
            RUPTURE -> "🔴"
            LOW -> "🟠"
            OK -> "✅"
        }
    }
}

/**
 * Expiry status enumeration.
 */
enum class ExpiryStatusEnum(val code: String, val libelle: String, val color: String) {
    EXPIRED("EXPIRED", "Expiré", "red"),
    CRITICAL("CRITICAL", "Critique (< 30 jours)", "red"),
    WARNING("WARNING", "Attention (< 90 jours)", "orange"),
    OK("OK", "Valide", "green");

    companion object {
        fun fromCode(code: String): ExpiryStatusEnum {
            return entries.find { it.code == code } ?: OK
        }

        fun fromDaysUntilExpiry(daysUntilExpiry: Int): ExpiryStatusEnum {
            return when {
                daysUntilExpiry <= 0 -> EXPIRED
                daysUntilExpiry <= 30 -> CRITICAL
                daysUntilExpiry <= 90 -> WARNING
                else -> OK
            }
        }
    }

    fun getEmoji(): String {
        return when (this) {
            EXPIRED -> "⛔"
            CRITICAL -> "🔴"
            WARNING -> "🟠"
            OK -> "✅"
        }
    }
}

/**
 * Product quick info model - matches MobileProductQuickInfoDTO from backend.
 */
@Parcelize
data class ProductQuickInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("codeCip") val codeCip: String?,
    @SerializedName("codeEan") val codeEan: String?,
    @SerializedName("stock") val stock: StockInfo,
    @SerializedName("price") val price: PriceInfo,
    @SerializedName("lots") val lots: List<LotInfo>,
    @SerializedName("salesStats") val salesStats: SalesStats,
    @SerializedName("supplierName") val supplierName: String?,
    @SerializedName("supplierId") val supplierId: Long?,
    @SerializedName("familyName") val familyName: String?,
    @SerializedName("familyId") val familyId: Long?
) : Parcelable

/**
 * Stock information with total and per-storage breakdown.
 */
@Parcelize
data class StockInfo(
    @SerializedName("totalQuantity") val totalQuantity: Int,
    @SerializedName("totalQtyStock") val totalQtyStock: Int,
    @SerializedName("totalQtyUg") val totalQtyUg: Int,
    @SerializedName("minThreshold") val minThreshold: Int,
    @SerializedName("maxThreshold") val maxThreshold: Int,
    @SerializedName("status") val status: String,
    @SerializedName("statusColor") val statusColor: String,
    @SerializedName("daysOfStock") val daysOfStock: Int,
    @SerializedName("storageStocks") val storageStocks: List<StorageStock> = emptyList()
) : Parcelable {

    /**
     * Get the stock status as enum.
     */
    fun getStockStatusEnum(): StockStatusEnum {
        return StockStatusEnum.fromCode(status)
    }

    /**
     * Get stock status emoji.
     */
    fun getStatusEmoji(): String {
        return getStockStatusEnum().getEmoji()
    }

    /**
     * Get stock status label.
     */
    fun getStatusLabel(): String {
        return getStockStatusEnum().libelle
    }

    /**
     * Check if stock is in critical state.
     */
    fun isCritical(): Boolean {
        val statusEnum = getStockStatusEnum()
        return statusEnum == StockStatusEnum.RUPTURE || statusEnum == StockStatusEnum.LOW
    }

    /**
     * Get days of stock text.
     */
    fun getDaysOfStockText(): String {
        return when {
            daysOfStock <= 0 -> "Rupture"
            daysOfStock >= 999 -> "Stock suffisant"
            else -> "$daysOfStock jours"
        }
    }

    /**
     * Get formatted total quantity text.
     */
    fun getFormattedTotalQuantity(): String {
        return if (totalQtyUg > 0) {
            "$totalQtyStock + $totalQtyUg UG"
        } else {
            totalQtyStock.toString()
        }
    }
}

/**
 * Stock per storage.
 */
@Parcelize
data class StorageStock(
    @SerializedName("storageId") val storageId: Long,
    @SerializedName("storageName") val storageName: String,
    @SerializedName("storageType") val storageType: String,
    @SerializedName("qtyStock") val qtyStock: Int,
    @SerializedName("qtyUg") val qtyUg: Int,
    @SerializedName("totalQuantity") val totalQuantity: Int
) : Parcelable {

    /**
     * Get formatted quantity text.
     */
    fun getFormattedQuantity(): String {
        return if (qtyUg > 0) {
            "$qtyStock + $qtyUg UG"
        } else {
            qtyStock.toString()
        }
    }
}

/**
 * Price information.
 */
@Parcelize
data class PriceInfo(
    @SerializedName("purchasePrice") val purchasePrice: Int,
    @SerializedName("sellingPrice") val sellingPrice: Int,
    @SerializedName("marginPercent") val marginPercent: Double,
    @SerializedName("vatRate") val vatRate: Int
) : Parcelable {

    fun getFormattedPurchasePrice(): String {
        return formatPrice(purchasePrice)
    }

    fun getFormattedSellingPrice(): String {
        return formatPrice(sellingPrice)
    }

    fun getFormattedMargin(): String {
        return String.format("%.1f%%", marginPercent)
    }

    companion object {
        fun formatPrice(amount: Int): String {
            return String.format("%,d", amount).replace(",", " ") + " F"
        }
    }
}

/**
 * Lot/batch information.
 */
@Parcelize
data class LotInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("lotNumber") val lotNumber: String?,
    @SerializedName("expiryDate") val expiryDate: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("daysUntilExpiry") val daysUntilExpiry: Int,
    @SerializedName("expiryStatus") val expiryStatus: String
) : Parcelable {

    /**
     * Get the expiry status as enum.
     */
    fun getExpiryStatusEnum(): ExpiryStatusEnum {
        return ExpiryStatusEnum.fromCode(expiryStatus)
    }

    /**
     * Get expiry status emoji.
     */
    fun getExpiryStatusEmoji(): String {
        return getExpiryStatusEnum().getEmoji()
    }

    /**
     * Get expiry status label.
     */
    fun getExpiryStatusLabel(): String {
        return getExpiryStatusEnum().libelle
    }

    /**
     * Check if lot is expired or critical.
     */
    fun isExpiredOrCritical(): Boolean {
        val statusEnum = getExpiryStatusEnum()
        return statusEnum == ExpiryStatusEnum.EXPIRED || statusEnum == ExpiryStatusEnum.CRITICAL
    }

    /**
     * Get formatted expiry text.
     */
    fun getExpiryText(): String {
        return when (getExpiryStatusEnum()) {
            ExpiryStatusEnum.EXPIRED -> "Expiré"
            ExpiryStatusEnum.CRITICAL -> "Expire dans $daysUntilExpiry jours"
            ExpiryStatusEnum.WARNING -> "Expire dans $daysUntilExpiry jours"
            ExpiryStatusEnum.OK -> expiryDate
        }
    }
}

/**
 * Sales statistics.
 */
@Parcelize
data class SalesStats(
    @SerializedName("todayQuantity") val todayQuantity: Int,
    @SerializedName("todayAmount") val todayAmount: Long,
    @SerializedName("weekQuantity") val weekQuantity: Int,
    @SerializedName("weekAmount") val weekAmount: Long,
    @SerializedName("monthQuantity") val monthQuantity: Int,
    @SerializedName("monthAmount") val monthAmount: Long,
    @SerializedName("averageDailyQuantity") val averageDailyQuantity: Double
) : Parcelable {

    fun getFormattedTodayAmount(): String {
        return PriceInfo.formatPrice(todayAmount.toInt())
    }

    fun getFormattedWeekAmount(): String {
        return PriceInfo.formatPrice(weekAmount.toInt())
    }

    fun getFormattedMonthAmount(): String {
        return PriceInfo.formatPrice(monthAmount.toInt())
    }
}

/**
 * Product search result - maps to ProduitSearch DTO from backend.
 * Uses the new structure with fournisseurs, rayons, and stocks arrays.
 */
@Parcelize
data class ProductSearchResult(
    @SerializedName("id") val id: Int,
    @SerializedName("codecipprincipalid") val codeCipPrincipalId: Int?,
    @SerializedName("libelle") val libelle: String,
    @SerializedName("codeeanlabo") val codeEanLabo: String?,
    @SerializedName("parentid") val parentId: Int?,
    @SerializedName("deconditionnable") val deconditionnable: Boolean = false,
    @SerializedName("itemqty") val itemQty: Int = 1,
    @SerializedName("vatrate") val vatRate: Int = 0,
    @SerializedName("fournisseurs") val fournisseurs: List<ProduitFournisseurSearch> = emptyList(),
    @SerializedName("rayons") val rayons: List<ProduitRayonSearch> = emptyList(),
    @SerializedName("stocks") val stocks: List<ProduitStockSearch> = emptyList(),
    // Computed fields from backend (JsonProperty)
    @SerializedName("totalQuantity") val totalQuantity: Int = 0,
    @SerializedName("regularUnitPrice") val regularUnitPrice: Int = 0,
    @SerializedName("codeProduit") val codeProduit: String? = null
) : Parcelable {

    /**
     * Get the display name.
     */
    fun getName(): String = libelle

    /**
     * Get the main code CIP.
     */
    fun getCodeCip(): String? = codeProduit

    /**
     * Get formatted selling price.
     */
    fun getFormattedPrice(): String {
        return PriceInfo.formatPrice(regularUnitPrice)
    }

    /**
     * Get formatted purchase price from main supplier.
     */
    fun getFormattedPurchasePrice(): String {
        val purchasePrice = getMainSupplier()?.purchasePrice ?: 0
        return PriceInfo.formatPrice(purchasePrice)
    }

    /**
     * Get the main supplier (fournisseur principal).
     */
    fun getMainSupplier(): ProduitFournisseurSearch? {
        return fournisseurs.find { it.id?.toInt() == codeCipPrincipalId }
            ?: fournisseurs.firstOrNull()
    }

    /**
     * Get stock status indicator.
     */
    fun getStockIndicator(): String {
        return when {
            totalQuantity <= 0 -> "🔴"
            totalQuantity <= 10 -> "🟠"
            else -> "✅"
        }
    }

    /**
     * Check if product is a child (detail) product.
     */
    fun isDetailProduct(): Boolean {
        return parentId != null
    }

    /**
     * Get rayons as comma-separated string.
     */
    fun getRayonsText(): String {
        return rayons.joinToString(", ") { it.libelle }
    }

    /**
     * Calculate margin percentage.
     */
    fun getMarginPercent(): Double {
        val purchasePrice = getMainSupplier()?.purchasePrice ?: 0
        return if (regularUnitPrice > 0) {
            ((regularUnitPrice - purchasePrice) * 100.0) / regularUnitPrice
        } else 0.0
    }

    /**
     * Get stock quantity (alias for totalQuantity for compatibility).
     */
    fun getStockQuantity(): Int = totalQuantity
}

/**
 * Supplier info for product search.
 */
@Parcelize
data class ProduitFournisseurSearch(
    @SerializedName("id") val id: Long?,
    @SerializedName("codeCip") val codeCip: String?,
    @SerializedName("codeEan") val codeEan: String?,
    @SerializedName("prixUni") val sellingPrice: Int = 0,
    @SerializedName("prixAchat") val purchasePrice: Int = 0
) : Parcelable

/**
 * Rayon info for product search.
 */
@Parcelize
data class ProduitRayonSearch(
    @SerializedName("code") val code: String?,
    @SerializedName("libelle") val libelle: String
) : Parcelable

/**
 * Stock info for product search.
 */
@Parcelize
data class ProduitStockSearch(
    @SerializedName("quantite") val quantity: Int = 0,
    @SerializedName("qteUg") val quantityUg: Int = 0,
    @SerializedName("storage") val storageId: Long?,
    @SerializedName("storageType") val storageType: String?
) : Parcelable
