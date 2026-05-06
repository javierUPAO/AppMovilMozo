package com.donabere.amm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.Bebida
import com.donabere.amm.network.RetrofitClient
import com.donabere.amm.repository.BebidasRepository
import kotlinx.coroutines.launch

class BebidasViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BebidasRepository(RetrofitClient.getApiService(application))

    private val _bebidas = MutableLiveData<List<Bebida>>()
    val bebidas: LiveData<List<Bebida>> = _bebidas

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun cargarBebidas() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.obtenerBebidas()
            _isLoading.value = false
            result.onSuccess {
                _bebidas.value = it
            }.onFailure {
                _error.value = it.message ?: "Error desconocido"
            }
        }
    }
}
