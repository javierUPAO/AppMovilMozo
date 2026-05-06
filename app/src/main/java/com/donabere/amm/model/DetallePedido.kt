package com.donabere.amm.model

data class DetallePedido(
    val id: Int,
    val productoId: Int,
    val cantidad: Int,
    val precioUnitario: Double,
    val nota: String? = null
)