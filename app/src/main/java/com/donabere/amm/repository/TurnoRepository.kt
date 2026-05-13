package com.donabere.amm.repository

import com.donabere.amm.model.Turno
import com.donabere.amm.model.enums.EstadoTurno
import com.donabere.amm.model.response.ResumenTurno
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


class TurnoRepository {
    private val db = FirebaseFirestore.getInstance()
    private val turnosRef = db.collection("turno")

    suspend fun obtenerTurnoActivoPorMozo(mozoId: String): Turno? {
        return try {
            val snapshot = turnosRef
                .whereEqualTo("mozoId", mozoId)
                .whereEqualTo("estado", EstadoTurno.ABIERTO.name)
                .limit(1)
                .get()
                .await()

            val doc = snapshot.documents.firstOrNull() ?: return null

            Turno(
                id = doc.id,
                mozoId = doc.getString("mozoId") ?: "",
                inicio = doc.getTimestamp("inicio"),
                fin = doc.getTimestamp("fin"),
                estado = parseEstado(doc.getString("estado")),
                totalMesas = (doc.getLong("totalMesas") ?: 0).toInt(),
                totalPedidos = (doc.getLong("totalPedidos") ?: 0).toInt(),
                totalVendido = doc.getDouble("totalVendido") ?: 0.0
            )
        } catch (e: Exception) {
            null
        }
    }



    suspend fun cerrarTurnoConResumen(
        turnoId: String,
        resumen: ResumenTurno
    ) {
        try {
            /* val horaActual = LocalTime.now()
 val horaCierrePermitida = LocalTime.of(16, 0) // 04:00 PM

 if (horaActual.isBefore(horaCierrePermitida)) {
 throw Exception("No se puede cerrar turno antes de las 04:00 PM")
}*/

            turnosRef.document(turnoId)
                .update(
                    mapOf(
                        "estado" to EstadoTurno.CERRADO.name,
                        "fin" to com.google.firebase.Timestamp.now(),
                        "totalMesas" to resumen.totalMesas,
                        "totalPedidos" to resumen.totalPedidos,
                        "totalVendido" to resumen.totalCobrado
                    )
                )
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun abrirTurnoSiNoExiste(mozoId: String) {
        //val existente = obtenerTurnoActivoPorMozo(mozoId)

       // if (existente != null) return

        val turno = hashMapOf(
            "mozoId" to mozoId,
            "inicio" to com.google.firebase.Timestamp.now(),
            "fin" to null,
            "estado" to EstadoTurno.ABIERTO.name,
            "totalMesas" to 0,
            "totalPedidos" to 0,
            "totalVendido" to 0.0
        )

        turnosRef.add(turno).await()
    }

    suspend fun obtenerResumenPorMozo(mozoId: String): ResumenTurno {
        return try {

            val snapshot = db.collection("pedidos")
                .whereEqualTo("mozoId", mozoId)
                .get()
                .await()

            val pedidos = snapshot.documents

            val totalPedidos = pedidos.size

            // MESAS únicas
            val mesasSet = mutableSetOf<String>()

            var totalCobrado = 0.0

            for (doc in pedidos) {

                val mesas = (doc.get("mesasIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                mesasSet.addAll(mesas)

                val cuentas = (doc.get("cuentas") as? List<*>)?.mapNotNull { it as? Map<String, Any> } ?: emptyList()

                for (cuenta in cuentas) {
                    if (cuenta["estadoPago"] == "PAGADO") {

                        val items = cuenta["items"] as? List<Map<String, Any>> ?: emptyList()

                        for (item in items) {

                            val cantidad = (item["cantidad"] as? Number)?.toDouble() ?: 0.0
                            val precio = (item["precioUnitario"] as? Number)?.toDouble() ?: 0.0

                            totalCobrado += cantidad * precio
                        }
                    }
                }
            }

            ResumenTurno(
                totalMesas = mesasSet.size,
                totalPedidos = totalPedidos,
                totalCobrado = totalCobrado
            )

        } catch (e: Exception) {
            throw e
        }
    }
    private fun parseEstado(raw: String?): EstadoTurno {
        return when (raw) {
            "ABIERTO" -> EstadoTurno.ABIERTO
            "CERRADO" -> EstadoTurno.CERRADO
            else -> EstadoTurno.CERRADO
        }
    }
}