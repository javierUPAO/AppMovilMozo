package com.donabere.amm.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.donabere.amm.databinding.ActivityStartBinding
import com.donabere.amm.utils.KeystoreManager
import com.donabere.amm.utils.BiometricUtils

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
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
                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("mode", "biometric") // Indica que es login biométrico
                startActivity(intent)
            } else {
                // No tiene huella registrada
                android.app.AlertDialog.Builder(this)
                    .setTitle("Huella no registrada")
                    .setMessage("Primero debes registrar tu huella. Por favor, inicia sesión con credenciales.")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }
}
