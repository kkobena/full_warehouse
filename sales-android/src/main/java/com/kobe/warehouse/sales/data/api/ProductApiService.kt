package com.kobe.warehouse.sales.data.api

import com.kobe.warehouse.sales.data.model.Product
import retrofit2.Response
import retrofit2.http.*

/**
 * Product API Service
 * Retrofit interface for product-related API endpoints
 */
interface ProductApiService {


  /**
   * Search products with autocomplete
   */
  @GET("api/produits/search")
  suspend fun autocompleteProducts(
    @Query("search") query: String,
    @Query("size") limit: Int = 4,
    @Query("magasinId") magasinId: Int = 1,
  ): Response<List<Product>>

  /**
   * Get product by code (barcode)
   * Backend endpoint: GET /api/produits/code/{code}
   */
  @GET("api/produits/code/{code}")
  suspend fun getProductByCode(
    @Path("code") code: String
  ): Response<List<Product>>

  /**
   * Get product by ID
   */
  @GET("api/produits/{id}")
  suspend fun getProductById(
    @Path("id") id: Long
  ): Response<Product>

}
