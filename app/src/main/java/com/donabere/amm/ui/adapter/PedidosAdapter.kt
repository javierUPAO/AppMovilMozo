package com.donabere.amm.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.GONE
import android.view.ViewGroup.VISIBLE
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.donabere.amm.databinding.ItemPedidoExpandibleBinding
import com.donabere.amm.model.Pedido
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.model.enums.EstadoPedido

class PedidosAdapter(
    private val onEliminarDetalle: (Pedido, DetallePedido) -> Unit,
    private val onAgregarPlato: (Pedido) -> Unit,
    private val onCambiarEstadoPedido: (Pedido) -> Unit,
    private val onPagarCuenta: (Pedido) -> Unit
) : RecyclerView.Adapter<PedidosAdapter.PedidoViewHolder>() {

    private var pedidos: List<Pedido> = emptyList()
    private var detallesPorPedido: Map<String, List<DetallePedido>> = emptyMap()
    private var expandidoId: String? = null

    fun actualizarPedidos(nuevosPedidos: List<Pedido>) {
        pedidos = nuevosPedidos
        notifyDataSetChanged()
    }

    fun actualizarDetalles(pedidoId: String, detalles: List<DetallePedido>) {
        val mapa = detallesPorPedido.toMutableMap()
        mapa[pedidoId] = detalles
        detallesPorPedido = mapa
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val binding = ItemPedidoExpandibleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PedidoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        holder.bind(pedidos[position])
    }

    override fun getItemCount() = pedidos.size

    inner class PedidoViewHolder(private val binding: ItemPedidoExpandibleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pedido: Pedido) {
            // Header del pedido
            binding.tvPedidoId.text = "ORD - #${pedido.id.takeLast(6)}"
            binding.tvMesas.text = "Mesa - ${pedido.mesasIds.joinToString(", ")}  -  \$${pedido.totalPagar}"
            binding.tvEstado.text = pedido.estado.name
            
            // Mostrar hora del pedido
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val hora = sdf.format(java.util.Date(pedido.creadoEn.seconds * 1000))
            binding.tvHora.text = hora

            // Expandir/Contraer
            val estaExpandido = expandidoId == pedido.id
            binding.detallesContainer.visibility = if (estaExpandido) {
                VISIBLE
            } else {
                GONE
            }

            binding.root.setOnClickListener {
                expandidoId = if (estaExpandido) null else pedido.id
                notifyItemChanged(adapterPosition)
            }

            // Mostrar detalles si está expandido
            if (estaExpandido) {
                val detalles = detallesPorPedido[pedido.id] ?: emptyList()
                if (detalles.isNotEmpty()) {
                    val adapterDetalles = DetallesDetalleAdapter(
                        onEliminar = { detalle ->
                            onEliminarDetalle(pedido, detalle)
                        }
                    )
                    adapterDetalles.actualizarDetalles(detalles)
                    binding.rvDetalles.apply {
                        layoutManager = LinearLayoutManager(itemView.context)
                        adapter = adapterDetalles
                    }
                    binding.tvSinDetalles.visibility = android.view.View.GONE
                } else {
                    binding.tvSinDetalles.visibility = android.view.View.VISIBLE
                }
                
                // Mostrar botones de acción
                binding.botonesAccion.visibility = VISIBLE
            } else {
                binding.botonesAccion.visibility = GONE
            }
            
            binding.btnCambiarEstadoPedido.text = when (pedido.estado) {
                EstadoPedido.BORRADOR -> "Enviar a cocina"
                EstadoPedido.COMANDADO -> "Enviar a cocina"
                EstadoPedido.PENDIENTE_PREPARACION -> "Enviar a cocina"
                EstadoPedido.PENDIENTE_CORRECCION_STOCK -> "Revisar stock"
                EstadoPedido.COCINA -> "Marcar listo"
                EstadoPedido.LISTO_PARA_ENTREGAR -> "Entregar"
                EstadoPedido.ATENDIDO -> "Marcar pagado"
                EstadoPedido.PAGADO -> "✓ Completado"
                EstadoPedido.PAGADO_PARCIAL -> "Completar pago"
                EstadoPedido.PAGO_EN_PROCESO -> "Pago en proceso"
            }
            
            binding.btnCambiarEstadoPedido.setOnClickListener {
                onCambiarEstadoPedido(pedido)
            }

            binding.btnPagarCuenta.setOnClickListener {
                onPagarCuenta(pedido)
            }

            when (pedido.estado) {

                EstadoPedido.ATENDIDO -> {
                    binding.btnCambiarEstadoPedido.visibility = View.GONE
                    binding.btnPagarCuenta.visibility = View.VISIBLE
                }

                EstadoPedido.PAGO_EN_PROCESO,
                EstadoPedido.PAGADO_PARCIAL -> {
                    binding.btnCambiarEstadoPedido.visibility = View.GONE
                    binding.btnPagarCuenta.visibility = View.VISIBLE
                }

                EstadoPedido.PAGADO -> {
                    binding.btnCambiarEstadoPedido.visibility = View.GONE
                    binding.btnPagarCuenta.visibility = View.GONE
                }

                else -> {
                    binding.btnCambiarEstadoPedido.visibility = View.VISIBLE
                    binding.btnPagarCuenta.visibility = View.GONE
                }
            }
        }
    }
}
