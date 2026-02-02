package com.kobe.warehouse.sales.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Customer model
 * Represents a customer in the pharmacy system
 */
@Parcelize
data class Customer(
  @SerializedName("id")
  val id: Int = 0,

  @SerializedName("firstName")
  val firstName: String = "",

  @SerializedName("lastName")
  val lastName: String = "",

  @SerializedName("phone")
  val phone: String? = null,

  @SerializedName("email")
  val email: String? = null,

  @SerializedName("fullName")
  val fullName: String = "",

  @SerializedName("remiseId")
  val remiseId: Long? = null,


  @SerializedName("currentBalance")
  val currentBalance: Int? = null,
  @SerializedName("tiersPayants")
  val tiersPayants: List<ClientTiersPayant> = emptyList(),

  @SerializedName("type")
  val type: String? = null,

  @SerializedName("sexe")
  val sexe: String? = null,

  @SerializedName("datNaiss")
  val datNaiss: String? = null,

  @SerializedName("num")
  val num: String? = null,

  @SerializedName("taux")
  val taux: Int? = null,

  @SerializedName("tiersPayantId")
  val tiersPayantId: Long? = null

) : Parcelable {

  /**
   * Get display name
   */
  fun getDisplayName(): String {
    return if (fullName.isNotEmpty()) {
      fullName
    } else {
      "$firstName $lastName".trim()
    }
  }

  /**
   * Get initials
   */
  fun getInitials(): String {
    val first = firstName.firstOrNull()?.uppercase() ?: ""
    val last = lastName.firstOrNull()?.uppercase() ?: ""
    return "$first$last"
  }


  /**
   * Get formatted current balance
   */
  fun getFormattedCurrentBalance(): String {
    val balance = currentBalance ?: 0
    return "${formatAmount(balance)} FCFA"
  }


  /**
   * Format amount with space as thousand separator
   */
  private fun formatAmount(amount: Int): String {
    return amount.toString().reversed().chunked(3).joinToString(" ").reversed()
  }

}

/**
 * Customer search request
 */
data class CustomerSearchRequest(
  @SerializedName("search")
  val search: String = "",

  @SerializedName("limit")
  val limit: Int = 20
)
