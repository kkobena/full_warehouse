package com.kobe.warehouse.sales.data.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

/**
 * Payment Mode model
 * Represents payment methods available in the system
 */
@Parcelize
@TypeParceler<ByteArray?, ByteArrayParceler>()
data class PaymentMode(
  @SerializedName("code")
  val code: String , // as unique identifier

  @SerializedName("libelle")
  val libelle: String ,
  @SerializedName("group")
  val group: String? = null, // CASH, CARD, MOBILE_MONEY, CHECK, CREDIT

  @SerializedName("qrCode")
  val qrCode: ByteArray? = null // QR code image as byte array

) : Parcelable {

  /**
   * Get payment group label
   */
  fun getGroupLabel(): String {
    return when (group) {
      "CASH" -> "Espèces"
      "CB" -> "Carte bancaire"
      "MOBILE" -> "Mobile Money"
      else -> group ?: "Autre"
    }
  }

  /**
   * Check if payment mode has QR code
   */
  fun hasQrCode(): Boolean {
    return qrCode != null && qrCode.isNotEmpty()
  }

  // Override equals and hashCode to handle ByteArray properly
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PaymentMode

    if (code != other.code) return false
    if (libelle != other.libelle) return false
    if (group != other.group) return false
    if (qrCode != null) {
      if (other.qrCode == null) return false
      if (!qrCode.contentEquals(other.qrCode)) return false
    } else if (other.qrCode != null) return false

    return true
  }

  override fun hashCode(): Int {
    var result = code.hashCode()
    result = 31 * result + libelle.hashCode()
    result = 31 * result + (group?.hashCode() ?: 0)
    result = 31 * result + (qrCode?.contentHashCode() ?: 0)
    return result
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
 * Custom Parceler for ByteArray to handle Parcelable
 */
object ByteArrayParceler : Parceler<ByteArray?> {
  override fun create(parcel: Parcel): ByteArray? {
    val size = parcel.readInt()
    if (size < 0) return null
    val byteArray = ByteArray(size)
    parcel.readByteArray(byteArray)
    return byteArray
  }

  override fun ByteArray?.write(parcel: Parcel, flags: Int) {
    if (this == null) {
      parcel.writeInt(-1)
    } else {
      parcel.writeInt(size)
      parcel.writeByteArray(this)
    }
  }
}


