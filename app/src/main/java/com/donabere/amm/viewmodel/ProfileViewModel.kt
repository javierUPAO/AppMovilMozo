package com.donabere.amm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.request.RegisterFingerprintRequest
import com.donabere.amm.network.RetrofitClient
import com.donabere.amm.repository.BiometricRepository
import com.donabere.amm.utils.JWTSigner
import com.donabere.amm.utils.KeystoreManager
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val biometricRepository = BiometricRepository(RetrofitClient.getApiService(application))
    private val keystoreManager = KeystoreManager
    private val jwtSigner = JWTSigner

    private val _fingerprintStatus = MutableLiveData<FingerprintStatus>()
    val fingerprintStatus: LiveData<FingerprintStatus> = _fingerprintStatus

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _success = MutableLiveData<String>()
    val success: LiveData<String> = _success

    init {
        checkFingerprintStatus()
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
                // 1. Generar clave RSA en Keystore
                val publicKeyPem = keystoreManager.generateFingerprintKeyPair()
                if (publicKeyPem == null) {
                    _error.value = "Error al generar clave de seguridad"
                    _isLoading.value = false
                    return@launch
                }

                // 2. Enviar al backend
                val request = RegisterFingerprintRequest(
                    email = email,
                    public_key = publicKeyPem
                )
                val result = biometricRepository.registerFingerprint(request)

                result.onSuccess { response ->
                    _success.value = "Huella registrada exitosamente"
                    _fingerprintStatus.value = FingerprintStatus.REGISTERED
                    _isLoading.value = false
                }

                result.onFailure { throwable ->
                    // Si falla en backend, eliminar la clave del Keystore
                    keystoreManager.deleteFingerprintKey()
                    _error.value = "Error: ${throwable.message}"
                    _fingerprintStatus.value = FingerprintStatus.NOT_REGISTERED
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                keystoreManager.deleteFingerprintKey()
                _error.value = "Error: ${e.message}"
                _fingerprintStatus.value = FingerprintStatus.NOT_REGISTERED
                _isLoading.value = false
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

    enum class FingerprintStatus {
        REGISTERED, NOT_REGISTERED
    }
}
