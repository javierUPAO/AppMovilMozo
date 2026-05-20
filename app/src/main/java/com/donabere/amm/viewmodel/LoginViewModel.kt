package com.donabere.amm.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.request.LoginRequest
import com.donabere.amm.model.request.BiometricLoginRequest
import com.donabere.amm.model.response.AuthResponse
import com.donabere.amm.network.RetrofitClient
import com.donabere.amm.repository.AuthRepository
import com.donabere.amm.repository.BiometricRepository
import com.donabere.amm.utils.TokenManager
import com.donabere.amm.utils.KeystoreManager
import com.donabere.amm.utils.JWTSigner
import com.donabere.amm.utils.BiometricUtils
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(RetrofitClient.getApiService(application))
    private val biometricRepository = BiometricRepository(RetrofitClient.getApiService(application))
    private val tokenManager = TokenManager(application)
    private val app = application

    private val _authResponse = MutableLiveData<AuthResponse>()
    val authResponse: LiveData<AuthResponse> = _authResponse

    private val _loginExitoso = MutableLiveData<Boolean>()
    val loginExitoso: LiveData<Boolean> = _loginExitoso

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(email: String, contrasena: String) {
        if (email.isBlank() || contrasena.isBlank()) {
            _error.value = "Completa todos los campos"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val request = LoginRequest(email, contrasena)
            val result = authRepository.login(request)
            
            _isLoading.value = false
            
            result.onSuccess { response ->
                if (!response.token.isNullOrEmpty()) {
                    tokenManager.saveToken(response.token)

                    _authResponse.value = response

                    _loginExitoso.value = true
                } else {
                    _error.value = "No se recibió un token válido del servidor"
                }
            }.onFailure { exception ->
                _error.value = exception.message ?: "Error desconocido"
            }
        }
    }

    fun biometricLogin(email: String, usuarioId: String = "") {
        if (email.isBlank()) {
            _error.value = "Email requerido"
            return
        }

        _isLoading.value = true
        
        try {
            // 1. Generar JWT localmente con clave privada
            val biometricToken = JWTSigner.generateBiometricToken(email)
            
            if (biometricToken == null) {
                _isLoading.value = false
                _error.value = "Error al generar token biométrico"
                return
            }

            Log.d("LoginViewModel", "JWT biométrico generado, enviando al backend...")
            
            // 2. Enviar al backend para validación y obtener JWT de sesión
            viewModelScope.launch {
                val request = BiometricLoginRequest(email, biometricToken)
                val result = biometricRepository.biometricLogin(request)
                
                _isLoading.value = false
                
                result.onSuccess { response ->
                    if (!response.token.isNullOrEmpty()) {
                        Log.d("LoginViewModel", "✓ Login biométrico exitoso, JWT recibido del backend")
                        
                        // Guardar el JWT de sesión del backend (no el local)
                        tokenManager.saveToken(response.token)
                        
                        // Guardar email y usuario_id
                        val sharedPreferences = app.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit()
                            .putString("user_email", email)
                            .apply()
                        
                        if (usuarioId.isNotBlank()) {
                            sharedPreferences.edit()
                                .putString("usuario_id", usuarioId)
                                .apply()
                        }
                        
                        _authResponse.value = response
                        _loginExitoso.value = true
                    } else {
                        _error.value = "No se recibió un token válido del servidor"
                    }
                }.onFailure { exception ->
                    Log.e("LoginViewModel", "❌ Error en login biométrico: ${exception.message}")
                    _error.value = exception.message ?: "Error en login biométrico"
                }
            }
            
        } catch (e: Exception) {
            _isLoading.value = false
            Log.e("LoginViewModel", "❌ Excepción en login biométrico: ${e.message}")
            _error.value = "Error en login biométrico: ${e.message}"
        }
    }

    fun hasBiometricAvailable(): Boolean {
        return BiometricUtils.isBiometricAvailable(getApplication())
    }

    fun hasFingerprintRegistered(): Boolean {
        return KeystoreManager.hasFingerprintKey()
    }
}