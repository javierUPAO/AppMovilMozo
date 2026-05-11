package com.donabere.amm.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.donabere.amm.adapter.MenuAdapter
import com.donabere.amm.databinding.FragmentPlatosBinding
import com.donabere.amm.repository.PedidoRepository
import com.donabere.amm.ui.SeleccionProductoActivity
import com.donabere.amm.viewmodel.MenuViewModel
import kotlinx.coroutines.launch

class PlatosFragment : Fragment() {

    private var _binding: FragmentPlatosBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: MenuViewModel by viewModels()
    private lateinit var adapter: MenuAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlatosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
        viewModel.cargarMenu()
    }

    private fun setupRecyclerView() {
        adapter = MenuAdapter(emptyList()) { dish ->
            // Si viene de SeleccionProductoActivity → devolver resultado
            if (activity is SeleccionProductoActivity) {
                parentFragmentManager.setFragmentResult(
                    SeleccionProductoActivity.RESULT_PLATO,
                    bundleOf(
                        SeleccionProductoActivity.EXTRA_PRODUCTO_ID     to dish.id,
                        SeleccionProductoActivity.EXTRA_PRODUCTO_NOMBRE to dish.title,
                        SeleccionProductoActivity.EXTRA_PRODUCTO_PRECIO to dish.price
                    )
                )
            } else {
                Toast.makeText(requireContext(), "Plato seleccionado: ${dish.title}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvMenu.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvMenu.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.menu.observe(viewLifecycleOwner) { dishes ->
            adapter.updateData(dishes)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.pbMenuLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}