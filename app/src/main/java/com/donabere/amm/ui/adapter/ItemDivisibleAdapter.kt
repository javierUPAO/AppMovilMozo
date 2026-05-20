package com.donabere.amm.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.R
import com.donabere.amm.model.ItemDivisible
import com.donabere.amm.model.PersonaCuenta
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ItemDivisibleAdapter(
    private val onAsignar: (detalleId: String, unidad: Int, personaIndex: Int?) -> Unit
) : ListAdapter<ItemDivisible, ItemDivisibleAdapter.ViewHolder>(DIFF) {

    // Las personas se actualizan desde fuera cuando cambia la lista
    private var personas: List<PersonaCuenta> = emptyList()

    fun actualizarPersonas(nuevas: List<PersonaCuenta>) {
        personas = nuevas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_divisible, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombre: TextView  = view.findViewById(R.id.tv_nombre_item)
        private val tvPrecio: TextView  = view.findViewById(R.id.tv_precio_item)
        private val chipGroup: ChipGroup = view.findViewById(R.id.chip_group_personas)

        fun bind(item: ItemDivisible) {
            tvNombre.text = item.displayNombre
            tvPrecio.text = "S/. %.2f x 1 = S/. %.2f".format(item.precio, item.precio)

            chipGroup.removeAllViews()

            // Chip "No asignado"
            val chipNoAsignado = crearChip(
                texto      = "No asignado",
                seleccionado = item.cuentaPersonaIndex == null,
                esNoAsignado = true
            )
            chipNoAsignado.setOnClickListener {
                onAsignar(item.detalleId, item.unidad, null)
            }
            chipGroup.addView(chipNoAsignado)

            // Un chip por cada persona
            personas.forEach { persona ->
                val seleccionado = item.cuentaPersonaIndex == persona.index
                val chip = crearChip(
                    texto        = persona.nombre,
                    seleccionado = seleccionado,
                    esNoAsignado = false
                )
                chip.setOnClickListener {
                    onAsignar(item.detalleId, item.unidad, persona.index)
                }
                chipGroup.addView(chip)
            }
        }

        private fun crearChip(
            texto: String,
            seleccionado: Boolean,
            esNoAsignado: Boolean
        ): Chip {
            val chip = Chip(itemView.context)
            chip.text = texto
            chip.isCheckable = false
            chip.isClickable = true

            if (seleccionado) {
                if (esNoAsignado) {
                    // "No asignado" seleccionado → gris oscuro
                    chip.setChipBackgroundColorResource(R.color.chip_no_asignado_bg)
                    chip.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.chip_no_asignado_text)
                    )
                } else {
                    // Persona seleccionada → naranja primario
                    chip.setChipBackgroundColorResource(R.color.primary_color)
                    chip.setTextColor(
                        ContextCompat.getColor(itemView.context, android.R.color.white)
                    )
                }
            } else {
                // No seleccionado → fondo gris claro, texto secundario
                chip.setChipBackgroundColorResource(R.color.chip_unselected_bg)
                chip.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.text_secondary)
                )
            }
            return chip
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ItemDivisible>() {
            override fun areItemsTheSame(a: ItemDivisible, b: ItemDivisible) =
                a.detalleId == b.detalleId && a.unidad == b.unidad
            override fun areContentsTheSame(a: ItemDivisible, b: ItemDivisible) = a == b
        }
    }
}