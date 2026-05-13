package com.donabere.amm.model

import com.donabere.amm.model.enums.EstadoCuenta

data class Cuenta(
    val id: String = "",
    val items: List<DetallePedido> = emptyList(),
    val total: Double = 0.0,
    val estadoPago: EstadoCuenta = EstadoCuenta.PENDIENTE
)