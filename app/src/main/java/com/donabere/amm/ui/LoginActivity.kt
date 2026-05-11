package com.donabere.amm.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.donabere.amm.MenuActivity
import com.donabere.amm.databinding.ActivityLoginBinding
import com.donabere.amm.viewmodel.LoginViewModel
import com.donabere.amm.utils.BiometricUtils
import com.donabere.amm.utils.KeystoreManager

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLoginButton()
        setupBiometricButton()
        setupObservers()

        // Mostrar botón de huella SOLO si ya está registrada
        if (loginViewModel.hasFingerprintRegistered() && loginViewModel.hasBiometricAvailable()) {
            binding.btnIngresarHuella.visibility = View.VISIBLE
        }
    }

    private fun setupLoginButton() {
        binding.btnIngresar.setOnClickListener {
            val email = binding.etUsuario.text.toString()
            val contrasena = binding.etContrasena.text.toString()
            binding.tvError.visibility = View.GONE
            loginViewModel.login(email, contrasena)
        }
    }

    private fun setupBiometricButton() {
        binding.btnIngresarHuella.setOnClickListener {
            val email = binding.etUsuario.text.toString()
            if (email.isBlank()) {
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = "Ingresa tu email primero"
                return@setOnClickListener
            }

            // Mostrar el prompt biométrico
            BiometricUtils.showBiometricPrompt(
                this,
                "Verificación de Huella",
                "Toca el sensor de huella para continuar",
                "Cancelar",
                object : BiometricUtils.BiometricCallback {
                    override fun onAuthenticationSuccess() {
                        loginViewModel.biometricLogin(email)
                    }

                    override fun onAuthenticationError(errorMessage: String) {
                        binding.tvError.visibility = View.VISIBLE
                        binding.tvError.text = "Error: $errorMessage"
                    }

                    override fun onAuthenticationFailed() {
                        binding.tvError.visibility = View.VISIBLE
                        binding.tvError.text = "Huella no reconocida. Intenta de nuevo"
                    }
                }
            )
        }
    }

    private fun setupObservers() {
        loginViewModel.loginExitoso.observe(this) { exitoso ->
            if (exitoso) {
                startActivity(Intent(this, MenuActivity::class.java))
                finish()
            }
        }

        loginViewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnIngresar.isEnabled = !loading
            binding.btnIngresarHuella.isEnabled = !loading
        }

        loginViewModel.error.observe(this) { mensaje ->
            binding.tvError.visibility = View.VISIBLE
            binding.tvError.text = mensaje
        }
    }
}