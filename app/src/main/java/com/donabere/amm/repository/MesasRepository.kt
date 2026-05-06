package com.donabere.amm.repository

import com.donabere.amm.model.response.MesaResponse
import com.donabere.amm.network.ApiService

class MesasRepository(private val apiService: ApiService) {

    suspend fun getMesas(): Result<List<MesaResponse>> {
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
