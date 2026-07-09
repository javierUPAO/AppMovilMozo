package com.donabere.amm.model.ui

import com.donabere.amm.model.DetallePedido

data class CuentaDivisionUi(
    val id: String,
    val nombre: String,
    val detalles: List<DetallePedido> = emptyList()
) {
    val total: Double
        get() = detalles
            .filter { !it.anulado }
            .sumOf { it.subtotal }
}