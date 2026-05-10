package com.donabere.amm.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.donabere.amm.model.Cuenta
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.model.Pedido
import com.donabere.amm.model.enums.EstadoCuenta
import com.donabere.amm.model.enums.EstadoPedido

private const val TAG = "PedidoRepository"

class PedidoRepository {

    private var pedidoActual: Pedido? = null
    private val detallesEnMemoria = mutableListOf<DetallePedido>()
    private var contadorIdDetalle = 1

    private val _detalles = MutableLiveData<List<DetallePedido>>(emptyList())
    private val _pedido = MutableLiveData<Pedido?>(null)

    fun getPedidoActivoPorMesa(mesaId: Int): LiveData<Pedido?> = _pedido

    fun getPedidoById(id: Int): LiveData<Pedido?> = _pedido

    fun getDetallesByPedido(pedidoId: Int): LiveData<List<DetallePedido>> = _detalles

    fun getCuentasByPedido(pedidoId: Int): LiveData<List<Cuenta>> =
        MutableLiveData(emptyList())

    suspend fun crearPedidoBorrador(mesasIds: List<Int>, mozoId: Int): Int {
        val pedido = Pedido(
            id       = 1, // ID fijo en memoria (solo hay un pedido activo a la vez)
            mozoId   = mozoId,
            mesasIds = mesasIds.joinToString(","),
            estado   = EstadoPedido.BORRADOR
        )
        pedidoActual = pedido
        _pedido.postValue(pedido)
        detallesEnMemoria.clear()
        _detalles.postValue(emptyList())
        Log.d(TAG, "Pedido borrador creado en memoria: mesas=$mesasIds")
        return pedido.id
    }

    suspend fun agregarDetalle(
        pedidoId: Int,
        productoId: Int,
        nombreProducto: String,
        precioUnitario: Double,
        cantidad: Int = 1,
        nota: String = ""
    ): Int {
        val existente = detallesEnMemoria.find {
            it.productoId == productoId && it.nota == nota && !it.anulado
        }

        if (existente != null) {
            val index = detallesEnMemoria.indexOf(existente)
            detallesEnMemoria[index] = existente.copy(cantidad = existente.cantidad + cantidad)
        } else {
            val detalle = DetallePedido(
                id             = contadorIdDetalle++,
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
        return existente?.id ?: contadorIdDetalle - 1
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

    suspend fun confirmarPedido(pedidoId: Int): Result<Unit> {
        if (pedidoActual == null) {
            return Result.failure(IllegalStateException("No hay pedido activo"))
        }

        val detallesActivos = detallesEnMemoria.filter { !it.anulado }
        if (detallesActivos.isEmpty()) {
            return Result.failure(IllegalStateException("El pedido no tiene productos"))
        }

        // Marcar como COMANDADO en memoria
        pedidoActual = pedidoActual?.copy(estado = EstadoPedido.COMANDADO)
        _pedido.postValue(pedidoActual)

        Log.d(TAG, "Pedido confirmado en memoria con ${detallesActivos.size} productos")
        return Result.success(Unit)
    }

    private fun actualizarDetallesYTotal() {
        val activos = detallesEnMemoria.filter { !it.anulado }
        _detalles.postValue(activos.toList())

        val total = activos.sumOf { it.subtotal }
        pedidoActual = pedidoActual?.copy(totalPagar = total)
        _pedido.postValue(pedidoActual)
    }
}