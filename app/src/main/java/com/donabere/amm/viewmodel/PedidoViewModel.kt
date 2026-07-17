package com.donabere.amm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.Cuenta
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.repository.PedidoRepository
import kotlinx.coroutines.launch

class PedidoViewModel(
    private val repository: PedidoRepository,
    private val mozoId: String,
    private val mozoRepository: com.donabere.amm.repository.MozoRepository = com.donabere.amm.repository.MozoRepository()
) : ViewModel() {

    // ─── Estado UI ────────────────────────────────────────────────────────────
    sealed class UiState {
        object Idle                              : UiState()
        object Loading                           : UiState()
        object PedidoEnviado                     : UiState()  // Enviado con éxito
        object PedidoEnviadoPendienteSincronizar : UiState()  // Guardado, pendiente sincronización
        data class Success(val mensaje: String) : UiState()
        data class Error(val mensaje: String)   : UiState()
    }

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    // ─── Datos observables ────────────────────────────────────────────────────
    val detalles: LiveData<List<DetallePedido>> = repository.observarDetalles()
    val pedido:   LiveData<*>                   = repository.observarPedido()
    val estadoSincronizacion: LiveData<PedidoRepository.EstadoSincronizacion> =
        repository.observarEstadoSincronizacion()
    val alertaSincronizacion: LiveData<String?> = repository.observarAlertasSincronizacion()

    private val _pedidoId = MutableLiveData<String?>(null)
    val pedidoId: LiveData<String?> = _pedidoId

    private var mesasIds: List<String> = emptyList()
    private var envioEnCurso = false


    // ─── Inicializar ─────────────────────────────────────────────────────────

    fun iniciarMesa(mesasIds: List<String>)
    {
        this.mesasIds = mesasIds
    }



    // ─── Carrito ──────────────────────────────────────────────────────────────

    fun agregarProducto(
        productoId: String,
        nombreProducto: String,
        precioUnitario: Double,
        cantidad: Int = 1,
        nota: String = "",
        imagenProducto: String = "",
        hayInternet: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                repository.agregarItemACuenta(
                    productoId     = productoId,
                    nombreProducto = nombreProducto,
                    precioUnitario = precioUnitario,
                    cantidad       = cantidad,
                    nota           = nota,
                    imagenUrl      = imagenProducto,
                    validarStock   = hayInternet
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

    fun actualizarCantidad(
        detalle: DetallePedido,
        nuevaCantidad: Int,
        hayInternet: Boolean
    ) {

        viewModelScope.launch {

            if (hayInternet && nuevaCantidad > detalle.cantidad) {

                val stock = repository.obtenerStock(detalle.productoId)

                if (stock != null && nuevaCantidad > stock) {

                    _uiState.value =
                        UiState.Error("Stock máximo disponible: $stock")

                    return@launch
                }
            }

            repository.actualizarCantidadDetalle(
                detalle,
                nuevaCantidad
            )
        }
    }

    fun actualizarNota(
        detalle: DetallePedido,
        nuevaNota: String
    ) {

        viewModelScope.launch {

            repository.actualizarNotaDetalle(
                detalle,
                nuevaNota
            )
        }
    }
    fun eliminarDetalle(detalle: DetallePedido) {
        viewModelScope.launch {

            repository.eliminarDetalle(
                detalle
            )
        }
    }

    fun restaurarDetalle(detalle: DetallePedido) {
        viewModelScope.launch {

            repository.restaurarDetalle(
                detalle
            )
        }
    }

    // ─── Confirmar ────────────────────────────────────────────────────────────

    fun confirmarPedido(hayInternet: Boolean) {
        if (envioEnCurso) return
        envioEnCurso = true
        val mesas = mesasIds

        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val resultado = if (hayInternet) {
                repository.confirmarPedidoOnline(
                    mozoId = mozoId,
                    mesasIds = mesas
                )
            } else {
                repository.confirmarPedidoOffline(
                    mozoId = mozoId,
                    mesasIds = mesas
                )
            }

            resultado.fold(
                onSuccess = {
                    _uiState.value = if (hayInternet) {
                        UiState.PedidoEnviado
                    } else {
                        UiState.PedidoEnviadoPendienteSincronizar
                    }
                },
                onFailure = { e ->
                    _uiState.value = UiState.Error(e.message ?: "Error al confirmar")
                }
            )
            envioEnCurso = false
        }
    }

    fun obtenerCuentas(
        pedidoId: String,
        onResult: (List<Cuenta>) -> Unit
    ) {
        viewModelScope.launch {

            repository.obtenerCuentas(pedidoId).fold(
                onSuccess = { cuentas ->
                    onResult(cuentas)
                },
                onFailure = { e ->
                    _uiState.value = UiState.Error(
                        e.message ?: "Error cargando cuentas"
                    )
                }
            )
        }
    }

    fun pagarCuenta(
        pedidoId: String,
        cuentaId: String,
        onSuccess: () -> Unit
    ) {

        viewModelScope.launch {

            repository.pagarCuenta(pedidoId, cuentaId)
                .onSuccess {

                    onSuccess()
                }
        }
    }

    fun resetUiState() { _uiState.value = UiState.Idle }

    fun limpiarAlertaSincronizacion() {
        repository.limpiarAlertaSincronizacion()
    }

    // ─── HU 3.3 · Edición de pedido activo ───────────────────────────────────────

    fun modificarCantidadEnPedidoActivo(
        pedidoId: String,
        cuentaId: String,
        detalle: DetallePedido,
        nuevaCantidad: Int
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.modificarCantidadDetalleFirestore(
                pedidoId     = pedidoId,
                cuentaId     = cuentaId,
                detalle      = detalle,
                nuevaCantidad = nuevaCantidad
            ).fold(
                onSuccess = { _uiState.value = UiState.Success("Cantidad actualizada") },
                onFailure = { e -> _uiState.value = UiState.Error(e.message ?: "Error al modificar") }
            )
        }
    }

    fun agregarProductoAPedidoActivo(
        pedidoId: String,
        cuentaId: String,
        productoId: String,
        nombreProducto: String,
        precioUnitario: Double,
        cantidad: Int = 1,
        nota: String = "",
        imagenUrl: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.agregarDetalleAPedidoActivo(
                pedidoId       = pedidoId,
                cuentaId       = cuentaId,
                productoId     = productoId,
                nombreProducto = nombreProducto,
                precioUnitario = precioUnitario,
                cantidad       = cantidad,
                nota           = nota,
                imagenUrl      = imagenUrl
            ).fold(
                onSuccess = { _uiState.value = UiState.Success("Producto agregado") },
                onFailure = { e -> _uiState.value = UiState.Error(e.message ?: "Error al agregar") }
            )
        }
    }

    fun anularProductoEnPedidoActivo(
        pedidoId: String,
        cuentaId: String,
        detalle: DetallePedido,
        motivo: String
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.anularDetalleFirestore(
                pedidoId = pedidoId,
                cuentaId = cuentaId,
                detalle  = detalle,
                motivo   = motivo,
                mozoId   = mozoId
            ).fold(
                onSuccess = { _uiState.value = UiState.Success("Plato anulado") },
                onFailure = { e -> _uiState.value = UiState.Error(e.message ?: "Error al anular") }
            )
        }
    }

    fun transferirPedidoAMozo(pedidoId: String, mozoDestinoId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.transferirPedidoAMozo(pedidoId, mozoDestinoId).fold(
                onSuccess = {
                    _uiState.value = UiState.Success("Pedido transferido correctamente")
                    onSuccess()
                },
                onFailure = { e ->
                    _uiState.value = UiState.Error(e.message ?: "Error al transferir")
                }
            )
        }
    }

    fun obtenerMozosActivos(onResult: (List<com.donabere.amm.model.Mozo>) -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            mozoRepository.obtenerTodosLosMozos().fold(
                onSuccess = { mozos ->
                    _uiState.value = UiState.Idle
                    onResult(mozos)
                },
                onFailure = { e ->
                    _uiState.value = UiState.Error(e.message ?: "Error obteniendo mozos")
                }
            )
        }
    }

    // ─── Factory ──────────────────────────────────────────────────────────────

    class Factory(
        private val repository: PedidoRepository,
        private val mozoId: String = "",          // vacío cuando solo se edita
        private val mozoRepository: com.donabere.amm.repository.MozoRepository = com.donabere.amm.repository.MozoRepository()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PedidoViewModel::class.java))
            return PedidoViewModel(repository, mozoId, mozoRepository) as T
        }
    }
}