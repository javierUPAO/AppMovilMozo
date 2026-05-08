package com.donabere.amm.repository

import com.donabere.amm.model.response.AuthResponse
import com.donabere.amm.model.request.RegisterFingerprintRequest
import com.donabere.amm.model.request.BiometricLoginRequest
import com.donabere.amm.network.ApiService

class BiometricRepository(private val apiService: ApiService) {
    
    suspend fun registerFingerprint(request: RegisterFingerprintRequest): Result<AuthResponse> {
        return try {
            val response = apiService.registerFingerprint(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                Result.failure(Exception("Error al registrar huella: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun biometricLogin(request: BiometricLoginRequest): Result<AuthResponse> {
        return try {
            val response = apiService.biometricLogin(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                Result.failure(Exception("Error en login biométrico: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
