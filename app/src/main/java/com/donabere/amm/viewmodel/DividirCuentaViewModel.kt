package com.donabere.amm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.model.ui.CuentaDivisionUi
import com.donabere.amm.model.ui.ProductoDividirUi
import com.donabere.amm.repository.PedidoRepository
import kotlinx.coroutines.launch

class DividirCuentaViewModel(
    private val repository: PedidoRepository
) : ViewModel() {

    private val _productos = MutableLiveData<List<ProductoDividirUi>>(emptyList())
    val productos: LiveData<List<ProductoDividirUi>> = _productos

    private val _cuentas = MutableLiveData<List<CuentaDivisionUi>>(emptyList())
    val cuentas: LiveData<List<CuentaDivisionUi>> = _cuentas

    private val _uiState = MutableLiveData<String?>()
    val uiState: LiveData<String?> = _uiState

    private var pedidoId: String = ""

    private var contadorCuentas = 1

    private var ultimoEliminado: Pair<String, DetallePedido>? = null

    fun cargarPedido(pedidoId: String) {
        this.pedidoId = pedidoId

        viewModelScope.launch {
            repository.obtenerEstadoDivisionCuenta(pedidoId).fold(
                onSuccess = { estado ->

                    _productos.value = estado.productos
                    _cuentas.value = estado.cuentas

                    contadorCuentas = estado.cuentas
                        .mapNotNull { cuenta ->
                            cuenta.id.removePrefix("cuenta_").toIntOrNull()
                        }
                        .maxOrNull()
                        ?: estado.cuentas.size
                },
                onFailure = {
                    _uiState.value = it.message ?: "Error al cargar división"
                }
            )
        }
    }

    fun agregarCuenta() {
        val cuentasActuales = _cuentas.value.orEmpty()

        var nuevoId: String

        do {
            contadorCuentas++
            nuevoId = "cuenta_$contadorCuentas"
        } while (cuentasActuales.any { it.id == nuevoId })

        val nuevaCuenta = CuentaDivisionUi(
            id = nuevoId,
            nombre = "Cuenta $contadorCuentas",
            detalles = emptyList()
        )

        _cuentas.value = cuentasActuales + nuevaCuenta
    }

    fun asignarProductoACuenta(
        productoKey: String,
        cuentaId: String,
        cantidad: Int? = null
    ) {
        val productosActuales = _productos.value.orEmpty().toMutableList()
        val cuentasActuales = _cuentas.value.orEmpty().toMutableList()

        val indexProducto = productosActuales.indexOfFirst { it.key == productoKey }
        if (indexProducto == -1) return

        val producto = productosActuales[indexProducto]

        if (producto.cantidadDisponible <= 0) return

        val cantidadAsignar = cantidad ?: producto.cantidadDisponible

        if (cantidadAsignar <= 0 || cantidadAsignar > producto.cantidadDisponible) return

        val indexCuenta = cuentasActuales.indexOfFirst { it.id == cuentaId }
        if (indexCuenta == -1) return

        val cuenta = cuentasActuales[indexCuenta]

        val detalleExistente = cuenta.detalles.find {
            repository.generarKeyDivision(
                productoId = it.productoId,
                precioUnitario = it.precioUnitario,
                nota = it.nota
            ) == productoKey && !it.anulado
        }

        val nuevosDetalles = if (detalleExistente != null) {
            cuenta.detalles.map { detalle ->
                if (detalle.id == detalleExistente.id) {
                    detalle.copy(
                        cantidad = detalle.cantidad + cantidadAsignar
                    )
                } else {
                    detalle
                }
            }
        } else {
            cuenta.detalles + DetallePedido(
                id = "tmp_${System.currentTimeMillis()}",
                productoId = producto.productoId,
                nombreProducto = producto.nombreProducto,
                precioUnitario = producto.precioUnitario,
                cantidad = cantidadAsignar,
                nota = producto.nota,
                imagenProducto = producto.imagenProducto,
                anulado = false,
                motivoAnulacion = "",
                cuentaId = cuentaId
            )
        }

        cuentasActuales[indexCuenta] = cuenta.copy(
            detalles = nuevosDetalles
        )

        productosActuales[indexProducto] = producto.copy(
            cantidadDisponible = producto.cantidadDisponible - cantidadAsignar
        )

        _productos.value = productosActuales
        _cuentas.value = cuentasActuales
    }

    fun incrementarDetalle(
        cuentaId: String,
        detalle: DetallePedido
    ) {
        val key = repository.generarKeyDivision(
            productoId = detalle.productoId,
            precioUnitario = detalle.precioUnitario,
            nota = detalle.nota
        )

        asignarProductoACuenta(
            productoKey = key,
            cuentaId = cuentaId,
            cantidad = 1
        )
    }

    fun decrementarDetalle(
        cuentaId: String,
        detalle: DetallePedido
    ) {
        val cuentasActuales = _cuentas.value.orEmpty().toMutableList()
        val productosActuales = _productos.value.orEmpty().toMutableList()

        val indexCuenta = cuentasActuales.indexOfFirst { it.id == cuentaId }
        if (indexCuenta == -1) return

        val cuenta = cuentasActuales[indexCuenta]

        val key = repository.generarKeyDivision(
            productoId = detalle.productoId,
            precioUnitario = detalle.precioUnitario,
            nota = detalle.nota
        )

        val nuevosDetalles = cuenta.detalles.mapNotNull { item ->

            if (item.id == detalle.id) {
                if (item.cantidad <= 1) {
                    null
                } else {
                    item.copy(cantidad = item.cantidad - 1)
                }
            } else {
                item
            }
        }

        cuentasActuales[indexCuenta] = cuenta.copy(
            detalles = nuevosDetalles
        )

        val indexProducto = productosActuales.indexOfFirst { it.key == key }
        if (indexProducto != -1) {
            val producto = productosActuales[indexProducto]
            productosActuales[indexProducto] = producto.copy(
                cantidadDisponible = producto.cantidadDisponible + 1
            )
        }

        _cuentas.value = cuentasActuales
        _productos.value = productosActuales
    }

    fun quitarDetalleDeCuenta(
        cuentaId: String,
        detalle: DetallePedido
    ) {
        val cuentasActuales = _cuentas.value.orEmpty().toMutableList()
        val productosActuales = _productos.value.orEmpty().toMutableList()

        val indexCuenta = cuentasActuales.indexOfFirst { it.id == cuentaId }
        if (indexCuenta == -1) return

        val cuenta = cuentasActuales[indexCuenta]

        val key = repository.generarKeyDivision(
            productoId = detalle.productoId,
            precioUnitario = detalle.precioUnitario,
            nota = detalle.nota
        )

        cuentasActuales[indexCuenta] = cuenta.copy(
            detalles = cuenta.detalles.filter { it.id != detalle.id }
        )

        val indexProducto = productosActuales.indexOfFirst { it.key == key }
        if (indexProducto != -1) {
            val producto = productosActuales[indexProducto]
            productosActuales[indexProducto] = producto.copy(
                cantidadDisponible = producto.cantidadDisponible + detalle.cantidad
            )
        }

        ultimoEliminado = cuentaId to detalle

        _cuentas.value = cuentasActuales
        _productos.value = productosActuales
    }

    fun deshacerEliminacion() {
        val eliminado = ultimoEliminado ?: return

        val cuentaId = eliminado.first
        val detalle = eliminado.second

        asignarProductoACuenta(
            productoKey = repository.generarKeyDivision(
                productoId = detalle.productoId,
                precioUnitario = detalle.precioUnitario,
                nota = detalle.nota
            ),
            cuentaId = cuentaId,
            cantidad = detalle.cantidad
        )

        ultimoEliminado = null
    }

    fun eliminarCuenta(cuentaId: String) {
        val cuentasActuales = _cuentas.value.orEmpty()

        if (cuentasActuales.size <= 1) {
            _uiState.value = "Debe existir al menos una cuenta"
            return
        }

        val cuentaEliminada = cuentasActuales.find { it.id == cuentaId } ?: return

        val productosActuales = _productos.value.orEmpty().toMutableList()

        cuentaEliminada.detalles
            .filter { !it.anulado }
            .forEach { detalle ->

                val key = repository.generarKeyDivision(
                    productoId = detalle.productoId,
                    precioUnitario = detalle.precioUnitario,
                    nota = detalle.nota
                )

                val indexProducto = productosActuales.indexOfFirst { it.key == key }

                if (indexProducto != -1) {
                    val producto = productosActuales[indexProducto]

                    productosActuales[indexProducto] = producto.copy(
                        cantidadDisponible = producto.cantidadDisponible + detalle.cantidad
                    )
                }
            }

        _productos.value = productosActuales

        _cuentas.value = cuentasActuales.filter { it.id != cuentaId }
    }

    fun guardarDivision() {
        val cuentasActuales = _cuentas.value.orEmpty()

        if (cuentasActuales.isEmpty()) {
            _uiState.value = "Debe existir al menos una cuenta"
            return
        }

        if (cuentasActuales.size == 1) {
            asignarProductosRestantesACuentaUnica()
        }

        val productosPendientes = _productos.value.orEmpty()
            .filter { it.cantidadDisponible > 0 }

        if (productosPendientes.isNotEmpty()) {
            _uiState.value = "Aún hay productos sin asignar"
            return
        }

        val cuentasValidas = _cuentas.value.orEmpty()
            .filter { cuenta ->
                cuenta.detalles.any { !it.anulado && it.cantidad > 0 }
            }

        if (cuentasValidas.isEmpty()) {
            _uiState.value = "No hay productos asignados"
            return
        }

        viewModelScope.launch {
            repository.guardarDivisionCuentas(
                pedidoId = pedidoId,
                cuentasDivididas = cuentasValidas
            ).fold(
                onSuccess = {
                    _uiState.value = "División guardada"
                },
                onFailure = {
                    _uiState.value = it.message ?: "Error al guardar división"
                }
            )
        }
    }

    private fun asignarProductosRestantesACuentaUnica() {
        val cuentasActuales = _cuentas.value.orEmpty()

        if (cuentasActuales.size != 1) return

        val cuenta = cuentasActuales.first()
        val productosActuales = _productos.value.orEmpty().toMutableList()
        val detallesCuenta = cuenta.detalles.toMutableList()

        productosActuales.forEachIndexed { index, producto ->

            if (producto.cantidadDisponible <= 0) return@forEachIndexed

            val existenteIndex = detallesCuenta.indexOfFirst { detalle ->
                repository.generarKeyDivision(
                    productoId = detalle.productoId,
                    precioUnitario = detalle.precioUnitario,
                    nota = detalle.nota
                ) == producto.key && !detalle.anulado
            }

            if (existenteIndex != -1) {
                val existente = detallesCuenta[existenteIndex]

                detallesCuenta[existenteIndex] = existente.copy(
                    cantidad = existente.cantidad + producto.cantidadDisponible
                )
            } else {
                detallesCuenta.add(
                    DetallePedido(
                        id = "tmp_${System.currentTimeMillis()}_$index",
                        productoId = producto.productoId,
                        nombreProducto = producto.nombreProducto,
                        precioUnitario = producto.precioUnitario,
                        cantidad = producto.cantidadDisponible,
                        nota = producto.nota,
                        imagenProducto = producto.imagenProducto,
                        anulado = false,
                        motivoAnulacion = "",
                        cuentaId = cuenta.id
                    )
                )
            }

            productosActuales[index] = producto.copy(
                cantidadDisponible = 0
            )
        }

        _productos.value = productosActuales

        _cuentas.value = listOf(
            cuenta.copy(
                detalles = detallesCuenta
            )
        )
    }

    class Factory(
        private val repository: PedidoRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DividirCuentaViewModel(repository) as T
        }
    }
}