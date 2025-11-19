package com.kobe.warehouse.sales.data.repository

import com.kobe.warehouse.sales.data.api.ProductApiService
import com.kobe.warehouse.sales.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Product Repository
 * Handles product search and caching
 */
class ProductRepository(
    private val productApiService: ProductApiService
) {

    /**
     * Search products by name or code
     */
    suspend fun searchProducts(
        search: String,
        size: Int = 5
    ): Result<List<Product>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = productApiService.autocompleteProducts(search, size)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to search products: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }


    /**
     * Get product by code (for barcode scanning)
     */
    suspend fun getProductByCode(code: String): Result<Product> {
        return withContext(Dispatchers.IO) {
            try {
                val response = productApiService.getProductByCode(code)
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Product not found: $code"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }





}
