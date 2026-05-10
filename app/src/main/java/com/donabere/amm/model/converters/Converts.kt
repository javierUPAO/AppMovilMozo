package com.donabere.amm.model.converters

import androidx.room.TypeConverter
import com.donabere.amm.model.enums.EstadoCuenta
import com.donabere.amm.model.enums.EstadoPedido

class Converts {

    @TypeConverter
    fun fromEstadoPedido(estado: EstadoPedido): String = estado.name

    @TypeConverter
    fun toEstadoPedido(value: String): EstadoPedido =
        EstadoPedido.valueOf(value)

    // EstadoCuentaLocal ↔ String
    @TypeConverter
    fun fromEstadoCuenta(estado: EstadoCuenta): String = estado.name

    @TypeConverter
    fun toEstadoCuenta(value: String): EstadoCuenta =
        EstadoCuenta.valueOf(value)
}