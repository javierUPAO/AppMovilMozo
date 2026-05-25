package com.donabere.amm.model

import com.donabere.amm.model.enums.EstadoPedido
import com.google.firebase.Timestamp

data class Pedido(
    val id: String = "",
    val mozoId: String = "",
    val mesasIds: List<String> = emptyList(),
    val cuentas:List<Cuenta> = emptyList(),
    val estado: EstadoPedido = EstadoPedido.PENDIENTE_PREPARACION,
    val totalPagar: Double = 0.0,
    val creadoEn: Timestamp = Timestamp.now()
)