package com.donabere.amm.repository

import com.donabere.amm.model.Mesa
import com.donabere.amm.model.enums.EstadoMesa
import com.donabere.amm.network.ApiService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MesasRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getMesas(): Result<List<Mesa>> {
        return try {

            val snapshot = db.collection("mesas")
                .get()
                .await()

            val mesas = snapshot.documents.mapNotNull { doc ->

                val estadoRaw = doc.getString("estado")
                val numClientes = doc.getLong("numClientes")?.toInt() ?: 0

                Mesa(
                    id = doc.id,
                    estado = parseEstado(estadoRaw),
                    numClientes = numClientes
                )
            }

            Result.success(mesas)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseEstado(raw: String?): EstadoMesa {
        return when (raw?.trim()?.uppercase()) {
            "LIBRE" -> EstadoMesa.LIBRE
            "OCUPADA" -> EstadoMesa.OCUPADA
            else -> EstadoMesa.LIBRE
        }
    }
}
