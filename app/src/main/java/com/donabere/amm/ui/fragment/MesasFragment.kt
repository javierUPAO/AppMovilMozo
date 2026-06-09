package com.donabere.amm.ui.fragment

import android.content.ClipDescription
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.donabere.amm.databinding.ActivityMesasBinding
import com.donabere.amm.model.Mesa
import com.donabere.amm.model.enums.EstadoMesa
import com.donabere.amm.ui.CrearPedidoActivity
import com.donabere.amm.ui.DetallePedidoActivity
import com.donabere.amm.ui.adapter.ItemMesaAdapter
import com.donabere.amm.viewmodel.MesasViewModel
import com.donabere.amm.viewmodel.TurnoViewModel

class MesasFragment : Fragment() {

    private var _binding: ActivityMesasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MesasViewModel by viewModels()
    private val turnoViewModel: TurnoViewModel by viewModels()

    private var tieneTurnoActivo = false

    private val adapter = ItemMesaAdapter(
        onMesaClick = onMesaClick@{ mesa ->
            when (mesa.estado) {
                EstadoMesa.LIBRE -> {
                    if (!tieneTurnoActivo) {
                        Toast.makeText(
                            requireContext(),
                            "Debes abrir un turno primero",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@onMesaClick
                    }

                    val mozoIdActual = obtenerMozoId()
                    if (mozoIdActual.isEmpty()) {
                        Log.e("MesasFragment", "INTENTO FALLIDO: mozoId está vacío en SharedPreferences.")
                        Toast.makeText(requireContext(), "Error: ID de mozo no encontrado. Por favor, vuelve a iniciar sesión.", Toast.LENGTH_LONG).show()
                        return@onMesaClick
                    }

                    val mesasParaPedido = if (mesa.grupoId != null) mesa.mesasAgrupadas else listOf(mesa.id)
                    startActivity(
                        CrearPedidoActivity.newIntent(
                            context  = requireContext(),
                            mesasIds = mesasParaPedido,
                            mozoId   = mozoIdActual
                        )
                    )
                }
                EstadoMesa.OCUPADA -> {
                    val pedidoId = mesa.pedidoId ?: return@onMesaClick
                    startActivity(
                        DetallePedidoActivity.newIntent(
                            context  = requireContext(),
                            pedidoId = pedidoId,
                            mesaId   = mesa.id
                        )
                    )
                }
                else -> { /* ignorar */ }
            }
        },
        onMesaDropped = { source, target ->
            handleMesaDropped(source, target)
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityMesasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        viewModel.fetchMesas()
        turnoViewModel.verificarTurno(obtenerMozoId())
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchMesas()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        binding.rvMesas.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvMesas.adapter = adapter

        // Listener en la lista para detectar cuando se suelta una mesa en el fondo (separar)
        binding.rvMesas.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                }
                DragEvent.ACTION_DROP -> {
                    val sourceMesa = event.localState as? Mesa
                    if (sourceMesa != null && sourceMesa.grupoId != null) {
                        handleMesaSeparation(sourceMesa)
                    }
                    true
                }
                else -> true
            }
        }
    }

    private fun observeViewModel() {
        viewModel.mesas.observe(viewLifecycleOwner) { mesas ->
            adapter.submitList(mesas)
            binding.rvMesas.visibility = View.VISIBLE
            binding.tvError.visibility = View.GONE
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) binding.rvMesas.visibility = View.GONE
        }
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            binding.tvError.text = errorMessage
            binding.tvError.visibility = View.VISIBLE
            binding.rvMesas.visibility = View.GONE
        }

        turnoViewModel.turnoActivo.observe(viewLifecycleOwner) { turno ->
            tieneTurnoActivo = turno != null
        }
    }

    private fun handleMesaDropped(source: Mesa, target: Mesa) {
        if (source.estado == EstadoMesa.LIBRE && target.estado == EstadoMesa.LIBRE) {
            mostrarConfirmacionAgrupacion(source, target)
        } else if (source.estado == EstadoMesa.LIBRE && target.estado == EstadoMesa.OCUPADA) {
            mostrarConfirmacionUnion(source, target)
        } else if (source.estado == EstadoMesa.OCUPADA && target.estado == EstadoMesa.LIBRE) {
            mostrarConfirmacionUnion(target, source)
        } else {
            Toast.makeText(
                requireContext(),
                "No se pueden fusionar dos pedidos activos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mostrarConfirmacionAgrupacion(mesa1: Mesa, mesa2: Mesa) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Agrupar Mesas")
            .setMessage("¿Desea agrupar la Mesa ${mesa1.id.replace("m", "")} y la Mesa ${mesa2.id.replace("m", "")}?")
            .setPositiveButton("Confirmar") { _, _ ->
                viewModel.agruparMesasLibres(mesa1, mesa2)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarConfirmacionUnion(mesaLibre: Mesa, mesaOcupada: Mesa) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Asociar Mesa")
            .setMessage("¿Desea añadir la Mesa ${mesaLibre.id.replace("m", "")} al pedido existente de la Mesa ${mesaOcupada.id.replace("m", "")}?")
            .setPositiveButton("Confirmar") { _, _ ->
                viewModel.agregarMesaAPedido(mesaLibre, mesaOcupada)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun handleMesaSeparation(mesa: Mesa) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Separar Mesa")
            .setMessage("¿Desea separar la Mesa ${mesa.id.replace("m", "")} del grupo actual?")
            .setPositiveButton("Separar") { _, _ ->
                viewModel.separarMesaDeGrupo(mesa)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun obtenerMozoId(): String {
        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val id = prefs.getString("mozoId", "") ?: ""

        Log.d("MesasFragment", "MozoId recuperado de SharedPreferences: '$id'")

        return id
    }
}