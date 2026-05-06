package com.donabere.amm.data.remote.mapper

import com.donabere.amm.model.Mesa
import com.donabere.amm.model.enums.EstadoMesa
import com.donabere.amm.model.response.MesaResponse

fun MesaResponse.toDomain(): Mesa {
    return Mesa(
        id = this.id,
        estado = if (this.status == 1) EstadoMesa.LIBRE else EstadoMesa.OCUPADA,
        numClientes = 0
    )
}