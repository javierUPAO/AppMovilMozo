package com.donabere.amm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {

    private val _loginExitoso = MutableLiveData<Boolean>()
    val loginExitoso: LiveData<Boolean> = _loginExitoso

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Usuario de prueba luego esto vendrá del backend
    private val usuarioDemo = "mozo1"
    private val contrasenaDemo = "1234"

    fun login(usuario: String, contrasena: String) {
        if (usuario.isBlank() || contrasena.isBlank()) {
            _error.value = "Completa todos los campos"
            return
        }
        if (usuario == usuarioDemo && contrasena == contrasenaDemo) {
            _loginExitoso.value = true
        } else {
            _error.value = "Usuario o contraseña incorrectos"
        }
    }
}