package com.donabere.amm.utils

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.RSAKeyGenParameterSpec
import javax.crypto.Cipher
import kotlin.math.pow

object KeystoreManager {
    private const val ALIAS_FINGERPRINT = "fingerprint_key"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val SIGNATURE_ALGORITHM = "SHA256withRSA"

    /**
     * Genera un par de claves RSA 2048 en el Android Keystore
     * @return Par de claves públicas (público como String en formato PEM)
     */
    fun generateFingerprintKeyPair(): String? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)

            // Si ya existe la clave, la borramos para generar una nueva
            if (keyStore.containsAlias(ALIAS_FINGERPRINT)) {
                keyStore.deleteEntry(ALIAS_FINGERPRINT)
            }

            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                KEYSTORE_PROVIDER
            )

            val keyGenSpec = KeyGenParameterSpec.Builder(
                ALIAS_FINGERPRINT,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setKeySize(2048)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(true)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // 300000 ms = 5 minutos de timeout después de autenticarse
                        setUserAuthenticationParameters(300000, KeyProperties.AUTH_BIOMETRIC_STRONG)
                    }
                }
                .build()

            keyPairGenerator.initialize(keyGenSpec)
            val keyPair = keyPairGenerator.generateKeyPair()

            // Convertir la clave pública a formato PEM
            publicKeyToPem(keyPair.public)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Obtiene la clave privada del Keystore para firmar
     */
    fun getPrivateKey(): PrivateKey? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            keyStore.getKey(ALIAS_FINGERPRINT, null) as? PrivateKey
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Crea un Cipher para operaciones criptográficas con autenticación biométrica
     */
    fun initSignatureCipher(): Cipher? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            val privateKey = keyStore.getKey(ALIAS_FINGERPRINT, null) as? PrivateKey
            
            if (privateKey == null) {
                return null
            }

            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, privateKey)
            cipher
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Firma datos usando la Signature API con la clave privada autenticada
     */
    fun signData(data: ByteArray): ByteArray? {
        return try {
            val privateKey = getPrivateKey() ?: return null
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initSign(privateKey)
            signature.update(data)
            signature.sign()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Obtiene la clave pública como String en formato PEM
     */
    fun getPublicKeyPem(): String? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            val certificate = keyStore.getCertificate(ALIAS_FINGERPRINT)
            val publicKey = certificate?.publicKey ?: return null
            publicKeyToPem(publicKey)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Verifica si ya existe una clave de huella registrada
     */
    fun hasFingerprintKey(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            keyStore.containsAlias(ALIAS_FINGERPRINT)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Convierte una clave pública a formato PEM (String)
     */
    private fun publicKeyToPem(publicKey: PublicKey): String {
        val encoded = publicKey.encoded
        val base64 = Base64.encodeToString(encoded, Base64.DEFAULT)

        return """-----BEGIN PUBLIC KEY-----
${base64.chunked(64).joinToString("\n")}
-----END PUBLIC KEY-----
"""
    }

    /**
     * Elimina la clave de huella registrada
     */
    fun deleteFingerprintKey(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            keyStore.deleteEntry(ALIAS_FINGERPRINT)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
