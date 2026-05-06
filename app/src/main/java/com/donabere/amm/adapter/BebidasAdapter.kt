package com.donabere.amm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.R
import com.donabere.amm.model.Bebida

class BebidasAdapter(
    private var bebidas: List<Bebida>,
    private val onBebidaClick: (Bebida) -> Unit
) : RecyclerView.Adapter<BebidasAdapter.BebidaViewHolder>() {

    class BebidaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDishTitle: TextView = itemView.findViewById(R.id.tvDishTitle)
        val tvDishPrice: TextView = itemView.findViewById(R.id.tvDishPrice)
        val tvDishDescription: TextView = itemView.findViewById(R.id.tvDishDescription)
        val tvDishStock: TextView = itemView.findViewById(R.id.tvDishStock)
        val flOutStockOverlay: FrameLayout = itemView.findViewById(R.id.flOutStockOverlay)
        val clDishContainer: ConstraintLayout = itemView.findViewById(R.id.clDishContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BebidaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dish, parent, false)
        return BebidaViewHolder(view)
    }

    override fun onBindViewHolder(holder: BebidaViewHolder, position: Int) {
        val bebida = bebidas[position]
        holder.tvDishTitle.text = bebida.name
        holder.tvDishPrice.text = "S/ ${bebida.price}"
        holder.tvDishDescription.text = bebida.description

        holder.tvDishStock.visibility = View.GONE
        holder.flOutStockOverlay.visibility = View.GONE
        holder.clDishContainer.alpha = 1.0f
        holder.itemView.setOnClickListener {
            onBebidaClick(bebida)
        }
        holder.itemView.isClickable = true
    }

    override fun getItemCount(): Int = bebidas.size

    fun updateData(newList: List<Bebida>) {
        bebidas = newList
        notifyDataSetChanged()
    }
}
