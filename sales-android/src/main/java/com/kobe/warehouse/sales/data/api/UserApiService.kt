package com.kobe.warehouse.sales.data.api

import com.kobe.warehouse.sales.data.model.User
import retrofit2.Response
import retrofit2.http.GET

interface UserApiService {

    @GET("api/users")
    suspend fun getUsers(): Response<List<User>>
}
