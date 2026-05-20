package com.donabere.amm.repository

import com.donabere.amm.model.Dish
import com.donabere.amm.network.ApiService

class MenuRepository(private val apiService: ApiService) {
    suspend fun obtenerMenu(): Result<List<Dish>> {
        return try {
            val response = apiService.obtenerMenu()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error al cargar menú: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
