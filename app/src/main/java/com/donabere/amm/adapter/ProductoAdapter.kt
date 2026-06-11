package com.donabere.amm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.donabere.amm.R
import com.donabere.amm.model.Producto
import com.donabere.amm.utils.ImageUrlResolver

class ProductoAdapter(
    private var productos: List<Producto>,
    private val onProductoClick: (Producto) -> Unit,
    private val onNotaClick: ((Producto) -> Unit)? = null
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    private var listaCompleta: List<Producto> = productos

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProductImage: ImageView = itemView.findViewById(R.id.ivDishImage)
        val tvProductName: TextView = itemView.findViewById(R.id.tvDishTitle)
        val tvProductPrice: TextView = itemView.findViewById(R.id.tvDishPrice)
        val tvProductDescription: TextView = itemView.findViewById(R.id.tvDishDescription)
        val tvProductStock: TextView = itemView.findViewById(R.id.tvDishStock)
        val flOutStockOverlay: FrameLayout = itemView.findViewById(R.id.flOutStockOverlay)
        val clProductContainer: ConstraintLayout = itemView.findViewById(R.id.clDishContainer)
        val btnNotaPlato: android.widget.ImageButton = itemView.findViewById(R.id.btnNotaPlato)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dish, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productos[position]
        holder.tvProductName.text = producto.nombre
        holder.tvProductPrice.text = "S/ ${producto.precio}"
        holder.tvProductDescription.text = producto.descripcion
        holder.tvProductStock.text = "Stock: ${producto.stock}"
        
        val imageUrl = ImageUrlResolver.resolve(holder.itemView.context, producto.imagen)
        holder.ivProductImage.load(imageUrl) {
            crossfade(true)
        }

        // Ícono de nota: visible solo en modo pedido (cuando hay callback)
        if (onNotaClick != null) {
            holder.btnNotaPlato.visibility = View.VISIBLE
            holder.btnNotaPlato.setOnClickListener { onNotaClick.invoke(producto) }
        } else {
            holder.btnNotaPlato.visibility = View.GONE
        }

        if (producto.stock <= 0) {
            holder.flOutStockOverlay.visibility = View.VISIBLE
            holder.itemView.setOnClickListener(null)
            holder.itemView.isClickable = false
        } else {
            holder.flOutStockOverlay.visibility = View.GONE
            holder.itemView.setOnClickListener {
                onProductoClick(producto)
            }
            holder.itemView.isClickable = true
        }
    }

    override fun getItemCount(): Int = productos.size

    fun updateData(newList: List<Producto>) {
        listaCompleta = newList
        productos = newList
        notifyDataSetChanged()
    }

    /** Filtra la grilla en tiempo real según el texto del buscador. */
    fun filter(query: String) {
        productos = if (query.isBlank()) {
            listaCompleta
        } else {
            listaCompleta.filter {
                it.nombre.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }
}
