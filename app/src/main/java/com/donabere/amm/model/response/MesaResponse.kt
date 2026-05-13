package com.donabere.amm.model.response

import com.google.gson.annotations.SerializedName

data class MesaResponse(
    @SerializedName("id") val id: String,
    @SerializedName("capacity") val capacity: Int,
    @SerializedName("status") val status: Int,
    @SerializedName("price") val price: Double,
    val pedidoId: String? = null
)