package com.donabere.amm.model

data class Bebida(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val image: String?,
    val stock: Int = 0
)