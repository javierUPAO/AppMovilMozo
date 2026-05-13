package com.donabere.amm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.repository.PedidoRepository
import kotlinx.coroutines.launch

class PedidoViewModel(
    private val repository: PedidoRepository,
    private val mozoId: String
) : ViewModel() {

    private val _pedidoId = MutableLiveData<String?>(null)
    val pedidoId: LiveData<String?> = _pedidoId

    val detalles: LiveData<List<DetallePedido>> = _pedidoId.switchMap { id ->
        if (id != null) repository.getDetallesByPedido(id)
        else MutableLiveData(emptyList())
    }

    sealed class UiState {
        object Idle        : UiState()
        object Loading     : UiState()
        object PedidoEnviado : UiState()
        data class Success(val mensaje: String) : UiState()
        data class Error(val mensaje: String)   : UiState()
    }

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    fun iniciarPedido(mesasIds: List<String>) {
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

    fun agregarProducto(
        mesasIds: List<String>,
        productoId: String,
        nombreProducto: String,
        precioUnitario: Double,
        cantidad: Int = 1,
        nota: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val id = _pedidoId.value
                    ?: repository.crearPedidoBorrador(mesasIds, mozoId)
                        .also { _pedidoId.value = it }

                val result = repository.agregarDetalle(
                    pedidoId       = id,
                    productoId     = productoId,
                    nombreProducto = nombreProducto,
                    precioUnitario = precioUnitario,
                    cantidad       = cantidad,
                    nota           = nota
                )

                result.fold(
                    onSuccess = { _uiState.value = UiState.Idle },
                    onFailure = { e ->
                        _uiState.value = UiState.Error(e.message ?: "Error al agregar producto")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error al agregar producto: ${e.message}")
            }
        }
    }

    fun actualizarCantidad(detalle: DetallePedido, nuevaCantidad: Int) {
        viewModelScope.launch {
            // Si está incrementando, validar stock antes de actualizar
            if (nuevaCantidad > detalle.cantidad) {
                val stock = repository.obtenerStock(detalle.productoId)
                if (stock != null && nuevaCantidad > stock) {
                    _uiState.value = UiState.Error(
                        "Stock insuficiente. Máximo disponible: $stock"
                    )
                    return@launch
                }
            }
            try {
                repository.actualizarCantidadDetalle(detalle, nuevaCantidad)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error al actualizar cantidad: ${e.message}")
            }
        }
    }

    fun actualizarNota(detalle: DetallePedido, nuevaNota: String) {
        viewModelScope.launch {
            try {
                repository.actualizarNotaDetalle(detalle, nuevaNota)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error al actualizar nota: ${e.message}")
            }
        }
    }

    fun eliminarDetalle(detalle: DetallePedido) {
        viewModelScope.launch {
            try {
                repository.eliminarDetalle(detalle)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error al eliminar producto: ${e.message}")
            }
        }
    }

    fun confirmarPedido() {
        val id = _pedidoId.value ?: run {
            _uiState.value = UiState.Error("No hay un pedido activo")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.confirmarPedido(id)
            result.fold(
                onSuccess = { _uiState.value = UiState.PedidoEnviado },
                onFailure = { e ->
                    _uiState.value = UiState.Error(e.message ?: "Error al confirmar pedido")
                }
            )
        }
    }

    fun resetUiState() {
        _uiState.value = UiState.Idle
    }

    class Factory(
        private val repository: PedidoRepository,
        private val mozoId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PedidoViewModel::class.java)) {
                return PedidoViewModel(repository, mozoId) as T
            }
            throw IllegalArgumentException("ViewModel desconocido")
        }
    }
}