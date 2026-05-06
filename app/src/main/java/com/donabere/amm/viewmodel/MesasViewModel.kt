package com.donabere.amm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.response.MesaResponse
import com.donabere.amm.network.RetrofitClient
import com.donabere.amm.repository.MesasRepository
import kotlinx.coroutines.launch

class MesasViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MesasRepository(RetrofitClient.getApiService(application))

    private val _mesas = MutableLiveData<List<MesaResponse>>()
    val mesas: LiveData<List<MesaResponse>> = _mesas

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun fetchMesas() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.getMesas()
            _isLoading.value = false
            
            result.onSuccess { list ->
                _mesas.value = list
            }.onFailure { exception ->
                _error.value = "Error al obtener mesas: ${exception.message}"
            }
        }
    }
}
