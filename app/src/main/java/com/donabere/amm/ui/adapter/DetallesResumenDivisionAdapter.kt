package com.donabere.amm.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.donabere.amm.databinding.ItemDetalleResumenDivisionBinding
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.utils.ImageUrlResolver

class DetallesResumenDivisionAdapter :
    RecyclerView.Adapter<DetallesResumenDivisionAdapter.ViewHolder>() {

    private var detalles = listOf<DetallePedido>()

    fun submitList(data: List<DetallePedido>) {
        detalles = data
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemDetalleResumenDivisionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(detalle: DetallePedido) {
            binding.tvNombreProducto.text = detalle.nombreProducto
            binding.tvPrecio.text = "S/. %.2f c/u".format(detalle.precioUnitario)
            binding.tvCantidad.text = detalle.cantidad.toString()
            binding.tvTotalProducto.text = "S/. %.2f".format(detalle.subtotal)

            val imageUrl = ImageUrlResolver.resolve(
                binding.root.context,
                detalle.imagenProducto
            )

            binding.ivProducto.load(imageUrl) {
                crossfade(true)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDetalleResumenDivisionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(detalles[position])
    }

    override fun getItemCount(): Int = detalles.size
}