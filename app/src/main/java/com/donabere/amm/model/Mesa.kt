package com.donabere.amm.model

import com.google.gson.annotations.SerializedName

data class Mesa(
    @SerializedName("id") val id: Int,
    @SerializedName("capacity") val capacity: Int,
    @SerializedName("status") val status: Int,
    @SerializedName("price") val price: Double
)
