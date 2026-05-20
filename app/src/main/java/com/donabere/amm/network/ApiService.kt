package com.donabere.amm.network

import com.donabere.amm.model.Dish
import com.donabere.amm.model.Bebida
import com.donabere.amm.model.Producto
import com.donabere.amm.model.response.AuthResponse
import com.donabere.amm.model.request.LoginRequest
import com.donabere.amm.model.request.RegisterFingerprintRequest
import com.donabere.amm.model.request.BiometricLoginRequest
import com.donabere.amm.model.response.MesaResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("admin/mesas")
    suspend fun obtenerMesas(): Response<List<MesaResponse>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/biometric/register")
    suspend fun registerFingerprint(@Body request: RegisterFingerprintRequest): Response<AuthResponse>

    @POST("auth/biometric/login")
    suspend fun biometricLogin(@Body request: BiometricLoginRequest): Response<AuthResponse>
    
    @GET("reservasion/dia/mesas/menu") 
    suspend fun obtenerMenu(): Response<List<Dish>>

    @GET("admin/drink/all") 
    suspend fun obtenerBebidas(): Response<List<Bebida>>

    @GET("admin/producto/all")
    suspend fun obtenerProductos(): Response<List<Producto>>
}
