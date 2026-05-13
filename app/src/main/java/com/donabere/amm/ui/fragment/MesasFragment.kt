package com.donabere.amm.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.donabere.amm.databinding.ActivityMesasBinding
import com.donabere.amm.model.enums.EstadoMesa
import com.donabere.amm.ui.CrearPedidoActivity
import com.donabere.amm.ui.DetallePedidoActivity
import com.donabere.amm.ui.adapter.ItemMesaAdapter
import com.donabere.amm.viewmodel.MesasViewModel

class MesasFragment : Fragment() {

    private var _binding: ActivityMesasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MesasViewModel by viewModels()
    private val adapter = ItemMesaAdapter { mesa ->

        when (mesa.estado) {

            EstadoMesa.LIBRE -> {
                startActivity(
                    CrearPedidoActivity.newIntent(
                        requireContext(),
                        mesasIds = listOf(mesa.id),
                        mozoId = ""
                    )
                )
            }

            EstadoMesa.OCUPADA -> {
                // si aún tienes pedidoId en otro lado lo manejas aquí
                Toast.makeText(
                    requireContext(),
                    "Mesa ocupada: ${mesa.id}",
                    Toast.LENGTH_SHORT
                ).show()

                // o abrir detalle si luego agregas pedidoId al modelo
            }

            else -> {
                Toast.makeText(
                    requireContext(),
                    "Estado desconocido",
                    Toast.LENGTH_SHORT
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
        binding.rvMesas.layoutManager = GridLayoutManager(requireContext(), 2)
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
