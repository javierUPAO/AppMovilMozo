package com.donabere.amm.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.donabere.amm.databinding.ActivityLoginBinding
import com.donabere.amm.viewmodel.LoginViewModel
import com.donabere.amm.utils.BiometricUtils
import com.donabere.amm.utils.KeystoreManager
import androidx.core.content.edit

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels()
    private var loginMode = "credentials" // Por defecto es credenciales

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener el modo de login del Intent
        loginMode = intent.getStringExtra("mode") ?: "credentials"

        // Si es modo biométrico, verificar que tenga huella
        if (loginMode == "biometric") {
            if (!loginViewModel.hasFingerprintRegistered() || !loginViewModel.hasBiometricAvailable()) {
                // No tiene huella, mostrar alerta y volver
                android.app.AlertDialog.Builder(this)
                    .setTitle("Huella no registrada")
                    .setMessage("No tienes huella registrada. Por favor inicia sesión con credenciales.")
                    .setPositiveButton("OK") { _, _ ->
                        finish() // Volver a StartActivity
                    }
                    .setCancelable(false)
                    .show()
                return
            }
        }

        setupLoginButton()
        setupBiometricButton()
        setupObservers()

        // Configurar visibilidad según el modo
        if (loginMode == "biometric") {
            // Modo biométrico: mostrar solo email y botón de huella
            binding.btnIngresarHuella.visibility = View.VISIBLE
            binding.etContrasena.visibility = View.GONE
            binding.btnIngresar.visibility = View.GONE
        } else {
            // Modo credenciales: mostrar todo
            binding.btnIngresarHuella.visibility = View.GONE
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

        loginViewModel.authResponse.observe(this) { response ->

            val sharedPreferences = getSharedPreferences("app_prefs", 0)

            sharedPreferences.edit {
                putString("user_email", binding.etUsuario.text.toString())
                    .putString("usuario_id", response.id.toString())
            }

            startActivity(Intent(this, MainActivity::class.java))
            finish()
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