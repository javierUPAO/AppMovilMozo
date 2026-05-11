package com.donabere.amm.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.donabere.amm.model.Cuenta
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.model.Pedido
import com.donabere.amm.model.enums.EstadoCuenta
import com.donabere.amm.model.enums.EstadoPedido
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val TAG = "PedidoRepository"

class PedidoRepository {

    private val db = FirebaseFirestore.getInstance()
    private val pedidosRef = db.collection("pedidos")
    private val mesasRef   = db.collection("mesas_estado")
    private val stockRef   = db.collection("stock")

    private var pedidoActual: Pedido? = null
    private val detallesEnMemoria = mutableListOf<DetallePedido>()
    private var contadorIdDetalle = 1

    private val _detalles = MutableLiveData<List<DetallePedido>>(emptyList())
    private val _pedido   = MutableLiveData<Pedido?>(null)


    fun getPedidoActivoPorMesa(mesaId: Int): LiveData<Pedido?> = _pedido
    fun getPedidoById(id: String): LiveData<Pedido?> = _pedido
    fun getDetallesByPedido(pedidoId: String): LiveData<List<DetallePedido>> = _detalles
    fun getCuentasByPedido(pedidoId: String): LiveData<List<Cuenta>> =
        MutableLiveData(emptyList())

    suspend fun crearPedidoBorrador(mesasIds: List<Int>, mozoId: Int): String {
        // Crear documento en Firestore
        val pedidoData = hashMapOf(
            "mozoId"    to mozoId,
            "mesasIds"  to mesasIds.joinToString(","),
            "estado"    to EstadoPedido.BORRADOR.name,
            "totalPagar" to 0.0,
            "creadoEn"  to System.currentTimeMillis()
        )
        val docRef = pedidosRef.add(pedidoData).await()
        val pedidoId = docRef.id

        val pedido = Pedido(
            id       = pedidoId,
            mozoId   = mozoId,
            mesasIds = mesasIds.joinToString(","),
            estado   = EstadoPedido.BORRADOR
        )
        pedidoActual = pedido
        _pedido.postValue(pedido)
        detallesEnMemoria.clear()
        _detalles.postValue(emptyList())

        Log.d(TAG, "Pedido borrador creado en Firestore: id=$pedidoId, mesas=$mesasIds")
        return pedidoId
    }

    suspend fun agregarDetalle(
        pedidoId: String,
        productoId: Int,
        nombreProducto: String,
        precioUnitario: Double,
        cantidad: Int = 1,
        nota: String = ""
    ): Result<String> {
        // Validar stock disponible
        val stockDisponible = obtenerStock(productoId)
        val cantidadActual = detallesEnMemoria
            .find { it.productoId == productoId && !it.anulado }?.cantidad ?: 0

        if (stockDisponible != null && cantidadActual + cantidad > stockDisponible) {
            return Result.failure(
                IllegalStateException("Stock insuficiente. Solo quedan ${stockDisponible - cantidadActual} disponibles.")
            )
        }

        val existente = detallesEnMemoria.find {
            it.productoId == productoId && it.nota == nota && !it.anulado
        }

        val detalleId: String
        if (existente != null) {
            val index = detallesEnMemoria.indexOf(existente)
            detallesEnMemoria[index] = existente.copy(cantidad = existente.cantidad + cantidad)
            detalleId = existente.id
        } else {
            detalleId = "detalle_${contadorIdDetalle++}"
            val detalle = DetallePedido(
                id             = detalleId,
                pedidoId       = pedidoId,
                productoId     = productoId,
                nombreProducto = nombreProducto,
                precioUnitario = precioUnitario,
                cantidad       = cantidad,
                nota           = nota
            )
            detallesEnMemoria.add(detalle)
        }

        actualizarDetallesYTotal()
        return Result.success(detalleId)
    }


    suspend fun actualizarCantidadDetalle(detalle: DetallePedido, nuevaCantidad: Int) {
        val index = detallesEnMemoria.indexOfFirst { it.id == detalle.id }
        if (index == -1) return
        if (nuevaCantidad <= 0) {
            detallesEnMemoria.removeAt(index)
        } else {
            detallesEnMemoria[index] = detalle.copy(cantidad = nuevaCantidad)
        }
        actualizarDetallesYTotal()
    }


    suspend fun actualizarNotaDetalle(detalle: DetallePedido, nuevaNota: String) {
        val index = detallesEnMemoria.indexOfFirst { it.id == detalle.id }
        if (index == -1) return
        detallesEnMemoria[index] = detalle.copy(nota = nuevaNota)
        actualizarDetallesYTotal()
    }


    suspend fun eliminarDetalle(detalle: DetallePedido) {
        detallesEnMemoria.removeIf { it.id == detalle.id }
        actualizarDetallesYTotal()
    }

    suspend fun confirmarPedido(pedidoId: String): Result<Unit> {
        if (pedidoActual == null) {
            return Result.failure(IllegalStateException("No hay pedido activo"))
        }

        val detallesActivos = detallesEnMemoria.filter { !it.anulado }
        if (detallesActivos.isEmpty()) {
            return Result.failure(IllegalStateException("El pedido no tiene productos"))
        }

        return try {
            val total = detallesActivos.sumOf { it.subtotal }

            // 1. Guardar detalles en subcolección de Firestore
            val detallesCol = pedidosRef.document(pedidoId).collection("detalles")
            detallesActivos.forEach { detalle ->
                val detalleData = hashMapOf(
                    "productoId"     to detalle.productoId,
                    "nombreProducto" to detalle.nombreProducto,
                    "precioUnitario" to detalle.precioUnitario,
                    "cantidad"       to detalle.cantidad,
                    "nota"           to detalle.nota,
                    "subtotal"       to detalle.subtotal
                )
                detallesCol.add(detalleData).await()
            }

            // 2. Actualizar estado del pedido en Firestore
            pedidosRef.document(pedidoId).update(
                mapOf(
                    "estado"     to EstadoPedido.COMANDADO.name,
                    "totalPagar" to total
                )
            ).await()

            // 3. Marcar mesa(s) como OCUPADA en Firestore
            val mesasIds = pedidoActual!!.mesasIds.split(",")
            mesasIds.forEach { mesaId ->
                mesasRef.document(mesaId.trim()).set(
                    hashMapOf(
                        "estado"    to "OCUPADA",
                        "pedidoId"  to pedidoId,
                        "mesaId"    to mesaId.trim().toInt()
                    )
                ).await()
            }

            // 4. Descontar stock de cada producto
            detallesActivos.forEach { detalle ->
                descontarStock(detalle.productoId, detalle.cantidad)
            }

            // 5. Actualizar estado en memoria
            pedidoActual = pedidoActual?.copy(
                estado     = EstadoPedido.COMANDADO,
                totalPagar = total
            )
            _pedido.postValue(pedidoActual)

            Log.d(TAG, "Pedido $pedidoId confirmado con ${detallesActivos.size} productos")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error al confirmar pedido: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun obtenerStock(productoId: Int): Int? {
        return try {
            val doc = stockRef.document(productoId.toString()).get().await()
            if (doc.exists()) doc.getLong("cantidad")?.toInt()
            else null
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener stock: ${e.message}")
            null
        }
    }

    suspend fun inicializarStockSiNoExiste(productoId: Int, stockInicial: Int) {
        try {
            val doc = stockRef.document(productoId.toString()).get().await()
            if (!doc.exists()) {
                stockRef.document(productoId.toString()).set(
                    hashMapOf("cantidad" to stockInicial)
                ).await()
                Log.d(TAG, "Stock inicializado: productoId=$productoId, cantidad=$stockInicial")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar stock: ${e.message}")
        }
    }

    private suspend fun descontarStock(productoId: Int, cantidad: Int) {
        try {
            val docRef = stockRef.document(productoId.toString())
            val doc = docRef.get().await()
            val stockActual = doc.getLong("cantidad")?.toInt() ?: return
            val nuevoStock = maxOf(0, stockActual - cantidad)
            docRef.update("cantidad", nuevoStock).await()
            Log.d(TAG, "Stock descontado: productoId=$productoId, nuevo stock=$nuevoStock")
        } catch (e: Exception) {
            Log.e(TAG, "Error al descontar stock: ${e.message}")
        }
    }

    suspend fun obtenerEstadoMesa(mesaId: Int): String {
        return try {
            val doc = mesasRef.document(mesaId.toString()).get().await()
            if (doc.exists()) doc.getString("estado") ?: "LIBRE"
            else "LIBRE"
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener estado de mesa: ${e.message}")
            "LIBRE"
        }
    }

    private fun actualizarDetallesYTotal() {
        val activos = detallesEnMemoria.filter { !it.anulado }
        _detalles.postValue(activos.toList())
        val total = activos.sumOf { it.subtotal }
        pedidoActual = pedidoActual?.copy(totalPagar = total)
        _pedido.postValue(pedidoActual)
    }
}