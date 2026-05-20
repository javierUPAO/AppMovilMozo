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

    private val repository       = MenuRepository(RetrofitClient.getApiService(application))
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
                val dishesConStockReal = dishes.map { dish ->
                    val stockFirestore = pedidoRepository.obtenerStock(dish.id)
                    // Si Firestore tiene el stock actualizado, usarlo; si no, usar el del backend
                    if (stockFirestore != null) dish.copy(stock = stockFirestore) else dish
                }
                _menu.value = dishesConStockReal
            }.onFailure {
                _error.value = it.message ?: "Error desconocido"
            }
        }
    }
}