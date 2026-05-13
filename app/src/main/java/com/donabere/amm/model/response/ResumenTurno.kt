package com.donabere.amm.model.response

data class ResumenTurno(
    val totalMesas: Int,
    val totalPedidos: Int,
    val totalCobrado: Double
)