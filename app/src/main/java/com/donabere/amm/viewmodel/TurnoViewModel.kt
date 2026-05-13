package com.donabere.amm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.Turno
import com.donabere.amm.repository.TurnoRepository
import kotlinx.coroutines.launch

class TurnoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TurnoRepository()

    private val _turnoActivo = MutableLiveData<Turno?>()
    val turnoActivo: LiveData<Turno?> = _turnoActivo

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error


    /**
     * Consulta si el mozo tiene turno abierto
     */
    fun verificarTurno(mozoId: String) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val turno = repository.obtenerTurnoActivoPorMozo(mozoId)
                _turnoActivo.value = turno
                _error.value = null
            } catch (e: Exception) {
                _turnoActivo.value = null
                _error.value = e.message ?: "Error al verificar turno"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun abrirTurno(mozoId: String) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                repository.abrirTurnoSiNoExiste(mozoId)
                verificarTurno(mozoId) // refresca estado
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al abrir turno"
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun cerrarTurno(mozoId: String) {
        val turno = _turnoActivo.value ?: return

        _isLoading.value = true

        viewModelScope.launch {
            try {

                val resumen = repository.obtenerResumenPorMozo(mozoId)

                repository.cerrarTurnoConResumen(turno.id, resumen)

                verificarTurno(mozoId)

            } catch (e: Exception) {
                _error.value = e.message ?: "Error al cerrar turno"
            } finally {
                _isLoading.value = false
            }
        }
    }



    fun tieneTurnoActivo(): Boolean {
        return _turnoActivo.value != null
    }

    fun tieneTurnoActivoSafe(): Boolean {
        return _turnoActivo.value?.estado?.name == "ABIERTO"
    }

    fun getTurnoActual(): Turno? {
        return _turnoActivo.value
    }
}