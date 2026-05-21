package com.donabere.amm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.repository.PedidoRepository
import kotlinx.coroutines.launch

class PedidoViewModel(
    private val repository: PedidoRepository,
    private val mozoId: String
) : ViewModel() {

    // ─── Estado UI ────────────────────────────────────────────────────────────
    sealed class UiState {
        object Idle          : UiState()
        object Loading       : UiState()
        object PedidoEnviado : UiState()
        data class Success(val mensaje: String) : UiState()
        data class Error(val mensaje: String)   : UiState()
    }

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    // ─── Datos observables ────────────────────────────────────────────────────
    val detalles: LiveData<List<DetallePedido>> = repository.observarDetalles()
    val pedido:   LiveData<*>                   = repository.observarPedido()

    private val _pedidoId = MutableLiveData<String?>(null)
    val pedidoId: LiveData<String?> = _pedidoId

    private lateinit var mesasIds: List<String>

    // ─── Inicializar ─────────────────────────────────────────────────────────

    fun iniciarPedido(mesasIds: List<String>) {
        this.mesasIds = mesasIds
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val id = repository.crearPedidoBorrador(mesasIds, mozoId)
                _pedidoId.value = id
                _uiState.value = UiState.Idle
            } catch (e: Exception) {
                _uiState.value = UiState.Error("No se pudo iniciar el pedido: ${e.message}")
            }
        }
    }

    // ─── Carrito ──────────────────────────────────────────────────────────────

    fun agregarProducto(
        productoId: String,
        nombreProducto: String,
        precioUnitario: Double,
        cantidad: Int = 1,
        nota: String = "",
        imagenProducto: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // Si no hay borrador aún, crearlo ahora
                val id = _pedidoId.value
                    ?: repository.crearPedidoBorrador(mesasIds, mozoId)
                        .also { _pedidoId.value = it }

                repository.agregarDetalle(
                    pedidoId       = id,
                    productoId     = productoId,
                    nombreProducto = nombreProducto,
                    precioUnitario = precioUnitario,
                    cantidad       = cantidad,
                    nota           = nota,
                    imagenUrl      = imagenProducto
                ).fold(
                    onSuccess = { _uiState.value = UiState.Idle },
                    onFailure = { e ->
                        _uiState.value = UiState.Error(e.message ?: "Error al agregar")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error: ${e.message}")
            }
        }
    }

    fun actualizarCantidad(detalle: DetallePedido, nuevaCantidad: Int) {
        viewModelScope.launch {
            // Si incrementa, validar stock
            if (nuevaCantidad > detalle.cantidad) {
                val stock = repository.obtenerStock(detalle.productoId)
                if (stock != null && nuevaCantidad > stock) {
                    _uiState.value = UiState.Error("Stock máximo disponible: $stock")
                    return@launch
                }
            }
            repository.actualizarCantidadDetalle(detalle, nuevaCantidad)
        }
    }

    fun actualizarNota(detalle: DetallePedido, nuevaNota: String) {
        repository.actualizarNotaDetalle(detalle, nuevaNota)
    }

    fun eliminarDetalle(detalle: DetallePedido) {
        repository.eliminarDetalle(detalle)
    }

    fun restaurarDetalle(detalle: DetallePedido) {
        repository.restaurarDetalle(detalle)
    }

    // ─── Confirmar ────────────────────────────────────────────────────────────

    fun confirmarPedido() {
        val id = _pedidoId.value ?: run {
            _uiState.value = UiState.Error("No hay pedido activo")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.confirmarPedido(id).fold(
                onSuccess = { _uiState.value = UiState.PedidoEnviado },
                onFailure = { e ->
                    _uiState.value = UiState.Error(e.message ?: "Error al confirmar")
                }
            )
        }
    }

    fun resetUiState() { _uiState.value = UiState.Idle }

    // ─── Factory ──────────────────────────────────────────────────────────────

    class Factory(
        private val repository: PedidoRepository,
        private val mozoId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PedidoViewModel::class.java))
            return PedidoViewModel(repository, mozoId) as T
        }
    }
}