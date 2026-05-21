package com.donabere.amm.ui.fragment

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.donabere.amm.databinding.FragmentListaPedidosBinding
import com.donabere.amm.repository.PedidoRepository
import com.donabere.amm.ui.adapter.PedidosAdapter
import com.donabere.amm.viewmodel.PedidosViewModel
import kotlinx.coroutines.launch

class ListaPedidosFragment : Fragment() {

    private var _binding: FragmentListaPedidosBinding? = null
    private val binding get() = _binding!!

    private val repository = PedidoRepository()
    private val viewModel: PedidosViewModel by viewModels {
        PedidosViewModel.Factory(repository)
    }

    private lateinit var adapter: PedidosAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null
    private val POLLING_INTERVAL = 5000L // 5 segundos

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaPedidosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFiltros()
        observeViewModel()
        cargarPedidos()
        startPolling()
    }

    private fun setupRecyclerView() {
        adapter = PedidosAdapter(
            onEliminarDetalle = { pedido, detalle ->
                mostrarDialogoEliminar(pedido, detalle)
            },
            onAgregarPlato = { pedido ->
                mostrarDialogoAgregarPlato(pedido)
            },
            onCambiarEstadoPedido = { pedido ->
                mostrarDialogoCambiarEstado(pedido)
            }
        )
        
        binding.recyclerViewPedidos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ListaPedidosFragment.adapter
        }
    }

    private fun setupFiltros() {
        val filtros = listOf("TODO", "PENDIENTE", "COCINA", "LISTO_ENT", "PAGADO", "ATENDIDO")
        
        binding.chipGroupFiltros.apply {
            filtros.forEach { filtro ->
                val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                    text = filtro
                    isCheckable = true
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            viewModel.aplicarFiltro(filtro)
                        }
                    }
                }
                addView(chip)
            }
            // Seleccionar "TODO" por defecto
            getChildAt(0)?.let { (it as? com.google.android.material.chip.Chip)?.isChecked = true }
        }
    }

    private fun observeViewModel() {
        viewModel.pedidos.observe(viewLifecycleOwner) { pedidos ->
            adapter.actualizarPedidos(pedidos)
            binding.tvPedidosVacios.visibility = if (pedidos.isEmpty()) View.VISIBLE else View.GONE
            
            // Cargar detalles de cada pedido
            pedidos.forEach { pedido ->
                repository.obtenerDetallesPedido(pedido.id).observe(viewLifecycleOwner) { detalles ->
                    adapter.actualizarDetalles(pedido.id, detalles)
                }
            }
        }
    }

    private fun cargarPedidos() {
        val mozoId = obtenerMozoId()
        if (mozoId.isEmpty()) {
            Toast.makeText(requireContext(), "Error: ID de mozo no encontrado", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.cargarPedidosDelDia(mozoId)
    }

    private fun obtenerMozoId(): String {
        val sharedPref = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("mozoId", "") ?: ""
    }

    private fun mostrarDialogoEliminar(
        pedido: com.donabere.amm.model.Pedido,
        detalle: com.donabere.amm.model.DetallePedido
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar detalle")
            .setMessage("¿Deseas eliminar este plato?\n\n${detalle.nombreProducto} (x${detalle.cantidad})\n\n⚠️ Es irreversible. ¿Estás seguro de continuar?")
            .setPositiveButton("Sí, eliminar") { _, _ ->
                eliminarDetallePedido(pedido, detalle)
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun eliminarDetallePedido(
        pedido: com.donabere.amm.model.Pedido,
        detalle: com.donabere.amm.model.DetallePedido
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = repository.eliminarDetallePedido(pedido.id, detalle.id)
            resultado.onSuccess {
                Toast.makeText(requireContext(), "Plato eliminado", Toast.LENGTH_SHORT).show()
            }
            resultado.onFailure { error ->
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("ListaPedidosFragment", "Error eliminando: ${error.message}")
            }
        }
    }

    private fun mostrarDialogoAgregarPlato(pedido: com.donabere.amm.model.Pedido) {
        val opciones = arrayOf("Pizza", "Hamburguesa", "Ensalada", "Bebida", "Postre")
        var platoSeleccionado = ""
        var cantidad = 1
        
        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar plato")
            .setSingleChoiceItems(opciones, 0) { _, which ->
                platoSeleccionado = opciones[which]
            }
            .setPositiveButton("Agregar") { _, _ ->
                if (platoSeleccionado.isNotEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Plato '$platoSeleccionado' agregado al pedido",
                        Toast.LENGTH_SHORT
                    ).show()
                    // TODO: Implementar lógica de agregar plato al Firestore
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun mostrarDialogoCambiarEstado(pedido: com.donabere.amm.model.Pedido) {
        val estadoActual = pedido.estado.name
        val proximoEstado = when (pedido.estado) {
            com.donabere.amm.model.enums.EstadoPedido.BORRADOR -> "PENDIENTE_PREPARACION (Enviar a cocina)"
            com.donabere.amm.model.enums.EstadoPedido.COMANDADO -> "PENDIENTE_PREPARACION"
            com.donabere.amm.model.enums.EstadoPedido.PENDIENTE_PREPARACION -> "COCINA"
            com.donabere.amm.model.enums.EstadoPedido.COCINA -> "LISTO_PARA_ENTREGAR"
            com.donabere.amm.model.enums.EstadoPedido.LISTO_PARA_ENTREGAR -> "ATENDIDO"
            com.donabere.amm.model.enums.EstadoPedido.ATENDIDO -> "PAGADO"
            com.donabere.amm.model.enums.EstadoPedido.PAGADO -> "Completado"
            com.donabere.amm.model.enums.EstadoPedido.PAGADO_PARCIAL -> "PAGADO"
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Cambiar estado del pedido")
            .setMessage("Estado actual: $estadoActual\n\n¿Deseas cambiar a: $proximoEstado?")
            .setPositiveButton("Sí, cambiar") { _, _ ->
                cambiarEstadoPedido(pedido)
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun cambiarEstadoPedido(pedido: com.donabere.amm.model.Pedido) {
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = repository.cambiarEstadoPedido(pedido.id)
            resultado.onSuccess {
                Toast.makeText(
                    requireContext(),
                    "Estado del pedido actualizado ✓",
                    Toast.LENGTH_SHORT
                ).show()
                cargarPedidos()
            }
            resultado.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    "Error: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("ListaPedidosFragment", "Error cambiando estado: ${error.message}")
            }
        }
    }

    private fun startPolling() {
        pollingRunnable = object : Runnable {
            override fun run() {
                cargarPedidos()
                handler.postDelayed(this, POLLING_INTERVAL)
            }
        }
        handler.postDelayed(pollingRunnable!!, POLLING_INTERVAL)
    }

    private fun stopPolling() {
        pollingRunnable?.let {
            handler.removeCallbacks(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPolling()
        _binding = null
    }
}

