package com.donabere.amm.rules

import com.donabere.amm.model.enums.EstadoPedido

object PedidoAnulacionRules {

    /**
     * Valida si un producto puede ser anulado según el estado actual del pedido.
     * Solo se permite anular si el pedido está en estado COMANDADO o PENDIENTE_PREPARACION.
     */
    fun puedeAnularProducto(estadoPedido: EstadoPedido): Boolean {
        return estadoPedido == EstadoPedido.COMANDADO || 
               estadoPedido == EstadoPedido.PENDIENTE_PREPARACION
    }

    /**
     * Valida si el motivo de la anulación es correcto.
     * No debe estar vacío y debe tener al menos una longitud mínima (por ejemplo, 5 caracteres).
     */
    fun esMotivoValido(motivo: String): Boolean {
        return motivo.trim().length >= 5
    }

    /**
     * Calcula el nuevo total de la cuenta tras anular una cantidad de un producto.
     */
    fun calcularNuevoTotal(totalActual: Double, precioUnitario: Double, cantidadAnulada: Int): Double {
        val descuento = precioUnitario * cantidadAnulada
        val nuevoTotal = totalActual - descuento
        return if (nuevoTotal < 0.0) 0.0 else nuevoTotal
    }

    /**
     * Valida si la cantidad a anular es correcta.
     * Debe ser mayor a 0 y menor o igual a la cantidad actual del producto en el detalle.
     */
    fun esCantidadValida(cantidadAnular: Int, cantidadActual: Int): Boolean {
        return cantidadAnular > 0 && cantidadAnular <= cantidadActual
    }
}
