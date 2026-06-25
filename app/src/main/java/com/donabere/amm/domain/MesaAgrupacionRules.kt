package com.donabere.amm.domain

import com.donabere.amm.model.Mesa
import com.donabere.amm.model.enums.EstadoMesa

object MesaAgrupacionRules {

    fun generarGrupoId(mesa1: Mesa, mesa2: Mesa): String {
        return mesa1.grupoId ?: mesa2.grupoId ?: "g_${mesa1.id}_${mesa2.id}"
    }

    fun obtenerMesasBase(mesa: Mesa): List<String> {
        return if (mesa.grupoId != null && mesa.mesasAgrupadas.isNotEmpty()) {
            mesa.mesasAgrupadas
        } else {
            listOf(mesa.id)
        }
    }

    fun obtenerMesasAgrupadas(mesa1: Mesa, mesa2: Mesa): List<String> {
        val grupoMesa1 = obtenerMesasBase(mesa1)
        val grupoMesa2 = obtenerMesasBase(mesa2)

        return (grupoMesa1 + grupoMesa2).distinct()
    }

    fun puedeAgruparMesasLibres(mesa1: Mesa, mesa2: Mesa): Boolean {
        return mesa1.estado == EstadoMesa.LIBRE &&
                mesa2.estado == EstadoMesa.LIBRE
    }

    fun puedeAgregarMesaAPedido(mesaLibre: Mesa, mesaOcupada: Mesa): Boolean {
        return mesaLibre.estado == EstadoMesa.LIBRE &&
                mesaOcupada.estado == EstadoMesa.OCUPADA &&
                mesaOcupada.pedidoId != null
    }

    fun obtenerMesasRestantesAlSeparar(mesa: Mesa): List<String> {
        return mesa.mesasAgrupadas.filter { it != mesa.id }
    }

    fun generarNombreVisual(mesa: Mesa): String {
        return if (mesa.grupoId != null && mesa.mesasAgrupadas.isNotEmpty()) {
            val numeros = mesa.mesasAgrupadas
                .map { it.replace("m", "").toIntOrNull() ?: 0 }
                .sorted()

            "Mesa " + numeros.joinToString("+")
        } else {
            "Mesa ${mesa.id.replace("m", "")}"
        }
    }
}