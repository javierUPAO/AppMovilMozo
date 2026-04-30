package com.donabere.amm.repository

import com.donabere.amm.model.AuthResponse
import com.donabere.amm.model.LoginRequest
import com.donabere.amm.network.ApiService

class AuthRepository(private val apiService: ApiService) {
    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
            val response = apiService.login(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                Result.failure(Exception("Error de credenciales o servidor: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
