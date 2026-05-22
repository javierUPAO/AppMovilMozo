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
            //Obtener datos de Firestore
            val estadosSnap = mesasRef.get().await()

            // Mapear datos de Firestore
            val estadosMap = estadosSnap.documents.associate { doc ->
                doc.id to (doc.getString("estado") ?: "LIBRE")
            }

            // Intentar obtener mesas del backend
            val mesasBackend = try {
                val response = apiService.obtenerMesas()
                if (response.isSuccessful) response.body() ?: emptyList()
                else emptyList() // Si falla, usar datos de Firestore solamente
            } catch (e: Exception) {
                // Sin internet u otro error → usar datos de Firestore
                android.util.Log.w("MesasRepository", "No se pudo conectar al backend: ${e.message}, usando datos de Firestore")
                emptyList()
            }

            // Si backend devolvió datos, usarlos
            val mesas = if (mesasBackend.isNotEmpty()) {
                mesasBackend.map { mesaResponse ->
                    val idNumerico = mesaResponse.id.toString()
                    val idConM     = "m$idNumerico"
                    val estadoFirestore = estadosMap[idConM] ?: estadosMap[idNumerico] ?: "LIBRE"
                    val estaOcupada = estadoFirestore == "OCUPADA"

                    Mesa(
                        id          = idConM,
                        estado      = if (estaOcupada) EstadoMesa.OCUPADA else EstadoMesa.LIBRE,
                        numClientes = mesaResponse.capacity,
                        pedidoId    = null
                    )
                }
            } else {
                // Si backend falló, armar mesas solo desde Firestore
                estadosMap.map { (idConM, estado) ->
                    Mesa(
                        id          = idConM,
                        estado      = if (estado == "OCUPADA") EstadoMesa.OCUPADA else EstadoMesa.LIBRE,
                        numClientes = 0, // No tenemos capacidad desde Firestore
                        pedidoId    = null
                    )
                }
            }

            if (mesas.isEmpty()) {
                Result.failure(Exception("No hay mesas disponibles"))
            } else {
                Result.success(mesas)
            }

        } catch (e: Exception) {
            android.util.Log.e("MesasRepository", "Error al obtener mesas: ${e.message}")
            Result.failure(e)
        }
    }
}