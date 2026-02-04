package com.kobe.warehouse.sales.data.repository

import com.kobe.warehouse.sales.data.api.UserApiService
import com.kobe.warehouse.sales.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(
    private val userApiService: UserApiService
) {

    suspend fun getUsers(): Result<List<User>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = userApiService.getUsers()
                if (response.isSuccessful && response.body() != null) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Erreur lors du chargement des utilisateurs"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
