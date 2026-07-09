package com.donabere.amm.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.databinding.ItemDetalleCuentaDivisionBinding
import com.donabere.amm.model.DetallePedido

class DetallesCuentaDivisionAdapter(
    private val cuentaId: String,
    private val onIncrementar: (String, DetallePedido) -> Unit,
    private val onDecrementar: (String, DetallePedido) -> Unit,
    private val onEliminar: (String, DetallePedido) -> Unit)
    : ListAdapter<DetallePedido, DetallesCuentaDivisionAdapter.DetalleViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetalleViewHolder {
        val binding = ItemDetalleCuentaDivisionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DetalleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetalleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DetalleViewHolder(
        private val binding: ItemDetalleCuentaDivisionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(detalle: DetallePedido) {

            binding.tvNombreProducto.text = detalle.nombreProducto
            binding.tvPrecio.text = "S/. %.2f c/u".format(detalle.precioUnitario)
            binding.tvCantidad.text = detalle.cantidad.toString()
            binding.tvSubtotal.text = "Sub Total: S/. %.2f".format(detalle.subtotal)

            binding.btnMas.setOnClickListener {
                onIncrementar(cuentaId, detalle)
            }

            binding.btnMenos.setOnClickListener {
                onDecrementar(cuentaId, detalle)
            }

            binding.btnEliminar.setOnClickListener {
                onEliminar(cuentaId, detalle)
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DetallePedido>() {
            override fun areItemsTheSame(
                oldItem: DetallePedido,
                newItem: DetallePedido
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: DetallePedido,
                newItem: DetallePedido
            ): Boolean = oldItem == newItem
        }
    }
}