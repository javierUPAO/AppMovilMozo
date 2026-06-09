package com.donabere.amm.repository

import com.donabere.amm.model.Mesa
import com.donabere.amm.model.enums.EstadoMesa
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
                val pedidoId = doc.getString("pedidoId")
                val grupoId = doc.getString("grupoId")
                val mesasAgrupadas = (doc.get("mesasAgrupadas") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                Mesa(
                    id = doc.id,
                    estado = parseEstado(estadoRaw),
                    numClientes = numClientes,
                    pedidoId = pedidoId,
                    grupoId = grupoId,
                    mesasAgrupadas = mesasAgrupadas
                )
            }

            Result.success(mesas)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun agruparMesasLibres(mesa1: Mesa, mesa2: Mesa): Result<Unit> {
        return try {
            val batch = db.batch()

            val grupoId = mesa1.grupoId ?: mesa2.grupoId ?: "g_${mesa1.id}_${mesa2.id}"

            val grupoMesa1 = if (mesa1.grupoId != null) mesa1.mesasAgrupadas else listOf(mesa1.id)
            val grupoMesa2 = if (mesa2.grupoId != null) mesa2.mesasAgrupadas else listOf(mesa2.id)
            val todasLasMesas = (grupoMesa1 + grupoMesa2).distinct()

            todasLasMesas.forEach { mesaId ->
                val docRef = db.collection("mesas").document(mesaId)
                batch.update(docRef, mapOf(
                    "grupoId" to grupoId,
                    "mesasAgrupadas" to todasLasMesas
                ))
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun agregarMesaAPedido(mesaLibre: Mesa, mesaOcupada: Mesa): Result<Unit> {
        return try {
            val batch = db.batch()

            val pedidoId = mesaOcupada.pedidoId
                ?: return Result.failure(IllegalStateException("La mesa destino no tiene un pedido activo"))

            val grupoId = mesaOcupada.grupoId ?: "g_${mesaOcupada.id}_${mesaLibre.id}"

            val grupoOcupado = if (mesaOcupada.grupoId != null) mesaOcupada.mesasAgrupadas else listOf(mesaOcupada.id)
            val grupoLibre = if (mesaLibre.grupoId != null) mesaLibre.mesasAgrupadas else listOf(mesaLibre.id)
            val todasLasMesas = (grupoOcupado + grupoLibre).distinct()

            todasLasMesas.forEach { mesaId ->
                val docRef = db.collection("mesas").document(mesaId)
                val updates = mutableMapOf<String, Any>(
                    "grupoId" to grupoId,
                    "mesasAgrupadas" to todasLasMesas
                )
                if (mesaId == mesaLibre.id || grupoLibre.contains(mesaId)) {
                    updates["estado"] = EstadoMesa.OCUPADA.name
                    updates["pedidoId"] = pedidoId
                }
                batch.update(docRef, updates)
            }

            val pedidoRef = db.collection("pedidos").document(pedidoId)
            val pedidoDoc = pedidoRef.get().await()
            val mesasIdsActuales = (pedidoDoc.get("mesasIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val nuevasMesasIds = (mesasIdsActuales + todasLasMesas).distinct()

            batch.update(pedidoRef, "mesasIds", nuevasMesasIds)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun separarMesaDeGrupo(mesa: Mesa): Result<Unit> {
        return try {
            val batch = db.batch()

            val grupoId = mesa.grupoId ?: return Result.success(Unit)
            val mesasDelGrupo = mesa.mesasAgrupadas

            val mesaRef = db.collection("mesas").document(mesa.id)
            val updatesMesa = mutableMapOf<String, Any?>(
                "grupoId" to null,
                "mesasAgrupadas" to emptyList<String>()
            )

            if (mesa.estado == EstadoMesa.OCUPADA) {
                updatesMesa["estado"] = EstadoMesa.LIBRE.name
                updatesMesa["pedidoId"] = null

                val pedidoId = mesa.pedidoId
                if (pedidoId != null) {
                    val pedidoRef = db.collection("pedidos").document(pedidoId)
                    val pedidoDoc = pedidoRef.get().await()
                    if (pedidoDoc.exists()) {
                        val mesasIdsActuales = (pedidoDoc.get("mesasIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        val nuevasMesasIds = mesasIdsActuales.filter { it != mesa.id }
                        batch.update(pedidoRef, "mesasIds", nuevasMesasIds)
                    }
                }
            }
            batch.update(mesaRef, updatesMesa)

            val restantes = mesasDelGrupo.filter { it != mesa.id }
            if (restantes.size > 1) {
                restantes.forEach { idRestante ->
                    val ref = db.collection("mesas").document(idRestante)
                    batch.update(ref, mapOf(
                        "mesasAgrupadas" to restantes
                    ))
                }
            } else if (restantes.size == 1) {
                val ref = db.collection("mesas").document(restantes[0])
                batch.update(ref, mapOf(
                    "grupoId" to null,
                    "mesasAgrupadas" to emptyList<String>()
                ))
            }

            batch.commit().await()
            Result.success(Unit)
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
