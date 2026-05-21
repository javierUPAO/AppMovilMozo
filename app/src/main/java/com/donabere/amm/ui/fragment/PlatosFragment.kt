package com.donabere.amm.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.donabere.amm.adapter.ProductoAdapter
import com.donabere.amm.databinding.FragmentPlatosBinding
import com.donabere.amm.model.enums.TipoProducto
import com.donabere.amm.ui.SeleccionProductoActivity
import com.donabere.amm.viewmodel.ProductoViewModel

class PlatosFragment : Fragment() {

    companion object {
        fun newInstance(
            onProductoSeleccionado: ((productoId: String, nombre: String, precio: Double, imagen: String) -> Unit)? = null
        ) = PlatosFragment().also { it.onProductoSeleccionado = onProductoSeleccionado }
    }

    private var _binding: FragmentPlatosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductoViewModel by viewModels()
    private lateinit var adapter: ProductoAdapter

    var onProductoSeleccionado: ((productoId: String, nombre: String, precio: Double, imagen: String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
            val callback = onProductoSeleccionado
            if (callback != null) {
                // Modo carrito → llama directo a CrearPedidoActivity
                callback(producto.id, producto.nombre, producto.precio, producto.imagen)
            } else {
                // Modo legacy → SeleccionProductoActivity escucha este result
                // ID se pasa como String (ya no Int)
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
        viewModel.productos.observe(viewLifecycleOwner) { adapter.updateData(it) }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.pbMenuLoading.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(viewLifecycleOwner) { msg ->
            if (msg.isNotEmpty()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}