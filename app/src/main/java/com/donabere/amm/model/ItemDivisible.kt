package com.donabere.amm.model

data class ItemDivisible(
    val detalleId: String,
    val unidad: Int,              // 1-based (unidad 1 de 3, unidad 2 de 3...)
    val totalUnidades: Int,       // total de ese producto
    val nombreProducto: String,
    val precio: Double,           // precio unitario
    var cuentaPersonaIndex: Int? = null
) {
    val displayNombre: String get() =
        if (totalUnidades > 1) "$nombreProducto (u.$unidad/$totalUnidades)"
        else nombreProducto
}