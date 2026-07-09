package com.donabere.amm.ui.adapter


import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.donabere.amm.databinding.ItemCuentaDivisionBinding
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.model.ui.CuentaDivisionUi

class CuentasDivisionAdapter(
    private val onProductoDrop: (productoKey: String, cuentaId: String) -> Unit,
    private val onIncrementarDetalle: (cuentaId: String, detalle: DetallePedido) -> Unit,
    private val onDecrementarDetalle: (cuentaId: String, detalle: DetallePedido) -> Unit,
    private val onEliminarDetalle: (cuentaId: String, detalle: DetallePedido) -> Unit,
    private val onEliminarCuenta: (cuentaId: String) -> Unit
) : ListAdapter<CuentaDivisionUi, CuentasDivisionAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(
        private val binding: ItemCuentaDivisionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cuenta: CuentaDivisionUi) {
            binding.tvNombreCuenta.text = cuenta.nombre
            binding.tvTotalCuenta.text = "TOTAL : S/. %.2f".format(cuenta.total)

            binding.btnEliminarCuenta.setOnClickListener {
                onEliminarCuenta(cuenta.id)
            }

            binding.root.setOnDragListener { _, event ->
                when (event.action) {
                    DragEvent.ACTION_DROP -> {
                        val productoKey = event.localState as? String
                        if (productoKey != null) {
                            onProductoDrop(productoKey, cuenta.id)
                        }
                        true
                    }

                    DragEvent.ACTION_DRAG_STARTED,
                    DragEvent.ACTION_DRAG_ENTERED,
                    DragEvent.ACTION_DRAG_EXITED,
                    DragEvent.ACTION_DRAG_ENDED -> true

                    else -> true
                }
            }

            val detallesAdapter = DetallesCuentaDivisionAdapter(
                cuentaId = cuenta.id,
                onIncrementar = onIncrementarDetalle,
                onDecrementar = onDecrementarDetalle,
                onEliminar = onEliminarDetalle
            )

            detallesAdapter.submitList(cuenta.detalles.filter { !it.anulado })

            binding.rvDetalleCuenta.layoutManager =
                LinearLayoutManager(binding.root.context)

            binding.rvDetalleCuenta.adapter = detallesAdapter

            binding.btnEliminarCuenta.visibility =
                if (itemCount > 1) View.VISIBLE else View.INVISIBLE

            binding.btnEliminarCuenta.setOnClickListener {
                onEliminarCuenta(cuenta.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCuentaDivisionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CuentaDivisionUi>() {
            override fun areItemsTheSame(
                oldItem: CuentaDivisionUi,
                newItem: CuentaDivisionUi
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: CuentaDivisionUi,
                newItem: CuentaDivisionUi
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}