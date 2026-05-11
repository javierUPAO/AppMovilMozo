package com.donabere.amm.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.donabere.amm.databinding.ActivityMesasBinding
import com.donabere.amm.ui.CrearPedidoActivity
import com.donabere.amm.ui.DetallePedidoActivity
import com.donabere.amm.ui.adapter.ItemMesaAdapter
import com.donabere.amm.viewmodel.MesasViewModel

class MesasFragment : Fragment() {

    private var _binding: ActivityMesasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MesasViewModel by viewModels()
    private val adapter = ItemMesaAdapter { mesa ->
        if (mesa.status == 0) {
            startActivity(
                CrearPedidoActivity.newIntent(
                    context  = requireContext(),
                    mesasIds = listOf(mesa.id),
                    mozoId   = 1
                )
            )
        } else {
            // Mesa ocupada → abrir detalle del pedido
            val pedidoId = mesa.pedidoId
            if (pedidoId != null) {
                startActivity(
                    DetallePedidoActivity.newIntent(
                        context  = requireContext(),
                        pedidoId = pedidoId,
                        mesaId   = mesa.id
                    )
                )
            } else {
                android.widget.Toast.makeText(
                    requireContext(),
                    "No se pudo obtener el pedido de la Mesa ${mesa.id}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        binding.rvMesas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMesas.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.mesas.observe(viewLifecycleOwner) { mesas ->
            adapter.submitList(mesas)
            binding.rvMesas.visibility = View.VISIBLE
            binding.tvError.visibility = View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.rvMesas.visibility = View.GONE
                binding.tvError.visibility = View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            binding.tvError.text = errorMessage
            binding.tvError.visibility = View.VISIBLE
            binding.rvMesas.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchMesas()
    }
}
