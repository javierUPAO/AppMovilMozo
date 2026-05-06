package com.donabere.amm.model

import com.donabere.amm.model.enums.EstadoTurno
import java.time.LocalDateTime

data class Turno(
    val id: Int,
    val inicio: LocalDateTime,
    val fin: LocalDateTime?,
    val estado: EstadoTurno,
    val totalMesas: Int,
    val totalPedidos: Int,
    val totalVendido: Double
)