package com.donabere.amm.utils

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

object BiometricUtils {
    private const val TAG = "BiometricUtils"

    /**
     * Verifica si el dispositivo tiene biometría disponible
     */
    fun isBiometricAvailable(context: Context): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            val canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                    or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando biometría: ${e.message}")
            false
        }
    }

    /**
     * Abre el diálogo de autenticación biométrica
     * @param activity Activity donde se mostrará el diálogo
     * @param title Título del diálogo
     * @param subtitle Subtítulo del diálogo
     * @param negativeButtonText Texto del botón negativo
     * @param callback Callback cuando se complete la autenticación
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        callback: BiometricCallback
    ) {
        try {
            val biometricPrompt = BiometricPrompt(
                activity,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        Log.d(TAG, "Autenticación biométrica exitosa")
                        callback.onAuthenticationSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Log.e(TAG, "Error biométrico: $errString")
                        callback.onAuthenticationError(errString.toString())
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Log.w(TAG, "Autenticación biométrica falló")
                        callback.onAuthenticationFailed()
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                )
                .build()

            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando BiometricPrompt: ${e.message}")
            callback.onAuthenticationError(e.message ?: "Error desconocido")
        }
    }

    interface BiometricCallback {
        fun onAuthenticationSuccess()
        fun onAuthenticationError(errorMessage: String)
        fun onAuthenticationFailed()
    }
}
