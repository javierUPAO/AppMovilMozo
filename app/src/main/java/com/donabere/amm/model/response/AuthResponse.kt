package com.donabere.amm.model.response

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("token", alternate = ["Token"]) val token: String?,
    @SerializedName("name") val name: String,
    @SerializedName("dni") val dni: String,
    @SerializedName("role") val role: String

    //./gradlew --stop
)