package com.donabere.amm.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.databinding.ItemMesaBinding
import com.donabere.amm.model.response.MesaResponse

class ItemMesaAdapter(
    private val onMesaClick: (MesaResponse) -> Unit
) : RecyclerView.Adapter<ItemMesaAdapter.MesaViewHolder>() {

    private var mesasList = listOf<MesaResponse>()

    fun submitList(mesaResponses: List<MesaResponse>) {
        mesasList = mesaResponses
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MesaViewHolder {
        val binding = ItemMesaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MesaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MesaViewHolder, position: Int) {
        holder.bind(mesasList[position])
    }

    override fun getItemCount(): Int = mesasList.size

    inner class MesaViewHolder(private val binding: ItemMesaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mesaResponse: MesaResponse) {
            binding.tvIdMesa.text = "Mesa ${mesaResponse.id}"
            binding.tvCapacidad.text = "Capacidad: ${mesaResponse.capacity} personas"
            binding.tvPrecio.text = "$${mesaResponse.price}"

            // Supongamos que status 0 = Disponible, 1 = Ocupada (según validación backend)
            if (mesaResponse.status == 0) {
                binding.tvEstado.text = "Disponible"
                binding.tvEstado.setTextColor(Color.parseColor("#388E3C")) // Verde
                binding.ivIconoMesa.setColorFilter(Color.parseColor("#388E3C"))
            } else {
                binding.tvEstado.text = "Ocupada"
                binding.tvEstado.setTextColor(Color.parseColor("#D32F2F")) // Rojo
                binding.ivIconoMesa.setColorFilter(Color.parseColor("#D32F2F"))
            }

            binding.root.setOnClickListener {
                onMesaClick(mesaResponse)
            }
        }

    }
}
