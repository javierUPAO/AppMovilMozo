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
            when (canAuthenticate) {
                BiometricManager.BIOMETRIC_SUCCESS -> true
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    Log.w(TAG, "Hardware biométrico no disponible")
                    false
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    Log.w(TAG, "Dispositivo sin sensor biométrico")
                    false
                }
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Log.w(TAG, "No hay huella registrada en el dispositivo")
                    false
                }
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                    Log.w(TAG, "Se requiere actualización de seguridad")
                    false
                }
                else -> {
                    Log.w(TAG, "Estado biométrico desconocido: $canAuthenticate")
                    false
                }
            }
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
                        Log.e(TAG, "Error biométrico (Code: $errorCode): $errString")
                        
                        // Mapear errores específicos
                        val messageToShow = when (errorCode) {
                            BiometricPrompt.ERROR_HW_NOT_PRESENT -> "Tu dispositivo no tiene sensor de huella"
                            BiometricPrompt.ERROR_HW_UNAVAILABLE -> "El sensor de huella no está disponible en este momento"
                            BiometricPrompt.ERROR_NO_BIOMETRICS -> "No hay huella registrada en el dispositivo"
                            BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> "Debes tener huella o PIN registrado"
                            BiometricPrompt.ERROR_LOCKOUT -> "Demasiados intentos fallidos. Espera 30 segundos e intenta de nuevo"
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "Biometría bloqueada. Debes reiniciar el dispositivo o reintentar después"
                            BiometricPrompt.ERROR_TIMEOUT -> "Tiempo de espera agotado. Intenta de nuevo"
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON -> "Cancelado por el usuario"
                            else -> errString.toString()
                        }
                        callback.onAuthenticationError(messageToShow)
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
