package com.donabere.amm.repository

import com.donabere.amm.model.Mesa
import com.donabere.amm.network.ApiService

class MesasRepository(private val apiService: ApiService) {

    suspend fun getMesas(): Result<List<Mesa>> {
        return try {
            val response = apiService.obtenerMesas()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error en la respuesta del servidor: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
