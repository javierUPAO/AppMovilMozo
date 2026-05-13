package com.donabere.amm.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.databinding.ItemMesaBinding
import com.donabere.amm.model.Mesa
import com.donabere.amm.model.enums.EstadoMesa
import androidx.core.graphics.toColorInt
import com.donabere.amm.R

class ItemMesaAdapter(
    private val onMesaClick: (Mesa) -> Unit
) : RecyclerView.Adapter<ItemMesaAdapter.MesaViewHolder>() {

    private var mesasList = listOf<Mesa>()

    fun submitList(mesas: List<Mesa>) {
        mesasList = mesas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MesaViewHolder {
        val binding = ItemMesaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MesaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MesaViewHolder, position: Int) {
        holder.bind(mesasList[position])
    }

    override fun getItemCount(): Int = mesasList.size

    inner class MesaViewHolder(
        private val binding: ItemMesaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mesa: Mesa) {

            binding.tvIdMesa.text = "${mesa.id}"
            when (mesa.estado) {

                EstadoMesa.LIBRE -> {
                }

                EstadoMesa.OCUPADA -> {
                    binding.ivIconoMesa.setImageResource(R.drawable.ic_table_selected)
                    binding.tvCapacidad.text = "${mesa.numClientes}"
                    binding.tvIdMesa.setTextColor(Color.parseColor("#CACACA"))
                    binding.tvCapacidadTexto.text= "Personas"
                }

                else -> {
                    binding.tvEstado.setTextColor(Color.GRAY)
                    binding.ivIconoMesa.setColorFilter(Color.GRAY)
                }
            }

            binding.root.setOnClickListener {
                onMesaClick(mesa)
            }
        }
    }
}