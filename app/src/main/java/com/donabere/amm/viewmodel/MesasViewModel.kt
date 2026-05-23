package com.donabere.amm.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.Mesa
import com.donabere.amm.network.RetrofitClient
import com.donabere.amm.repository.MesasRepository
import com.donabere.amm.repository.MozoRepository
import kotlinx.coroutines.launch

class MesasViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService      = RetrofitClient.getApiService(application)
    private val mesasRepository = MesasRepository()
    private val mozoRepository  = MozoRepository()

    // ── mozoId: tanto como campo directo (para leer rápido) como LiveData ──
    var mozoIdRecuperado: String = ""

    private val _mozoIdLiveData = MutableLiveData<String>("")
    val mozoIdLiveData: LiveData<String> = _mozoIdLiveData

    // ── Mesas ────────────────────────────────────────────────────────────────
    private val _mesas     = MutableLiveData<List<Mesa>>()
    val mesas: LiveData<List<Mesa>> = _mesas

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error     = MutableLiveData("")
    val error: LiveData<String> = _error

    // ── Cargar mozoId (solo si no viene de prefs) ────────────────────────────

    fun cargarMozoId(usuarioId: String) {
        if (usuarioId.isEmpty() || mozoIdRecuperado.isNotEmpty()) return

        viewModelScope.launch {
            mozoRepository.obtenerMozoPorUsuarioId(usuarioId)
                .onSuccess { mozo ->
                    mozoIdRecuperado = mozo.id
                    _mozoIdLiveData.postValue(mozo.id)
                    Log.d("MesasViewModel", "mozoId cargado: ${mozo.id}")
                }
                .onFailure { e ->
                    Log.e("MesasViewModel", "Error cargando mozo: ${e.message}")
                }
        }
    }

    // Permite precargar directamente desde prefs sin hacer red
    fun setMozoId(id: String) {
        if (id.isNotEmpty()) {
            mozoIdRecuperado = id
            _mozoIdLiveData.value = id
        }
    }

    // ── Mesas ─────────────────────────────────────────────────────────────────

    fun fetchMesas() {
        viewModelScope.launch {
            _isLoading.value = true
            mesasRepository.getMesas()
                .onSuccess { lista ->
                    _mesas.value = lista
                    _isLoading.value = false
                }
                .onFailure { e ->
                    _error.value = e.message ?: "Error al cargar mesas"
                    _isLoading.value = false
                }
        }
    }
}