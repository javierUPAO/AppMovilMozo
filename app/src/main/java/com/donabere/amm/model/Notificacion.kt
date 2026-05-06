package com.donabere.amm.model

import com.donabere.amm.model.enums.TipoNotificacion
import java.time.LocalDateTime

data class Notificacion(
    val id: Int,
    val tipo: TipoNotificacion,
    val mensaje: String,
    val leida: Boolean,
    val fecha: LocalDateTime,
    val pedidoId: Int
)