package com.donabere.amm.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.databinding.ItemCuentaResumenDivisionBinding
import com.donabere.amm.model.ui.CuentaDivisionUi

class CuentasResumenDivisionAdapter :
    RecyclerView.Adapter<CuentasResumenDivisionAdapter.ViewHolder>() {

    private var cuentas = listOf<CuentaDivisionUi>()
    private val cuentasExpandidas = mutableSetOf<String>()

    fun submitList(data: List<CuentaDivisionUi>) {
        cuentas = data

        if (cuentasExpandidas.isEmpty() && data.isNotEmpty()) {
            cuentasExpandidas.add(data.first().id)
        }

        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemCuentaResumenDivisionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cuenta: CuentaDivisionUi, position: Int) {
            binding.tvNombreCuenta.text = cuenta.nombre.replace("Cuenta", "Persona")
            binding.tvTotalCuenta.text = "S/. %.2f".format(cuenta.total)
            binding.tvTotalCuentaCuerpo.text = "Total cuenta: S/. %.2f".format(cuenta.total)

            binding.tvIconoPersona.setBackgroundColor(
                if (position % 2 == 0) {
                    Color.parseColor("#2563EB")
                } else {
                    Color.parseColor("#22C55E")
                }
            )

            val expandido = cuentasExpandidas.contains(cuenta.id)

            binding.contenidoCuentaResumen.visibility =
                if (expandido) View.VISIBLE else View.GONE

            binding.tvFlecha.text =
                if (expandido) "⌃" else "⌄"

            binding.headerCuentaResumen.setOnClickListener {
                if (cuentasExpandidas.contains(cuenta.id)) {
                    cuentasExpandidas.remove(cuenta.id)
                } else {
                    cuentasExpandidas.add(cuenta.id)
                }

                notifyItemChanged(adapterPosition)
            }

            val detallesAdapter = DetallesResumenDivisionAdapter()
            detallesAdapter.submitList(
                cuenta.detalles.filter { !it.anulado && it.cantidad > 0 }
            )

            binding.rvDetallesResumen.apply {
                layoutManager = LinearLayoutManager(binding.root.context)
                adapter = detallesAdapter
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCuentaResumenDivisionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(cuentas[position], position)
    }

    override fun getItemCount(): Int = cuentas.size
}