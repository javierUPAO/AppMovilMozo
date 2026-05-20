package com.donabere.amm.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.R
import java.text.NumberFormat
import java.util.Locale

class DetallePedidoReadOnlyAdapter(
    private val items: List<Item>
) : RecyclerView.Adapter<DetallePedidoReadOnlyAdapter.ViewHolder>() {

    data class Item(
        val nombre: String,
        val cantidad: Int,
        val subtotal: Double
    )

    private val moneyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detalle_readonly, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombre:   TextView = view.findViewById(R.id.tv_nombre_producto)
        private val tvCantidad: TextView = view.findViewById(R.id.tv_cantidad)
        private val tvSubtotal: TextView = view.findViewById(R.id.tv_subtotal)

        fun bind(item: Item) {
            tvNombre.text   = item.nombre
            tvCantidad.text = "x${item.cantidad}"
            tvSubtotal.text = moneyFormat.format(item.subtotal)
        }
    }
}