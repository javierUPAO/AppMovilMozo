package com.donabere.amm.model

data class Dish(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val image: String?,
    val stock: Int
)
