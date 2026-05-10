package com.donabere.amm.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "detalles_pedido",
    foreignKeys = [
        ForeignKey(
            entity = Pedido::class,
            parentColumns = ["id"],
            childColumns = ["pedidoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pedidoId")]
)
data class DetallePedido(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val pedidoId: Int,
    val productoId: Int,
    val nombreProducto: String,
    val precioUnitario: Double,
    val cantidad: Int,
    val nota: String = "",
    val anulado: Boolean = false,
    val motivoAnulacion: String? = null
) {
    val subtotal: Double get() = precioUnitario * cantidad
}