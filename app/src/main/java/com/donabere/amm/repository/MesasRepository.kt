package com.donabere.amm.repository

import com.donabere.amm.model.Mesa
import com.donabere.amm.model.enums.EstadoMesa
import com.donabere.amm.network.ApiService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MesasRepository(private val apiService: ApiService) {

    private val db       = FirebaseFirestore.getInstance()
    private val mesasRef = db.collection("mesas")

    suspend fun getMesas(): Result<List<Mesa>> {
        return try {
            // 1. Lista base desde el backend REST
            val response = apiService.obtenerMesas()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Error al obtener mesas: ${response.code()}"))
            }
            val mesasBackend = response.body() ?: emptyList()

            // 2. Estados desde Firestore (colección "mesas")
            val estadosSnap = mesasRef.get().await()

            // CORRECCIÓN AQUÍ: Ahora mapeamos el campo "estado" real, ya no el "pedidoId"
            val estadosMap = estadosSnap.documents.associate { doc ->
                doc.id to (doc.getString("estado") ?: "LIBRE")
            }

            // 3. Cruzar: probar con "m{id}" primero, luego con "{id}" solo
            val mesas = mesasBackend.map { mesaResponse ->
                val idNumerico = mesaResponse.id.toString()
                val idConM     = "m$idNumerico" // → "m1", "m2"...

                // Buscar el estado en Firestore con cualquiera de los dos formatos
                val estadoFirestore = estadosMap[idConM] ?: estadosMap[idNumerico] ?: "LIBRE"

                // Si Firestore dice que está ocupada, marcamos true
                val estaOcupada = estadoFirestore == "OCUPADA"

                Mesa(
                    id          = idConM,
                    estado      = if (estaOcupada) EstadoMesa.OCUPADA else EstadoMesa.LIBRE,
                    numClientes = mesaResponse.capacity,
                    pedidoId    = null // Ya no guardamos el ID aquí, la pantalla de mesas lo buscará dinámicamente
                )
            }

            Result.success(mesas)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}