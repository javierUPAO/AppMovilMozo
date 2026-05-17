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
import com.donabere.amm.adapter.ProductoAdapter
import com.donabere.amm.databinding.FragmentPlatosBinding
import com.donabere.amm.model.enums.TipoProducto
import com.donabere.amm.repository.PedidoRepository
import com.donabere.amm.ui.SeleccionProductoActivity
import com.donabere.amm.viewmodel.ProductoViewModel
import kotlinx.coroutines.launch

class PlatosFragment : Fragment() {

    private var _binding: FragmentPlatosBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ProductoViewModel by viewModels()
    private lateinit var adapter: ProductoAdapter

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
        viewModel.cargarProductosPorTipo(TipoProducto.PLATO)
    }

    private fun setupRecyclerView() {
        adapter = ProductoAdapter(emptyList()) { producto ->
            if (activity is SeleccionProductoActivity) {
                parentFragmentManager.setFragmentResult(
                    SeleccionProductoActivity.RESULT_PLATO,
                    bundleOf(
                        SeleccionProductoActivity.EXTRA_PRODUCTO_ID     to producto.id,
                        SeleccionProductoActivity.EXTRA_PRODUCTO_NOMBRE to producto.nombre,
                        SeleccionProductoActivity.EXTRA_PRODUCTO_PRECIO to producto.precio
                    )
                )
            }
        }
        binding.rvMenu.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvMenu.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.productos.observe(viewLifecycleOwner) { productos ->
            adapter.updateData(productos)
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