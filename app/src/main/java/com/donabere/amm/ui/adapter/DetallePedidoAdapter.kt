package com.donabere.amm.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.R
import com.donabere.amm.model.DetallePedido
import java.text.NumberFormat
import java.util.Locale

class DetallePedidoAdapter (
    private val onIncrement: (DetallePedido) -> Unit,
    private val onDecrement: (DetallePedido) -> Unit,
    private val onDelete: (DetallePedido) -> Unit,
    private val onEditNota: (DetallePedido) -> Unit
) : ListAdapter<DetallePedido, DetallePedidoAdapter.ViewHolder>(DIFF_CALLBACK){
    private val moneyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detalle_pedido, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getItemAt(position: Int): DetallePedido = getItem(position)

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombre: TextView = view.findViewById(R.id.tv_nombre_producto)
        private val tvNota: TextView = view.findViewById(R.id.tv_nota)
        private val tvPrecio: TextView = view.findViewById(R.id.tv_precio_unitario)
        private val tvCantidad: TextView = view.findViewById(R.id.tv_cantidad)
        private val tvSubtotal: TextView = view.findViewById(R.id.tv_subtotal)
        private val btnMas: ImageButton = view.findViewById(R.id.btn_mas)
        private val btnMenos: ImageButton = view.findViewById(R.id.btn_menos)
        private val btnNota: ImageButton = view.findViewById(R.id.btn_nota)

        fun bind(detalle: DetallePedido) {
            tvNombre.text = detalle.nombreProducto
            tvCantidad.text = detalle.cantidad.toString()
            tvPrecio.text = moneyFormat.format(detalle.precioUnitario)
            tvSubtotal.text = moneyFormat.format(detalle.subtotal)

            if (detalle.nota.isBlank()) {
                tvNota.visibility = View.GONE
            } else {
                tvNota.visibility = View.VISIBLE
                tvNota.text = "📝 ${detalle.nota}"
            }

            btnMas.setOnClickListener { onIncrement(detalle) }
            btnMenos.setOnClickListener {
                if (detalle.cantidad > 1) onDecrement(detalle)
                else onDelete(detalle) // Si es 1 y presiona −, elimina
            }
            btnNota.setOnClickListener { onEditNota(detalle) }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DetallePedido>() {
            override fun areItemsTheSame(
                oldItem: DetallePedido,
                newItem: DetallePedido
            ) = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: DetallePedido,
                newItem: DetallePedido
            ) = oldItem == newItem
        }
    }
    class SwipeToDeleteCallback(
        private val adapter: DetallePedidoAdapter,
        private val onSwipe: (DetallePedido) -> Unit
    ) : ItemTouchHelper.SimpleCallback(
        0, // no drag
        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // swipe ambos lados
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ) = false // no usamos drag

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            val detalle = adapter.getItemAt(position)
            onSwipe(detalle)
        }
    }
}