package com.donabere.amm.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.donabere.amm.databinding.ActivityLoginBinding
import com.donabere.amm.databinding.DialogRegisterBiometricBinding
import com.donabere.amm.viewmodel.LoginViewModel
import com.donabere.amm.viewmodel.BiometricRegisterViewModel
import com.donabere.amm.utils.BiometricUtils
import com.donabere.amm.utils.KeystoreManager

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels()
    private val biometricRegisterViewModel: BiometricRegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLoginButton()
        setupBiometricButton()
        setupObservers()
        
        // Mostrar botón de huella si ya está registrada
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
                startActivity(Intent(this, MesasActivity::class.java))
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

        loginViewModel.mostrarOpcionBiometria.observe(this) { mostrar ->
            if (mostrar) {
                mostrarDialogoRegistroBiometria()
            }
        }

        biometricRegisterViewModel.registerExitoso.observe(this) { exitoso ->
            if (exitoso) {
                loginViewModel.skipBiometricRegistration()
            }
        }

        biometricRegisterViewModel.error.observe(this) { error ->
            // Mostrar error pero permitir continuar
            binding.tvError.visibility = View.VISIBLE
            binding.tvError.text = error
        }
    }

    private fun mostrarDialogoRegistroBiometria() {
        val dialogBinding = DialogRegisterBiometricBinding.inflate(layoutInflater)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        // Verificar si existe huella local
        biometricRegisterViewModel.checkIfFingerprintKeyExists()
        val hasFingerprintKey = KeystoreManager.hasFingerprintKey()
        
        android.util.Log.d("BiometricDialog", "Has fingerprint key: $hasFingerprintKey")
        
        if (hasFingerprintKey) {
            // Mostrar opción de login con huella y borrar huella
            android.util.Log.d("BiometricDialog", "Showing delete button")
            dialogBinding.tvDialogTitle.text = "Opciones de Huella Dactilar"
            dialogBinding.tvDialogSubtitle.text = "Ya tienes una huella registrada"
            dialogBinding.btnLoginBiometric.visibility = View.VISIBLE
            dialogBinding.btnDeleteBiometric.visibility = View.VISIBLE
            dialogBinding.btnRegisterBiometric.text = "Registrar Nueva Huella"
        } else {
            // Solo mostrar opción de registrar huella
            android.util.Log.d("BiometricDialog", "Hiding delete button")
            dialogBinding.tvDialogTitle.text = "Registrar Huella Dactilar"
            dialogBinding.tvDialogSubtitle.text = "¿Deseas registrar tu huella dactilar para un acceso más rápido?"
            dialogBinding.btnLoginBiometric.visibility = View.GONE
            dialogBinding.btnDeleteBiometric.visibility = View.GONE
            dialogBinding.btnRegisterBiometric.text = "Registrar Huella"
        }

        // Botón: Loguear con Huella
        dialogBinding.btnLoginBiometric.setOnClickListener {
            dialogBinding.pbRegisterBiometric.visibility = View.VISIBLE
            dialogBinding.tvBiometricStatus.text = "Autenticándose..."
            dialogBinding.btnLoginBiometric.isEnabled = false
            dialogBinding.btnRegisterBiometric.isEnabled = false
            dialogBinding.btnDeleteBiometric.isEnabled = false
            
            BiometricUtils.showBiometricPrompt(
                this,
                "Autenticación Biométrica",
                "Toca el sensor para autenticarte",
                "Cancelar",
                object : BiometricUtils.BiometricCallback {
                    override fun onAuthenticationSuccess() {
                        val email = loginViewModel.userEmail.value ?: ""
                        dialogBinding.tvBiometricStatus.text = "Generando token..."
                        loginViewModel.biometricLogin(email)
                        dialog.dismiss()
                    }

                    override fun onAuthenticationError(errorMessage: String) {
                        dialogBinding.pbRegisterBiometric.visibility = View.GONE
                        dialogBinding.tvBiometricStatus.text = "Error: $errorMessage"
                        dialogBinding.btnLoginBiometric.isEnabled = true
                        dialogBinding.btnRegisterBiometric.isEnabled = true
                        dialogBinding.btnDeleteBiometric.isEnabled = true
                    }

                    override fun onAuthenticationFailed() {
                        dialogBinding.pbRegisterBiometric.visibility = View.GONE
                        dialogBinding.tvBiometricStatus.text = "Huella no reconocida"
                        dialogBinding.btnLoginBiometric.isEnabled = true
                        dialogBinding.btnRegisterBiometric.isEnabled = true
                        dialogBinding.btnDeleteBiometric.isEnabled = true
                    }
                }
            )
        }

        // Botón: Registrar Huella
        dialogBinding.btnRegisterBiometric.setOnClickListener {
            val email = loginViewModel.userEmail.value ?: ""
            
            dialogBinding.pbRegisterBiometric.visibility = View.VISIBLE
            dialogBinding.tvBiometricStatus.text = "Generando claves..."
            dialogBinding.btnRegisterBiometric.isEnabled = false
            dialogBinding.btnLoginBiometric.isEnabled = false
            dialogBinding.btnDeleteBiometric.isEnabled = false
            
            BiometricUtils.showBiometricPrompt(
                this,
                "Registrar Huella",
                "Toca el sensor para registrar tu huella",
                "Cancelar",
                object : BiometricUtils.BiometricCallback {
                    override fun onAuthenticationSuccess() {
                        dialogBinding.tvBiometricStatus.text = "Registrando en servidor..."
                        biometricRegisterViewModel.generateAndRegisterFingerprint(email)
                    }

                    override fun onAuthenticationError(errorMessage: String) {
                        dialogBinding.pbRegisterBiometric.visibility = View.GONE
                        dialogBinding.tvBiometricStatus.text = "Error: $errorMessage"
                        dialogBinding.btnRegisterBiometric.isEnabled = true
                        dialogBinding.btnLoginBiometric.isEnabled = true
                        dialogBinding.btnDeleteBiometric.isEnabled = true
                    }

                    override fun onAuthenticationFailed() {
                        dialogBinding.pbRegisterBiometric.visibility = View.GONE
                        dialogBinding.tvBiometricStatus.text = "Huella no reconocida"
                        dialogBinding.btnRegisterBiometric.isEnabled = true
                        dialogBinding.btnLoginBiometric.isEnabled = true
                        dialogBinding.btnDeleteBiometric.isEnabled = true
                    }
                }
            )
        }

        // Botón: Borrar Huella
        dialogBinding.btnDeleteBiometric.setOnClickListener {
            // Pedir confirmación con huella
            BiometricUtils.showBiometricPrompt(
                this,
                "Confirmación de Borrado",
                "Toca el sensor para confirmar que deseas borrar tu huella",
                "Cancelar",
                object : BiometricUtils.BiometricCallback {
                    override fun onAuthenticationSuccess() {
                        dialogBinding.pbRegisterBiometric.visibility = View.VISIBLE
                        dialogBinding.tvBiometricStatus.text = "Borrando huella..."
                        dialogBinding.btnRegisterBiometric.isEnabled = false
                        dialogBinding.btnLoginBiometric.isEnabled = false
                        dialogBinding.btnDeleteBiometric.isEnabled = false
                        
                        biometricRegisterViewModel.deleteFingerprintKey()
                        
                        dialogBinding.root.postDelayed({
                            dialogBinding.pbRegisterBiometric.visibility = View.GONE
                            dialogBinding.tvBiometricStatus.text = "Huella eliminada. Puedes registrar una nueva."
                            dialogBinding.btnLoginBiometric.visibility = View.GONE
                            dialogBinding.btnDeleteBiometric.visibility = View.GONE
                            dialogBinding.btnRegisterBiometric.isEnabled = true
                            dialogBinding.btnRegisterBiometric.text = "Registrar Huella"
                        }, 1000)
                    }

                    override fun onAuthenticationError(errorMessage: String) {
                        dialogBinding.tvBiometricStatus.text = "Error: $errorMessage"
                    }

                    override fun onAuthenticationFailed() {
                        dialogBinding.tvBiometricStatus.text = "Confirmación rechazada"
                    }
                }
            )
        }

        dialogBinding.btnSkipBiometric.setOnClickListener {
            dialog.dismiss()
            loginViewModel.skipBiometricRegistration()
        }

        biometricRegisterViewModel.registerExitoso.observe(this) {
            dialog.dismiss()
            loginViewModel.skipBiometricRegistration()
        }

        dialog.show()
    }
}