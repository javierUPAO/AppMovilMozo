package com.donabere.amm.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.request.RegisterFingerprintRequest
import com.donabere.amm.network.RetrofitClient
import com.donabere.amm.repository.BiometricRepository
import com.donabere.amm.utils.KeystoreManager
import kotlinx.coroutines.launch

class BiometricRegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val biometricRepository = BiometricRepository(RetrofitClient.getApiService(application))

    private val _registerExitoso = MutableLiveData<Boolean>()
    val registerExitoso: LiveData<Boolean> = _registerExitoso

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _publicKey = MutableLiveData<String>()
    val publicKey: LiveData<String> = _publicKey
    
    private val _hasFingerprintKey = MutableLiveData<Boolean>()
    val hasFingerprintKey: LiveData<Boolean> = _hasFingerprintKey
    
    private val _deletionExitosa = MutableLiveData<Boolean>()
    val deletionExitosa: LiveData<Boolean> = _deletionExitosa

    init {
        checkIfFingerprintKeyExists()
    }
    
    fun checkIfFingerprintKeyExists() {
        _hasFingerprintKey.value = KeystoreManager.hasFingerprintKey()
    }

    fun generateAndRegisterFingerprint(email: String) {
        _isLoading.value = true

        try {
            // Generar par de claves en el Keystore
            val publicKeyPem = KeystoreManager.generateFingerprintKeyPair()
            
            if (publicKeyPem == null) {
                _isLoading.value = false
                _error.value = "Error al generar claves de huella"
                return
            }

            _publicKey.value = publicKeyPem

            // Enviar la clave pública al backend
            sendPublicKeyToBackend(email, publicKeyPem)
        } catch (e: Exception) {
            _isLoading.value = false
            _error.value = "Error: ${e.message}"
            Log.e("BiometricRegister", "Error al registrar huella: ${e.message}")
        }
    }

    private fun sendPublicKeyToBackend(email: String, publicKey: String) {
        viewModelScope.launch {
            try {
                val request = RegisterFingerprintRequest(email, publicKey)
                val result = biometricRepository.registerFingerprint(request)

                _isLoading.value = false

                result.onSuccess { response ->
                    Log.d("BiometricRegister", "Huella registrada exitosamente")
                    _registerExitoso.value = true
                }.onFailure { exception ->
                    _error.value = "Error al guardar en servidor: ${exception.message}"
                    // Aunque falle el servidor, la clave ya está en el dispositivo
                    Log.e("BiometricRegister", "Error enviando clave: ${exception.message}")
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Error de conexión: ${e.message}"
                Log.e("BiometricRegister", "Error en corrutina: ${e.message}")
            }
        }
    }

    fun deleteFingerprintKey() {
        try {
            val deleted = KeystoreManager.deleteFingerprintKey()
            if (deleted) {
                _deletionExitosa.value = true
                _hasFingerprintKey.value = false
                _error.value = "Huella borrada exitosamente"
                Log.d("BiometricRegister", "Huella local eliminada")
            } else {
                _error.value = "Error al borrar la huella"
            }
        } catch (e: Exception) {
            _error.value = "Error: ${e.message}"
            Log.e("BiometricRegister", "Error borrando huella: ${e.message}")
        }
    }
}
