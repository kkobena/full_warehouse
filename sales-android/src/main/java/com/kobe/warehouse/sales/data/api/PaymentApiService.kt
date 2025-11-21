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
    @GET("api/payment-restricts-modes")
    suspend fun getPaymentModes(): Response<List<PaymentMode>>



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
  val qrCode: String, // byte[]

)
