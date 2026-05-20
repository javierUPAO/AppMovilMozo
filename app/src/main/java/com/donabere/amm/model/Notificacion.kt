package com.donabere.amm.model

import com.donabere.amm.model.enums.TipoNotificacion
import com.google.firebase.Timestamp

data class Notificacion(
    val id: String,
    val tipo: TipoNotificacion,
    val mensaje: String,
    val leida: Boolean,
    val fecha: Timestamp = Timestamp.now(),
    val pedidoId: String
)