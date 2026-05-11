package com.donabere.amm.model

data class DetallePedido(
    val id: String = "",
    val pedidoId: String = "",
    val productoId: Int = 0,
    val nombreProducto: String = "",
    val precioUnitario: Double = 0.0,
    val cantidad: Int = 1,
    val nota: String = "",
    val anulado: Boolean = false,
    val motivoAnulacion: String? = null
) {
    val subtotal: Double get() = precioUnitario * cantidad
}