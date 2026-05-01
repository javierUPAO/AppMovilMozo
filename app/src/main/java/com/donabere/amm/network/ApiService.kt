package com.donabere.amm.network

import com.donabere.amm.model.AuthResponse
import com.donabere.amm.model.LoginRequest
import com.donabere.amm.model.Mesa
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("api/v1/admin/mesas")
    suspend fun obtenerMesas(): Response<List<Mesa>>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/v1/reservasion/dia/mesas/menu")
    suspend fun obtenerMenu(): Response<List<com.donabere.amm.model.Dish>>
}
