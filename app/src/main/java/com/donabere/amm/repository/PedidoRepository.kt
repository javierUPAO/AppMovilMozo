package com.donabere.amm.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.donabere.amm.model.Cuenta
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.model.Pedido
import com.donabere.amm.model.enums.EstadoCuenta
import com.donabere.amm.model.enums.EstadoPedido
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val TAG = "PedidoRepository"

/**
 * Toda la lógica de pedidos vive en Firestore.
 *
 * Colecciones usadas:
 * pedidos/                         → documento por pedido
 * pedidos/{id}/detalles/           → subcolección de DetallePedido
 * pedidos/{id}/cuentas/            → subcolección de Cuenta (solo al dividir)
 * mesas/{mesaId}                   → estado LIBRE / OCUPADA (Modificado)
 * productos/{productoId}           → { stock: Int }
 */
class PedidoRepository {

    private val db         = FirebaseFirestore.getInstance()
    private val pedidosRef = db.collection("pedidos")
    private val mesasRef   = db.collection("mesas") // Corregido: Apunta directamente a mesas
    private val stockRef   = db.collection("productos")

    // ─── Estado en memoria (borrador antes de enviar a cocina) ───────────────

    private var pedidoActual: Pedido? = null
    private val detallesBorrador = mutableListOf<DetallePedido>()
    private var contadorDetalle  = 1

    private val _detalles = MutableLiveData<List<DetallePedido>>(emptyList())
    private val _pedido   = MutableLiveData<Pedido?>(null)

    fun observarDetalles(): LiveData<List<DetallePedido>> = _detalles
    fun observarPedido():   LiveData<Pedido?>             = _pedido

    // Compatibilidad con ViewModel existente
    fun getDetallesByPedido(pedidoId: String): LiveData<List<DetallePedido>> = _detalles
    fun getPedidoActivoPorMesa(mesaId: String): LiveData<Pedido?>             = _pedido

    // ─── Crear borrador ──────────────────────────────────────────────────────

    /**
     * Crea el documento del pedido en Firestore como BORRADOR.
     * Los detalles se guardan solo en memoria hasta confirmarPedido().
     */
    suspend fun crearPedidoBorrador(mesasIds: List<String>, mozoId: String): String {
        // Corregido: Asegurar que los IDs tengan el formato "m1", "m2", etc.
        val mesasIdsFormateadas = mesasIds.map { id ->
            if (id.startsWith("m")) id else "m$id"
        }

        val data = hashMapOf(
            "mozoId"     to mozoId,
            "mesasIds"   to mesasIdsFormateadas,
            "estado"     to EstadoPedido.BORRADOR.name,
            "totalPagar" to 0.0,
            "creadoEn"   to Timestamp.now() // Corregido: Se guarda como Timestamp nativo de Firebase
        )
        val docRef   = pedidosRef.add(data).await()
        val pedidoId = docRef.id

        pedidoActual = Pedido(
            id       = pedidoId,
            mozoId   = mozoId,
            mesasIds = mesasIdsFormateadas,
            estado   = EstadoPedido.BORRADOR
        )
        detallesBorrador.clear()
        contadorDetalle = 1
        emitirEstado()

        Log.d(TAG, "Borrador creado: $pedidoId para mesas $mesasIdsFormateadas")
        return pedidoId
    }

    // ─── Gestión de detalles (en memoria, sobre el borrador) ─────────────────

    suspend fun agregarDetalle(
        pedidoId: String,
        productoId: String,
        nombreProducto: String,
        precioUnitario: Double,
        cantidad: Int = 1,
        nota: String = ""
    ): Result<String> {
        val stock = obtenerStock(productoId)
        val yaEnCarrito = detallesBorrador
            .filter { it.productoId == productoId && !it.anulado }
            .sumOf { it.cantidad }

        if (stock != null && yaEnCarrito + cantidad > stock) {
            return Result.failure(
                IllegalStateException("Stock insuficiente. Disponible: ${stock - yaEnCarrito}")
            )
        }

        val existente = detallesBorrador.find {
            it.productoId == productoId && it.nota == nota && !it.anulado
        }

        return if (existente != null) {
            val idx = detallesBorrador.indexOf(existente)
            detallesBorrador[idx] = existente.copy(cantidad = existente.cantidad + cantidad)
            emitirEstado()
            Result.success(existente.id)
        } else {
            val detalleId = "d_${contadorDetalle++}"
            detallesBorrador.add(
                DetallePedido(
                    id             = detalleId,
                    productoId     = productoId,
                    nombreProducto = nombreProducto,
                    precioUnitario = precioUnitario,
                    cantidad       = cantidad,
                    nota           = nota
                )
            )
            emitirEstado()
            Result.success(detalleId)
        }
    }

    fun actualizarCantidadDetalle(detalle: DetallePedido, nuevaCantidad: Int) {
        val idx = detallesBorrador.indexOfFirst { it.id == detalle.id }
        if (idx == -1) return
        if (nuevaCantidad <= 0) {
            detallesBorrador.removeAt(idx)
        } else {
            detallesBorrador[idx] = detalle.copy(cantidad = nuevaCantidad)
        }
        emitirEstado()
    }

    fun actualizarNotaDetalle(detalle: DetallePedido, nuevaNota: String) {
        val idx = detallesBorrador.indexOfFirst { it.id == detalle.id }
        if (idx == -1) return
        detallesBorrador[idx] = detalle.copy(nota = nuevaNota)
        emitirEstado()
    }

    fun eliminarDetalle(detalle: DetallePedido) {
        detallesBorrador.removeIf { it.id == detalle.id }
        emitirEstado()
    }

    fun restaurarDetalle(detalle: DetallePedido) {
        detallesBorrador.add(detalle)
        emitirEstado()
    }

    suspend fun obtenerStock(productoId: String): Int? {
        return try {
            val doc = stockRef.document(productoId).get().await()
            if (doc.exists()) doc.getLong("stock")?.toInt() else null
        } catch (e: Exception) {
            Log.e(TAG, "Error stock: ${e.message}")
            null
        }
    }


    // ─── Confirmar pedido (enviar a cocina) ───────────────────────────────────

    /**
     * 1. Guarda los detalles en la subcolección Firestore
     * 2. Actualiza el estado del pedido a COMANDADO
     * 3. Marca las mesas como OCUPADA en la colección 'mesas'
     * 4. Descuenta el stock
     */
    suspend fun confirmarPedido(pedidoId: String): Result<Unit> {
        val activos = detallesBorrador.filter { !it.anulado }
        if (activos.isEmpty()) {
            return Result.failure(IllegalStateException("El pedido no tiene productos"))
        }

        return try {
            val total       = activos.sumOf { it.subtotal }
            val detallesCol = pedidosRef.document(pedidoId).collection("detalles")

            val batch = db.batch()

            // 1. Guardar cada detalle
            activos.forEach { detalle ->
                val ref = detallesCol.document()
                batch.set(ref, hashMapOf(
                    "productoId"     to detalle.productoId,
                    "nombreProducto" to detalle.nombreProducto,
                    "precioUnitario" to detalle.precioUnitario,
                    "cantidad"       to detalle.cantidad,
                    "nota"           to detalle.nota,
                    "anulado"        to false
                ))
            }

            // 2. Actualizar pedido
            val pedidoRef = pedidosRef.document(pedidoId)
            batch.update(pedidoRef, mapOf(
                "estado"     to EstadoPedido.COMANDADO.name,
                "totalPagar" to total
            ))

            // 3. Marcar mesas como OCUPADA
            pedidoActual?.mesasIds?.forEach { mesaId ->
                val mesaRef = mesasRef.document(mesaId.trim())
                batch.update(mesaRef, mapOf(
                    "estado" to "OCUPADA"
                    // ¡ELIMINAMOS LA LÍNEA DE pedidoId AQUÍ!
                ))
            }

            batch.commit().await()

            // 4. Descontar stock
            activos.forEach { descontarStock(it.productoId, it.cantidad) }

            // 5. Limpiar estado en memoria
            pedidoActual = pedidoActual?.copy(estado = EstadoPedido.COMANDADO, totalPagar = total)
            detallesBorrador.clear()
            emitirEstado()

            Log.d(TAG, "Pedido $pedidoId confirmado. Total: $total")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error al confirmar pedido: ${e.message}")
            Result.failure(e)
        }
    }

    // ─── División de cuentas ──────────────────────────────────────────────────

    suspend fun dividirCuentas(
        pedidoId: String,
        division: Map<String, List<DetallePedido>>
    ): Result<List<Cuenta>> {
        return try {
            val cuentasCol = pedidosRef.document(pedidoId).collection("cuentas")
            val batch      = db.batch()
            val cuentas    = mutableListOf<Cuenta>()
            var letra      = 'A'

            division.forEach { (_, detalles) ->
                val cuentaRef  = cuentasCol.document()
                val nombre     = "Cuenta $letra"
                val totalCuenta = detalles.sumOf { it.subtotal }
                letra++

                batch.set(cuentaRef, hashMapOf(
                    "nombre"      to nombre,
                    "totalCuenta" to totalCuenta,
                    "estadoPago"  to EstadoCuenta.PENDIENTE.name,
                    "detallesIds" to detalles.map { it.id }
                ))

                cuentas.add(
                    Cuenta(
                        id         = cuentaRef.id,
                        nombre     = nombre,
                        detalles   = detalles,
                        estadoPago = EstadoCuenta.PENDIENTE
                    )
                )
            }

            batch.commit().await()
            Log.d(TAG, "Cuentas creadas: ${cuentas.size} para pedido $pedidoId")
            Result.success(cuentas)

        } catch (e: Exception) {
            Log.e(TAG, "Error al dividir cuentas: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun marcarCuentaPagada(pedidoId: String, cuentaId: String): Result<Unit> {
        return try {
            val cuentaRef = pedidosRef.document(pedidoId)
                .collection("cuentas").document(cuentaId)
            cuentaRef.update("estadoPago", EstadoCuenta.PAGADO.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Privados ─────────────────────────────────────────────────────────────

    private fun emitirEstado() {
        val activos = detallesBorrador.filter { !it.anulado }
        _detalles.postValue(activos.toList())
        val total = activos.sumOf { it.subtotal }
        pedidoActual = pedidoActual?.copy(totalPagar = total)
        _pedido.postValue(pedidoActual)
    }

    private suspend fun descontarStock(productoId: String, cantidad: Int) {
        try {
            val docRef     = stockRef.document(productoId)
            val stockActual = docRef.get().await().getLong("stock")?.toInt() ?: return

            docRef.update("stock", maxOf(0, stockActual - cantidad)).await()

        } catch (e: Exception) {
            Log.e(TAG, "Error descontando stock $productoId: ${e.message}")
        }
    }
}