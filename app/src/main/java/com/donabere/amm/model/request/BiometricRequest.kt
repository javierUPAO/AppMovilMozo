package com.donabere.amm.model.request

data class RegisterFingerprintRequest(
    val email: String,
    val public_key: String
)

data class BiometricLoginRequest(
    val email: String,
    val biometric_token: String
)
