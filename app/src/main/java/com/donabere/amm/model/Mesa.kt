package com.donabere.amm.model

import com.donabere.amm.model.enums.EstadoMesa

data class Mesa(
    val id: String,
    val estado: EstadoMesa,
    val numClientes: Int,
    val pedidoId: String? = null,   // no null cuando está OCUPADA
    val grupoId: String? = null,
    val mesasAgrupadas: List<String> = emptyList()
)