package com.donabere.amm.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.donabere.amm.model.enums.EstadoCuenta

@Entity(
    tableName = "cuentas",
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
data class Cuenta(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val pedidoId: Int,
    val itemsIds: String = "",
    val items: List<DetallePedido> = emptyList(),
    val total: Double = 0.0,
    val estadoPago: EstadoCuenta = EstadoCuenta.PENDIENTE,
    val idRemoto: Long? = null,
)