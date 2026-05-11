package com.donabere.amm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.Dish
import com.donabere.amm.network.RetrofitClient
import com.donabere.amm.repository.MenuRepository
import com.donabere.amm.repository.PedidoRepository
import kotlinx.coroutines.launch

class MenuViewModel(application: Application) : AndroidViewModel(application) {

    private val repository    = MenuRepository(RetrofitClient.getApiService(application))
    private val pedidoRepository = PedidoRepository()

    private val _menu = MutableLiveData<List<Dish>>()
    val menu: LiveData<List<Dish>> = _menu

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun cargarMenu() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.obtenerMenu()
            _isLoading.value = false
            result.onSuccess { dishes ->
                // Sincronizar stock con Firestore
                val dishesConStockReal = dishes.map { dish ->
                    // Si existe en Firestore, usar ese stock; si no, inicializar con el del backend
                    val stockFirestore = pedidoRepository.obtenerStock(dish.id)
                    if (stockFirestore == null) {
                        pedidoRepository.inicializarStockSiNoExiste(dish.id, dish.stock)
                        dish // primera vez → stock del backend
                    } else {
                        dish.copy(stock = stockFirestore) // ya existe → stock real
                    }
                }
                _menu.value = dishesConStockReal
            }.onFailure {
                _error.value = it.message ?: "Error desconocido"
            }
        }
    }
}