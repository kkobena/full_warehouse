package com.kobe.warehouse.sales.data.api

import com.kobe.warehouse.sales.data.model.PaymentMode
import retrofit2.Response
import retrofit2.http.*

/**
 * Payment API Service
 * Retrofit interface for payment-related API endpoints
 */
interface PaymentApiService {

    /**
     * Get all enabled payment modes
     */
    @GET("api/payment-modes")
    suspend fun getPaymentModes(): Response<List<PaymentMode>>

    /**
     * Get payment mode by code
     */
    @GET("api/payment-modes/{code}")
    suspend fun getPaymentModeByCode(
        @Path("code") code: String
    ): Response<PaymentMode>

    /**
     * Get payment modes by group
     */
    @GET("api/payment-modes/group/{group}")
    suspend fun getPaymentModesByGroup(
        @Path("group") group: String
    ): Response<List<PaymentMode>>

    /**
     * Get QR code for mobile money payment
     */
    @GET("api/payment-modes/{code}/qr-code")
    suspend fun getPaymentQrCode(
        @Path("code") code: String
    ): Response<QrCodeResponse>
}

/**
 * QR Code response
 */
data class QrCodeResponse(
    val code: String,
    val qrCodeData: String, // Base64 encoded PNG image
    val expiresAt: String? = null
)
