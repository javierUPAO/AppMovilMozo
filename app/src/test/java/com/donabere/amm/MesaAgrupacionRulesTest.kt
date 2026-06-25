package com.donabere.amm

import com.donabere.amm.domain.MesaAgrupacionRules
import com.donabere.amm.model.Mesa
import com.donabere.amm.model.enums.EstadoMesa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MesaAgrupacionRulesTest {

    @Test
    fun agruparDosMesasLibres_debeGenerarGrupoConAmbasMesas() {
        // Given
        val mesa1 = Mesa(
            id = "m1",
            estado = EstadoMesa.LIBRE,
            numClientes = 0
        )

        val mesa2 = Mesa(
            id = "m2",
            estado = EstadoMesa.LIBRE,
            numClientes = 0
        )

        // When
        val puedeAgrupar = MesaAgrupacionRules.puedeAgruparMesasLibres(mesa1, mesa2)
        val grupoId = MesaAgrupacionRules.generarGrupoId(mesa1, mesa2)
        val mesasAgrupadas = MesaAgrupacionRules.obtenerMesasAgrupadas(mesa1, mesa2)

        // Then
        assertTrue(puedeAgrupar)
        assertEquals("g_m1_m2", grupoId)
        assertEquals(listOf("m1", "m2"), mesasAgrupadas)
    }

    @Test
    fun agregarMesaLibreAMesaOcupada_debePermitirAsociarlaAlPedidoExistente() {
        // Given
        val mesaLibre = Mesa(
            id = "m4",
            estado = EstadoMesa.LIBRE,
            numClientes = 0
        )

        val mesaOcupada = Mesa(
            id = "m3",
            estado = EstadoMesa.OCUPADA,
            numClientes = 3,
            pedidoId = "p100"
        )

        // When
        val puedeAgregar = MesaAgrupacionRules.puedeAgregarMesaAPedido(
            mesaLibre = mesaLibre,
            mesaOcupada = mesaOcupada
        )

        val grupoId = MesaAgrupacionRules.generarGrupoId(mesaOcupada, mesaLibre)
        val mesasAgrupadas = MesaAgrupacionRules.obtenerMesasAgrupadas(mesaOcupada, mesaLibre)

        // Then
        assertTrue(puedeAgregar)
        assertEquals("g_m3_m4", grupoId)
        assertEquals(listOf("m3", "m4"), mesasAgrupadas)
        assertEquals("p100", mesaOcupada.pedidoId)
    }

    @Test
    fun separarMesaDeGrupo_debeDevolverMesasRestantes() {
        // Given
        val mesaSeparada = Mesa(
            id = "m2",
            estado = EstadoMesa.LIBRE,
            numClientes = 0,
            grupoId = "g_m1_m2",
            mesasAgrupadas = listOf("m1", "m2")
        )

        // When
        val restantes = MesaAgrupacionRules.obtenerMesasRestantesAlSeparar(mesaSeparada)

        // Then
        assertEquals(listOf("m1"), restantes)
    }

    @Test
    fun generarNombreVisual_debeMostrarMesaUnoMasDos() {
        // Given
        val mesaAgrupada = Mesa(
            id = "m1",
            estado = EstadoMesa.LIBRE,
            numClientes = 0,
            grupoId = "g_m1_m2",
            mesasAgrupadas = listOf("m2", "m1")
        )

        // When
        val nombreVisual = MesaAgrupacionRules.generarNombreVisual(mesaAgrupada)

        // Then
        assertEquals("Mesa 1+2", nombreVisual)
    }
}