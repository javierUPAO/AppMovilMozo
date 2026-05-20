package com.donabere.amm.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.Mozo
import com.donabere.amm.model.request.RegisterFingerprintRequest
import com.donabere.amm.network.RetrofitClient
import com.donabere.amm.repository.BiometricRepository
import com.donabere.amm.repository.MozoRepository
import com.donabere.amm.utils.KeystoreManager
import com.donabere.amm.utils.TokenManager
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val keystoreManager = KeystoreManager
    private val biometricRepository = BiometricRepository(RetrofitClient.getApiService(application))
    private val mozoRepository = MozoRepository()

    private var usuarioId: String = ""

    private val _fingerprintStatus = MutableLiveData<FingerprintStatus>()
    val fingerprintStatus: LiveData<FingerprintStatus> = _fingerprintStatus

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _success = MutableLiveData<String>()
    val success: LiveData<String> = _success

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    private val _mozo = MutableLiveData<Mozo>()
    val mozo: LiveData<Mozo> = _mozo

    init {
        checkFingerprintStatus()
    }

    fun cargarMozo() {
        viewModelScope.launch {
            val result = mozoRepository.obtenerMozoPorUsuarioId(usuarioId)

            result.onSuccess {
                _mozo.value = it
            }

            result.onFailure {
                _error.value = it.message
            }
        }
    }
    fun setUsuarioId(id: String) {
        usuarioId = id
    }

    fun checkFingerprintStatus() {
        if (keystoreManager.hasFingerprintKey()) {
            _fingerprintStatus.value = FingerprintStatus.REGISTERED
        } else {
            _fingerprintStatus.value = FingerprintStatus.NOT_REGISTERED
        }
    }

    fun registerFingerprint(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Limpiar huella anterior si existe
                if (keystoreManager.hasFingerprintKey()) {
                    keystoreManager.deleteFingerprintKey()
                    Log.d("ProfileViewModel", "Huella anterior eliminada")
                }
                
                // 2. Generar clave RSA en Keystore
                val publicKeyPem = keystoreManager.generateFingerprintKeyPair()
                if (publicKeyPem == null) {
                    _error.value = "Error al generar clave de seguridad"
                    _isLoading.value = false
                    return@launch
                }

                Log.d("ProfileViewModel", "Clave de huella generada localmente, enviando al backend...")
                
                // 3. Enviar clave pública al backend
                val request = RegisterFingerprintRequest(email, publicKeyPem)
                val result = biometricRepository.registerFingerprint(request)
                
                result.onSuccess { response ->
                    Log.d("ProfileViewModel", "✓ Huella registrada en backend exitosamente")
                    _success.value = "Huella registrada exitosamente"
                    _fingerprintStatus.value = FingerprintStatus.REGISTERED
                    _isLoading.value = false
                }.onFailure { exception ->
                    Log.e("ProfileViewModel", "❌ Error registrando en backend: ${exception.message}")
                    keystoreManager.deleteFingerprintKey()
                    _error.value = "Error al registrar huella: ${exception.message}"
                    _fingerprintStatus.value = FingerprintStatus.NOT_REGISTERED
                    _isLoading.value = false
                }
                
            } catch (e: Exception) {
                keystoreManager.deleteFingerprintKey()
                _error.value = "Error: ${e.message}"
                _fingerprintStatus.value = FingerprintStatus.NOT_REGISTERED
                _isLoading.value = false
                Log.e("ProfileViewModel", "Error registrando huella: ${e.message}")
            }
        }
    }

    fun deleteFingerprint(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Eliminar del dispositivo primero
                keystoreManager.deleteFingerprintKey()
                
                // Luego notificar al backend (para futuras referencias)
                // Por ahora solo eliminamos localmente
                _success.value = "Huella eliminada"
                _fingerprintStatus.value = FingerprintStatus.NOT_REGISTERED
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Error al eliminar: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun logout(context: Context) {
        viewModelScope.launch {

            TokenManager(context).clearToken()

            context.getSharedPreferences("app_prefs", 0)
                .edit()
                .clear()
                .apply()

            _fingerprintStatus.value = FingerprintStatus.NOT_REGISTERED
            _mozo.value = null

            _logoutEvent.value = true
        }
    }

    enum class FingerprintStatus {
        REGISTERED, NOT_REGISTERED
    }
}
