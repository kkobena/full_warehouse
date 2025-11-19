package com.kobe.warehouse.sales.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Payment Mode model
 * Represents payment methods available in the system
 */
@Parcelize
data class PaymentMode(
  @SerializedName("code")
  val code: String = "",

  @SerializedName("libelle")
  val libelle: String = "",

  @SerializedName("order")
  val order: Int = 0,

  @SerializedName("group")
  val group: String = "CASH", // CASH, CARD, MOBILE_MONEY, CHECK, CREDIT

  @SerializedName("qrCode")
  val qrCode: String? = null // Base64 encoded QR code image

) : Parcelable {

  /**
   * Get payment group label
   */
  fun getGroupLabel(): String {
    return when (group) {
      "CASH" -> "Espèces"
      "CB" -> "Carte bancaire"
      "MOBILE" -> "Mobile Money"
      "CHEQUE" -> "Chèque"
      "CREDIT" -> "Crédit"
      else -> group
    }
  }

  /**
   * Check if payment mode has QR code
   */
  fun hasQrCode(): Boolean {
    return !qrCode.isNullOrEmpty()
  }

  /**
   * Check if this is mobile money payment
   */
  fun isMobileMoney(): Boolean {
    return group == "MOBILE" || code == "OM" || code == "MTN" || code == "MOOV" || code == "WAVE"
  }

  /**
   * Check if this is cash payment
   */
  fun isCash(): Boolean {
    return group == "CASH" || code == "CASH"
  }

  /**
   * Check if this is card payment
   */
  fun isCard(): Boolean {
    return group == "CB" || code == "CB"
  }

  /**
   * Get icon resource based on payment group
   */
  fun getIconResource(): String {
    return when (group) {
      "CASH" -> "ic_money"
      "CB" -> "ic_credit_card"
      "MOBILE" -> "ic_phone_android"
      "CHEQUE" -> "ic_receipt"
      "CREDIT" -> "ic_account_balance"
      else -> "ic_payment"
    }
  }
}

/**
 * Payment group enum
 */
enum class PaymentGroup {
  CASH,
  CARD,
  MOBILE_MONEY,
  CHECK,
  CREDIT
}
