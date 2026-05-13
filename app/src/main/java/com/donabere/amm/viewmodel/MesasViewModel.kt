package com.donabere.amm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.response.MesaResponse
import com.donabere.amm.network.RetrofitClient
import com.donabere.amm.repository.MesasRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MesasViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MesasRepository(RetrofitClient.getApiService(application))
    private val mesasRef = FirebaseFirestore.getInstance().collection("mesas_estado")

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
                val mesasConEstado = list.map { mesa ->
                    val doc = mesasRef.document(mesa.id.toString()).get().await()
                    if (doc.exists() && doc.getString("estado") == "OCUPADA") {
                        mesa.copy(
                            status   = 1,
                            pedidoId = doc.getString("pedidoId")
                        )
                    } else {
                        mesa
                    }
                }
                _mesas.value = mesasConEstado
            }.onFailure { exception ->
                _error.value = "Error al obtener mesas: ${exception.message}"
            }
        }
    }

    fun actualizarEstadoMesaLocal(mesaId: Int, ocupada: Boolean) {
        val mesasActuales = _mesas.value?.toMutableList() ?: return
        val index = mesasActuales.indexOfFirst { it.id.equals(mesaId) }
        if (index != -1) {
            mesasActuales[index] = mesasActuales[index].copy(status = if (ocupada) 1 else 0)
            _mesas.value = mesasActuales
        }
    }


    private suspend fun obtenerEstadoMesaFirestore(mesaId: String): String {
        return try {
            val doc = mesasRef.document(mesaId).get().await()
            if (doc.exists()) doc.getString("estado") ?: "LIBRE"
            else "LIBRE"
        } catch (e: Exception) {
            "LIBRE"
        }
    }
}