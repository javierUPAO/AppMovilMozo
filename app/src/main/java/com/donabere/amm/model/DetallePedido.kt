package com.donabere.amm.model

data class DetallePedido(
    val id: String = "",
    val productoId: String = "",
    val nombreProducto: String = "",
    val precioUnitario: Double = 0.0,
    val cantidad: Int = 1,
    val nota: String = "",
    val anulado: Boolean = false,
    val motivoAnulacion: String = "",
    // A qué cuenta pertenece este detalle
    val cuentaId: String? = null
) {
    val subtotal: Double get() = precioUnitario * cantidad
}