package com.donabere.amm.utils

import android.util.Log
import io.jsonwebtoken.Jwts
import java.security.PrivateKey
import java.util.*

object JWTSigner {
    /**
     * Genera un JWT firmado con la clave privada de huella
     * El JWT contiene:
     * - sub: email del usuario
     * - iat: timestamp de creación
     * - exp: expira en 5 minutos
     * - challenge: un identificador único
     */
    fun generateBiometricToken(email: String): String? {
        return try {
            Log.d("JWTSigner", "INICIANDO generación de token biométrico")
            
            val privateKey = KeystoreManager.getPrivateKey()
            Log.d("JWTSigner", "Clave privada obtenida: ${privateKey != null}")
            
            if (privateKey == null) {
                Log.e("JWTSigner", "❌ ERROR: No se encontró clave privada en Keystore")
                Log.e("JWTSigner", "¿Existe clave? ${KeystoreManager.hasFingerprintKey()}")
                return null
            }

            val now = Date()
            val expiryDate = Date(now.time + 5 * 60 * 1000) // 5 minutos

            Log.d("JWTSigner", "Construyendo JWT con email: $email")
            
            val token = Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("challenge", UUID.randomUUID().toString())
                .signWith(privateKey)
                .compact()

            Log.d("JWTSigner", "✓ Token biométrico generado exitosamente")
            token
        } catch (e: Exception) {
            Log.e("JWTSigner", "❌ ERROR CRÍTICO al generar token biométrico: ${e.message}")
            Log.e("JWTSigner", "Causa: ${e.cause}")
            e.printStackTrace()
            null
        }
    }
}
