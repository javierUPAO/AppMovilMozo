package com.donabere.amm.model

import com.donabere.amm.model.enums.EstadoCuenta

data class Cuenta(
    val id: String = "",
    val nombre: String = "",          // "Cuenta A", "Cuenta B", etc.
    val detalles: List<DetallePedido> = emptyList(),
    val estadoPago: EstadoCuenta = EstadoCuenta.PENDIENTE
) {
    val total: Double get() = detalles.filter { !it.anulado }.sumOf { it.subtotal }
}