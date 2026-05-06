package com.donabere.amm.model

import com.donabere.amm.model.enums.EstadoMesa

data class Mesa(
    val id: Int,
    val estado: EstadoMesa,
    val numClientes: Int
)

