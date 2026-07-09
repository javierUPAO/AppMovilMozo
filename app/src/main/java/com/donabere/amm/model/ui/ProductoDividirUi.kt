package com.donabere.amm.model.ui

data class ProductoDividirUi(
    val key: String,
    val productoId: String,
    val nombreProducto: String,
    val precioUnitario: Double,
    val nota: String = "",
    val imagenProducto: String = "",
    val cantidadTotal: Int,
    val cantidadDisponible: Int
) {
    val subtotalDisponible: Double
        get() = precioUnitario * cantidadDisponible
}

