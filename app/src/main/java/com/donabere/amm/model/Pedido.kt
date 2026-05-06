package com.donabere.amm.model

import com.donabere.amm.model.enums.EstadoPedido
import com.donabere.amm.model.response.MesaResponse


data class Pedido(
    val id: Int,
    val estado: EstadoPedido,
    val totalPagar: Double = 0.0,
    val cuentas: List<Cuenta> =  emptyList(),
    val mesas: List<MesaResponse> = emptyList(),
    val mozoId: Int
)
