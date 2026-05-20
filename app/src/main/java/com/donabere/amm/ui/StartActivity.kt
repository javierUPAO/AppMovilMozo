package com.donabere.amm.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.donabere.amm.databinding.ActivityStartBinding
import com.donabere.amm.utils.KeystoreManager
import com.donabere.amm.utils.BiometricUtils
import com.donabere.amm.viewmodel.LoginViewModel
import com.google.firebase.firestore.FirebaseFirestore

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
        setupObservers()
    }

    private fun setupButtons() {
        // Botón: Loguarse con Credenciales
        binding.btnLoginCredentials.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("mode", "credentials") // Indica que es login por credenciales
            startActivity(intent)
        }

        // Botón: Loguarse con Huella
        binding.btnLoginBiometric.setOnClickListener {
            // Verificar si tiene huella registrada y capacidad biométrica
            if (KeystoreManager.hasFingerprintKey() && BiometricUtils.isBiometricAvailable(this)) {
                // Mostrar el prompt biométrico directamente aquí
                showBiometricPromptHere()
            } else {
                // No tiene huella registrada - mostrar opciones
                val mensaje = if (!BiometricUtils.isBiometricAvailable(this)) {
                    "Tu dispositivo no soporta autenticación biométrica"
                } else {
                    "No tienes huella registrada en tu dispositivo.\n\nVe a tu Perfil y registra tu huella primero."
                }
                
                android.app.AlertDialog.Builder(this)
                    .setTitle("Huella no registrada")
                    .setMessage(mensaje)
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .setCancelable(true)
                    .show()
            }
        }
    }

    private fun showBiometricPromptHere() {
        // Obtener el email guardado de SharedPreferences
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val email = sharedPreferences.getString("user_email", "")
        val usuarioId = sharedPreferences.getString("usuario_id", "")
        
        if (email.isNullOrBlank()) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("No hay email registrado. Debes iniciar sesión con credenciales primero.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
            return
        }

        // Mostrar el prompt biométrico directamente
        BiometricUtils.showBiometricPrompt(
            this,
            "Verificación de Huella",
            "Toca el sensor de huella para continuar",
            "Cancelar",
            object : BiometricUtils.BiometricCallback {
                override fun onAuthenticationSuccess() {
                    loginViewModel.biometricLogin(email, usuarioId ?: "")
                }

                override fun onAuthenticationError(errorMessage: String) {
                    val displayMessage = if (errorMessage.contains("Demasiados intentos", ignoreCase = true) || 
                                           errorMessage.contains("lockout", ignoreCase = true)) {
                        "Biometría bloqueada temporalmente.\n\n" +
                        "Por favor:\n" +
                        "1. Espera 30-60 segundos\n" +
                        "2. O reinicia tu teléfono\n" +
                        "3. O usa login con email y contraseña"
                    } else {
                        errorMessage
                    }
                    
                    android.app.AlertDialog.Builder(this@StartActivity)
                        .setTitle("Error de Biometría")
                        .setMessage(displayMessage)
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .show()
                }

                override fun onAuthenticationFailed() {
                    android.app.AlertDialog.Builder(this@StartActivity)
                        .setTitle("Huella no reconocida")
                        .setMessage("Asegúrate de:\n" +
                                   "1. Limpiar el sensor\n" +
                                   "2. Presionar bien el dedo\n" +
                                   "3. Verificar que tu huella esté registrada en Configuración")
                        .setPositiveButton("Reintentar") { dialog, _ -> 
                            dialog.dismiss()
                            showBiometricPromptHere()
                        }
                        .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            }
        )
    }

    private fun setupObservers() {
        loginViewModel.loginExitoso.observe(this) { exitoso ->
            if (exitoso) {
                val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
                val usuarioId = sharedPreferences.getString("usuario_id", "")
                
                Log.d("StartActivity", "Login exitoso. UsuarioId: $usuarioId")
                
                // Si NO tenemos usuario_id, algo está mal
                if (usuarioId.isNullOrBlank()) {
                    Log.e("StartActivity", "❌ ERROR: No hay usuario_id guardado")
                    android.app.AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("No se pudo obtener los datos de usuario. Intenta nuevamente.")
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .show()
                    return@observe
                }
                
                // SIEMPRE buscar mozoId fresco en Firestore (tanto para credenciales como para huella)
                Log.d("StartActivity", "Buscando mozoId para usuarioId: $usuarioId")
                FirebaseFirestore.getInstance().collection("mozo")
                    .whereEqualTo("usuarioId", usuarioId)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val mozoIdReal = documents.documents[0].id
                            sharedPreferences.edit().putString("mozoId", mozoIdReal).apply()
                            Log.d("StartActivity", "✓ MozoId encontrado y guardado: $mozoIdReal")
                        } else {
                            Log.e("StartActivity", "❌ No se encontró mozo con usuarioId: $usuarioId")
                        }
                        abrirMainActivity()
                    }
                    .addOnFailureListener { e ->
                        Log.e("StartActivity", "❌ Error en Firestore: ${e.message}")
                        e.printStackTrace()
                        abrirMainActivity()
                    }
            }
        }

        loginViewModel.error.observe(this) { error ->
            if (error.isNotEmpty()) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Error de Autenticación")
                    .setMessage(error)
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }
    
    private fun abrirMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
