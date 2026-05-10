package com.donabere.amm.model

import android.content.PeriodicSync
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.donabere.amm.model.enums.EstadoPedido
import com.donabere.amm.model.response.MesaResponse

@Entity(tableName = "pedidos")
data class Pedido(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val mozoId: Int,
    val mesasIds: String,
    val mesas: List<MesaResponse> = emptyList(),
    val estado: EstadoPedido = EstadoPedido.BORRADOR,
    val totalPagar: Double = 0.0,
    val creadoEn: Long = System.currentTimeMillis(),
    val cuentas: List<Cuenta> =  emptyList(),
    val ultimoIntentoSync: Long? = null,
    val idRemoto: Long? = null
)
