package com.donabere.amm.repository

import com.donabere.amm.model.Turno
import com.donabere.amm.model.enums.EstadoTurno
import com.donabere.amm.model.response.ResumenTurno
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
        val calendar = java.util.Calendar.getInstance()

        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        val inicioDia = com.google.firebase.Timestamp(calendar.time)

        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)

        val finDia = com.google.firebase.Timestamp(calendar.time)

        val snapshot = turnosRef
            .whereEqualTo("mozoId", mozoId)
            .whereGreaterThanOrEqualTo("inicio", inicioDia)
            .whereLessThan("inicio", finDia)
            .limit(1)
            .get()
            .await()

        if (!snapshot.isEmpty) {
                    throw Exception("Ya existe un turno registrado hoy")
                }

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

    suspend fun obtenerResumenPorTurno(
        turno: Turno
    ): ResumenTurno {

        return try {

            val snapshot = db.collection("pedidos")
                .whereEqualTo("mozoId", turno.mozoId)
                .whereGreaterThanOrEqualTo(
                    "creadoEn",
                    turno.inicio ?: Timestamp.now()
                )
                .whereLessThanOrEqualTo(
                    "creadoEn",
                    turno.fin ?: Timestamp.now()
                )
                .get()
                .await()

            val pedidos = snapshot.documents

            val totalPedidos = pedidos.size

            val mesasSet = mutableSetOf<String>()

            var totalCobrado = 0.0

            for (pedidoDoc in pedidos) {

                // MESAS
                val mesas = (pedidoDoc.get("mesasIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?: emptyList()

                mesasSet.addAll(mesas)

                // SUBCOLLECTION CUENTAS
                val cuentasSnapshot = pedidoDoc.reference
                    .collection("cuentas")
                    .get()
                    .await()

                for (cuentaDoc in cuentasSnapshot.documents) {

                    val estadoPago =
                        cuentaDoc.getString("estadoPago") ?: ""

                    if (estadoPago != "PAGADO") continue

                    // SUBCOLLECTION DETALLES
                    val detallesSnapshot = cuentaDoc.reference
                        .collection("detalles")
                        .get()
                        .await()

                    for (detalleDoc in detallesSnapshot.documents) {

                        val cantidad =
                            (detalleDoc.getDouble("cantidad") ?: 0.0)

                        val precio =
                            (detalleDoc.getDouble("precioUnitario") ?: 0.0)

                        val anulado =
                            detalleDoc.getBoolean("anulado") ?: false

                        if (!anulado) {
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

    suspend fun obtenerIdsDeMozosConTurnoAbierto(): List<String> {
        val snapshot = turnosRef
            .whereEqualTo("estado", EstadoTurno.ABIERTO.name)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.getString("mozoId") }
    }

    suspend fun obtenerUltimoTurnoPorMozo(mozoId: String): Turno? {

        val snapshot = turnosRef
            .whereEqualTo("mozoId", mozoId)
            .orderBy("inicio", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        val doc = snapshot.documents.firstOrNull() ?: return null

        return Turno(
            id = doc.id,
            mozoId = doc.getString("mozoId") ?: "",
            inicio = doc.getTimestamp("inicio"),
            fin = doc.getTimestamp("fin"),
            estado = parseEstado(doc.getString("estado")),
            totalMesas = (doc.getLong("totalMesas") ?: 0).toInt(),
            totalPedidos = (doc.getLong("totalPedidos") ?: 0).toInt(),
            totalVendido = doc.getDouble("totalVendido") ?: 0.0
        )
    }
    private fun parseEstado(raw: String?): EstadoTurno {
        return when (raw) {
            "ABIERTO" -> EstadoTurno.ABIERTO
            "CERRADO" -> EstadoTurno.CERRADO
            else -> EstadoTurno.CERRADO
        }
    }
}