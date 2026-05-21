package com.donabere.amm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.donabere.amm.model.Pedido
import com.donabere.amm.model.enums.EstadoPedido
import com.donabere.amm.repository.PedidoRepository

class PedidosViewModel(private val repository: PedidoRepository) : ViewModel() {

    private val _pedidosOriginales = MutableLiveData<List<Pedido>>()
    private val _pedidosFiltrados = MutableLiveData<List<Pedido>>()
    
    val pedidos: LiveData<List<Pedido>> = _pedidosFiltrados

    private val _filtroActual = MutableLiveData<String>("TODO")
    val filtroActual: LiveData<String> = _filtroActual

    /**
     * Carga los pedidos del mozo del día actual
     */
    fun cargarPedidosDelDia(mozoId: String) {
        repository.obtenerPedidosDelMozoDiaActual(mozoId).observeForever { pedidos ->
            _pedidosOriginales.value = pedidos
            aplicarFiltro(_filtroActual.value ?: "TODO")
        }
    }

    /**
     * Aplica el filtro seleccionado
     */
    fun aplicarFiltro(filtro: String) {
        _filtroActual.value = filtro
        val pedidosOriginales = _pedidosOriginales.value ?: emptyList()
        
        val pedidosFiltrados = when (filtro) {
            "TODO" -> pedidosOriginales
            "PENDIENTE" -> pedidosOriginales.filter { it.estado == EstadoPedido.BORRADOR || it.estado == EstadoPedido.PENDIENTE_PREPARACION }
            "COCINA" -> pedidosOriginales.filter { it.estado == EstadoPedido.COCINA }
            "LISTO_ENT" -> pedidosOriginales.filter { it.estado == EstadoPedido.LISTO_PARA_ENTREGAR }
            "PAGADO" -> pedidosOriginales.filter { it.estado == EstadoPedido.PAGADO }
            "ATENDIDO" -> pedidosOriginales.filter { it.estado == EstadoPedido.ATENDIDO }
            else -> pedidosOriginales
        }
        
        _pedidosFiltrados.value = pedidosFiltrados
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val repository: PedidoRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PedidosViewModel(repository) as T
        }
    }
}
