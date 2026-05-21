package com.donabere.amm.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.databinding.ItemMesaBinding
import com.donabere.amm.model.Mesa
import com.donabere.amm.model.enums.EstadoMesa
import com.donabere.amm.R

class ItemMesaAdapter(
    private val onMesaClick: (Mesa) -> Unit
) : RecyclerView.Adapter<ItemMesaAdapter.MesaViewHolder>() {

    private var mesasList = listOf<Mesa>()

    fun submitList(mesas: List<Mesa>) {
        this.mesasList = mesas
        // notifyDataSetChanged() // <-- A VECES ESTO CAUSA QUE LOS CLICKS SE PIERDAN
        notifyItemRangeChanged(0, mesas.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MesaViewHolder {
        val binding = ItemMesaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MesaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MesaViewHolder, position: Int) {
        holder.bind(mesasList[position])
    }

    override fun getItemCount() = mesasList.size

    inner class MesaViewHolder(
        private val binding: ItemMesaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mesa: Mesa) {
            binding.tvIdMesa.text = mesa.id

            when (mesa.estado) {
                EstadoMesa.LIBRE -> {
                    // Apariencia normal
                    binding.ivIconoMesa.setImageResource(R.drawable.ic_table_new)
                    binding.ivIconoMesa.clearColorFilter()
                    binding.tvIdMesa.setTextColor(Color.BLACK)
                    binding.tvCapacidad.text = ""
                    binding.tvCapacidadTexto.text = ""
                    binding.tvEstado.text = ""

                    // Click habilitado
                    binding.root.alpha = 1f
                    binding.root.isEnabled  = true
                    binding.root.setOnClickListener { onMesaClick(mesa) }
                }

                EstadoMesa.OCUPADA -> {
                    // Apariencia ocupada
                    binding.ivIconoMesa.setImageResource(R.drawable.ic_table_selected)
                    binding.tvIdMesa.setTextColor(Color.parseColor("#CACACA"))
                    binding.tvCapacidad.text = if (mesa.numClientes > 0)
                        "${mesa.numClientes}" else ""
                    binding.tvCapacidadTexto.text = if (mesa.numClientes > 0) "Personas" else ""
                    binding.tvEstado.text = "OCUPADA"
                    binding.tvEstado.setTextColor(Color.parseColor("#F5A623"))

                }

                else -> {
                    binding.tvEstado.setTextColor(Color.GRAY)
                    binding.ivIconoMesa.setColorFilter(Color.GRAY)
                    binding.root.alpha = 0.5f
                    binding.root.isEnabled = false
                    binding.root.setOnClickListener(null)
                }
            }
        }
    }
}