package com.donabere.amm.repository

import com.donabere.amm.model.Bebida
import com.donabere.amm.network.ApiService

class BebidasRepository(private val apiService: ApiService) {
    suspend fun obtenerBebidas(): Result<List<Bebida>> {
        return try {
            val response = apiService.obtenerBebidas()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error al cargar bebidas: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
