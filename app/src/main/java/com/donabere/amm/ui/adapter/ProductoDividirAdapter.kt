package com.donabere.amm.ui.adapter

import android.content.ClipData
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.donabere.amm.databinding.ItemProductoDividirBinding

import com.donabere.amm.model.ui.ProductoDividirUi
import com.donabere.amm.utils.ImageUrlResolver

class ProductosDividirAdapter ():
    ListAdapter<ProductoDividirUi, ProductosDividirAdapter.ProductoViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val binding = ItemProductoDividirBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductoViewHolder(
        private val binding: ItemProductoDividirBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(producto: ProductoDividirUi) {

            binding.tvNombreProducto.text = producto.nombreProducto
            binding.tvPrecio.text = "S/. %.2f".format(producto.precioUnitario)
            binding.tvCantidad.text = producto.cantidadDisponible.toString()
            binding.tvSubtotal.text = "S/. %.2f".format(producto.subtotalDisponible)
            val imageUrl = ImageUrlResolver.resolve(binding.root.context, producto.imagenProducto)
            binding.ivProducto.load(imageUrl) {
                crossfade(true)
            }

            binding.root.alpha =
                if (producto.cantidadDisponible > 0) 1f else 0.35f

            binding.root.isEnabled = producto.cantidadDisponible > 0

            binding.root.setOnLongClickListener {

                if (producto.cantidadDisponible <= 0) {
                    return@setOnLongClickListener false
                }

                val clipData = ClipData.newPlainText(
                    "producto_key",
                    producto.key
                )

                val shadow = View.DragShadowBuilder(binding.root)

                binding.root.startDragAndDrop(
                    clipData,
                    shadow,
                    producto.key,
                    0
                )

                true
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ProductoDividirUi>() {
            override fun areItemsTheSame(
                oldItem: ProductoDividirUi,
                newItem: ProductoDividirUi
            ): Boolean = oldItem.key == newItem.key

            override fun areContentsTheSame(
                oldItem: ProductoDividirUi,
                newItem: ProductoDividirUi
            ): Boolean = oldItem == newItem
        }
    }
}