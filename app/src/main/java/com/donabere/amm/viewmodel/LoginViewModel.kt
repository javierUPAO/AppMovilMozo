package com.donabere.amm.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.donabere.amm.model.request.LoginRequest
import com.donabere.amm.network.RetrofitClient
import com.donabere.amm.repository.AuthRepository
import com.donabere.amm.utils.TokenManager
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(RetrofitClient.getApiService(application))
    private val tokenManager = TokenManager(application)

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
                    _loginExitoso.value = true
                } else {
                    _error.value = "No se recibió un token válido del servidor"
                }
            }.onFailure { exception ->
                _error.value = exception.message ?: "Error desconocido"
            }
        }
    }
}