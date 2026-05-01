package com.donabere.amm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.R
import com.donabere.amm.model.Dish

class MenuAdapter(
    private var menuList: List<Dish>,
    private val onDishClick: (Dish) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    class MenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDishTitle: TextView = itemView.findViewById(R.id.tvDishTitle)
        val tvDishPrice: TextView = itemView.findViewById(R.id.tvDishPrice)
        val tvDishDescription: TextView = itemView.findViewById(R.id.tvDishDescription)
        val tvDishStock: TextView = itemView.findViewById(R.id.tvDishStock)
        val flOutStockOverlay: FrameLayout = itemView.findViewById(R.id.flOutStockOverlay)
        val clDishContainer: ConstraintLayout = itemView.findViewById(R.id.clDishContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dish, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val dish = menuList[position]
        holder.tvDishTitle.text = dish.title
        holder.tvDishPrice.text = "S/ ${dish.price}"
        holder.tvDishDescription.text = dish.description
        holder.tvDishStock.text = "Disponibles: ${dish.stock}"

        if (dish.stock <= 0) {
            holder.flOutStockOverlay.visibility = View.VISIBLE
            holder.clDishContainer.alpha = 0.5f
            holder.itemView.setOnClickListener(null)
            holder.itemView.isClickable = false
        } else {
            holder.flOutStockOverlay.visibility = View.GONE
            holder.clDishContainer.alpha = 1.0f
            holder.itemView.setOnClickListener {
                onDishClick(dish)
            }
            holder.itemView.isClickable = true
        }
    }

    override fun getItemCount(): Int = menuList.size

    fun updateData(newList: List<Dish>) {
        menuList = newList
        notifyDataSetChanged()
    }
}
