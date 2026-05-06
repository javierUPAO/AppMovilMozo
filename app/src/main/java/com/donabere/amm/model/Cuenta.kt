package com.donabere.amm.model

import com.donabere.amm.model.DetallePedido
import com.donabere.amm.model.enums.EstadoCuenta

data class Cuenta(
    val id: Int,
    val pedidoId: Int,
    val items: List<DetallePedido> = emptyList(),
    val total: Double,
    val estado: EstadoCuenta
)