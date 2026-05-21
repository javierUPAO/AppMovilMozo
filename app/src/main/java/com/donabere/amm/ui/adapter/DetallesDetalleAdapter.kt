package com.donabere.amm.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.databinding.ItemDetalleDetalleBinding
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.R
import coil.load

class DetallesDetalleAdapter(
    private val onEliminar: (DetallePedido) -> Unit
) : RecyclerView.Adapter<DetallesDetalleAdapter.ViewHolder>() {

    private var detalles: List<DetallePedido> = emptyList()

    fun actualizarDetalles(nuevosDetalles: List<DetallePedido>) {
        detalles = nuevosDetalles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDetalleDetalleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(detalles[position])
    }

    override fun getItemCount() = detalles.size

    inner class ViewHolder(private val binding: ItemDetalleDetalleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(detalle: DetallePedido) {
            // Nombre y cantidad
            binding.tvNombre.text = detalle.nombreProducto
            binding.tvCantidad.text = "x${detalle.cantidad}"
            binding.tvPrecio.text = "\$\$${(detalle.precioUnitario * detalle.cantidad)}"

            // Nota (si existe)
            if (detalle.nota.isNotEmpty()) {
                binding.tvNota.text = "Nota: ${detalle.nota}"
                binding.tvNota.visibility = android.view.View.VISIBLE
            } else {
                binding.tvNota.visibility = android.view.View.GONE
            }

            // Botón eliminar
            binding.btnEliminar.text = "🗑️"
            binding.btnEliminar.setOnClickListener {
                onEliminar(detalle)
            }

            // Cargar imagen del producto
            if (detalle.imagenProducto.isNotEmpty()) {
                binding.ivProducto.load(detalle.imagenProducto) {
                    crossfade(true)
                    error(R.drawable.ic_nav_bascket)
                    fallback(R.drawable.ic_nav_bascket)
                }
            } else {
                binding.ivProducto.setImageResource(R.drawable.ic_nav_bascket)
            }
            
            // Ocultar elementos de estado que ya no se usan
            binding.chipEstado.visibility = android.view.View.GONE
            binding.btnCambiarEstado.visibility = android.view.View.GONE
        }
    }
}
