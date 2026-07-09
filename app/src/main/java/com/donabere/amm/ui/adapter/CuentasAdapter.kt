package com.donabere.amm.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.databinding.ItemCuentaBinding
import com.donabere.amm.model.Cuenta
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.model.enums.EstadoCuenta

class CuentasAdapter(
    private val onPagarClick: (Cuenta) -> Unit,
) : RecyclerView.Adapter<CuentasAdapter.ViewHolder>() {

    private var lista = listOf<Cuenta>()

    fun submitList(data: List<Cuenta>) {
        lista = data
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemCuentaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var expandido = false

        fun bind(cuenta: Cuenta) {

            binding.tvCuentaId.text =
                "Cuenta #${cuenta.id.takeLast(4)}"

            binding.tvEstadoCuenta.text =
                cuenta.estadoPago.name

            binding.tvTotalCuenta.text =
                "Total: S/. ${"%.2f".format(cuenta.total)}"

            // Estado visual
            when (cuenta.estadoPago) {

                EstadoCuenta.PAGADO -> {

                    binding.tvEstadoCuenta.setTextColor(
                        Color.parseColor("#16A34A")
                    )

                    binding.btnPagarCuenta.visibility = View.GONE
                }

                else -> {

                    binding.tvEstadoCuenta.setTextColor(
                        Color.parseColor("#DC2626")
                    )

                    binding.btnPagarCuenta.visibility = View.VISIBLE
                }
            }

            // Recycler detalles
            val detallesAdapter = DetallesDetalleAdapter(
                onEliminar = { }
            )

            detallesAdapter.actualizarDetalles(cuenta.detalles)

            binding.rvDetalleCuenta.layoutManager =
                LinearLayoutManager(binding.root.context)

            binding.rvDetalleCuenta.adapter =
                detallesAdapter

            // Expandir / Contraer
            binding.detailContainer.visibility =
                if (expandido) View.VISIBLE else View.GONE

            binding.headerCuenta.setOnClickListener {

                expandido = !expandido

                binding.detailContainer.visibility =
                    if (expandido) View.VISIBLE
                    else View.GONE
            }

            // Pagar
            binding.btnPagarCuenta.setOnClickListener {
                onPagarClick(cuenta)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding = ItemCuentaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lista[position])
    }

    override fun getItemCount() = lista.size
}