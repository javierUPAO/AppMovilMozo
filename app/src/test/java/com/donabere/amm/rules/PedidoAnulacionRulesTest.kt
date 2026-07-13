package com.donabere.amm.rules

import com.donabere.amm.model.enums.EstadoPedido
import org.junit.Assert.*
import org.junit.Test

class PedidoAnulacionRulesTest {

    @Test
    fun puedeAnularProducto_enEstadoComandadoOPendiente_retornaTrue() {
        assertTrue(PedidoAnulacionRules.puedeAnularProducto(EstadoPedido.COMANDADO))
        assertTrue(PedidoAnulacionRules.puedeAnularProducto(EstadoPedido.PENDIENTE_PREPARACION))
    }

    @Test
    fun puedeAnularProducto_enEstadoCocinaOAtendido_retornaFalse() {
        assertFalse(PedidoAnulacionRules.puedeAnularProducto(EstadoPedido.COCINA))
        assertFalse(PedidoAnulacionRules.puedeAnularProducto(EstadoPedido.LISTO_PARA_ENTREGAR))
        assertFalse(PedidoAnulacionRules.puedeAnularProducto(EstadoPedido.ATENDIDO))
        assertFalse(PedidoAnulacionRules.puedeAnularProducto(EstadoPedido.PAGADO))
    }

    @Test
    fun esMotivoValido_conMotivoSuficiente_retornaTrue() {
        assertTrue(PedidoAnulacionRules.esMotivoValido("Error del cliente"))
        assertTrue(PedidoAnulacionRules.esMotivoValido("Plato equivocado"))
    }

    @Test
    fun esMotivoValido_conMotivoCortoOVacio_retornaFalse() {
        assertFalse(PedidoAnulacionRules.esMotivoValido(""))
        assertFalse(PedidoAnulacionRules.esMotivoValido("   "))
        assertFalse(PedidoAnulacionRules.esMotivoValido("Ups")) // Menor a 5 caracteres
    }

    @Test
    fun calcularNuevoTotal_restaMontoCorrectamente() {
        val totalActual = 120.0
        val precioUnitario = 20.0
        val cantidadAnulada = 2
        
        val nuevoTotal = PedidoAnulacionRules.calcularNuevoTotal(totalActual, precioUnitario, cantidadAnulada)
        assertEquals(80.0, nuevoTotal, 0.001)
    }

    @Test
    fun calcularNuevoTotal_limitaEnCero() {
        val totalActual = 50.0
        val precioUnitario = 60.0
        val cantidadAnulada = 1
        
        val nuevoTotal = PedidoAnulacionRules.calcularNuevoTotal(totalActual, precioUnitario, cantidadAnulada)
        assertEquals(0.0, nuevoTotal, 0.001)
    }

    @Test
    fun esCantidadValida_dentroDelRango_retornaTrue() {
        assertTrue(PedidoAnulacionRules.esCantidadValida(2, 3))
        assertTrue(PedidoAnulacionRules.esCantidadValida(3, 3))
    }

    @Test
    fun esCantidadValida_fueraDelRango_retornaFalse() {
        assertFalse(PedidoAnulacionRules.esCantidadValida(0, 3))
        assertFalse(PedidoAnulacionRules.esCantidadValida(-1, 3))
        assertFalse(PedidoAnulacionRules.esCantidadValida(4, 3))
    }
}
