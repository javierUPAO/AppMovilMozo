package com.donabere.amm.model

import com.donabere.amm.model.enums.TipoProducto

data class Producto(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val precio: Double,
    val imagen: String,
    val tipo: TipoProducto,
    val stock: Int
)
