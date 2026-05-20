package com.donabere.amm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.Producto
import com.donabere.amm.model.enums.TipoProducto
import com.donabere.amm.repository.ProductoRepository
import kotlinx.coroutines.launch

class ProductoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProductoRepository()

    private val _productos  = MutableLiveData<List<Producto>>()
    val productos: LiveData<List<Producto>> = _productos

    private val _isLoading  = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error      = MutableLiveData("")
    val error: LiveData<String> = _error

    fun cargarProductosPorTipo(tipo: TipoProducto) {
        _isLoading.value = true
        viewModelScope.launch {
            repository.obtenerProductos()
                .onSuccess { todos ->
                    _productos.value = todos.filter { it.tipo == tipo }
                    _isLoading.value = false
                }
                .onFailure { e ->
                    _error.value = e.message ?: "Error al cargar productos"
                    _isLoading.value = false
                }
        }
    }
}