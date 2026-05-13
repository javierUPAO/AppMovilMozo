package com.donabere.amm.model

import com.donabere.amm.model.enums.EstadoTurno
import com.google.firebase.Timestamp

data class Turno(
    val id: String = "",
    val mozoId: String = "",
    val inicio: Timestamp? = Timestamp.now(),
    val fin: Timestamp? = null,
    val estado: EstadoTurno = EstadoTurno.CERRADO,
    val totalMesas: Int = 0,
    val totalPedidos: Int = 0,
    val totalVendido: Double = 0.0
)