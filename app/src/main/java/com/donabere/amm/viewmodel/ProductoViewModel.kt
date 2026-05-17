package com.donabere.amm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.Producto
import com.donabere.amm.model.enums.TipoProducto
import com.donabere.amm.repository.ProductoRepository
import com.donabere.amm.repository.PedidoRepository
import kotlinx.coroutines.launch

class ProductoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProductoRepository()
    private val pedidoRepository = PedidoRepository()

    private val _productos = MutableLiveData<List<Producto>>()
    val productos: LiveData<List<Producto>> = _productos

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun cargarProductosPorTipo(tipo: TipoProducto) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.obtenerProductos()
            result.onSuccess { productosCompletos ->
                val productosFiltrados = productosCompletos.filter { it.tipo == tipo }
                
                val productosConStockReal = productosFiltrados.map { producto ->
                    val stockFirestore = pedidoRepository.obtenerStock(producto.id)
                    if (stockFirestore == null) {
                        pedidoRepository.inicializarStockSiNoExiste(producto.id, producto.stock)
                        producto
                    } else {
                        producto.copy(stock = stockFirestore)
                    }
                }
                _productos.value = productosConStockReal
                _isLoading.value = false
            }.onFailure {
                _error.value = it.message ?: "Error desconocido"
                _isLoading.value = false
            }
        }
    }
}
