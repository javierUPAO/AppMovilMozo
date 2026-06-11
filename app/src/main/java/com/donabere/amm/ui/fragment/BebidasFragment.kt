package com.donabere.amm.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.donabere.amm.adapter.ProductoAdapter
import com.donabere.amm.databinding.FragmentBebidasBinding
import com.donabere.amm.model.enums.TipoProducto
import com.donabere.amm.ui.SeleccionProductoActivity
import com.donabere.amm.viewmodel.ProductoViewModel

class BebidasFragment : Fragment() {

    companion object {
        fun newInstance(
            onProductoSeleccionado: ((productoId: String, nombre: String, precio: Double, imagen: String) -> Unit)? = null,
            onNotaClick: ((productoId: String, nombre: String, precio: Double, imagen: String) -> Unit)? = null
        ) = BebidasFragment().also {
            it.onProductoSeleccionado = onProductoSeleccionado
            it.onNotaClick = onNotaClick
        }
    }

    private var _binding: FragmentBebidasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductoViewModel by viewModels()
    private lateinit var adapter: ProductoAdapter
    private lateinit var buscadorAdapter: ArrayAdapter<String>

    var onProductoSeleccionado: ((productoId: String, nombre: String, precio: Double, imagen: String) -> Unit)? = null
    var onNotaClick: ((productoId: String, nombre: String, precio: Double, imagen: String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBebidasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupBuscador()
        observeViewModel()
        viewModel.cargarProductosPorTipo(TipoProducto.BEBIDA)
    }

    private fun setupBuscador() {
        buscadorAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        binding.actBuscadorBebidas.setAdapter(buscadorAdapter)

        // filtra la grilla en tiempo real conforme se escribe
        binding.actBuscadorBebidas.addTextChangedListener { text ->
            adapter.filter(text?.toString() ?: "")
        }

        // se toca una sugerencia de la lista
        binding.actBuscadorBebidas.setOnItemClickListener { _, _, position, _ ->
            val nombreBebida = buscadorAdapter.getItem(position) ?: return@setOnItemClickListener

            val imm = requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.actBuscadorBebidas.windowToken, 0)

            binding.actBuscadorBebidas.clearFocus()
        }
    }

    private fun setupRecyclerView() {
        val notaCallback = onNotaClick?.let { cb ->
            { producto: com.donabere.amm.model.Producto ->
                cb(producto.id, producto.nombre, producto.precio, producto.imagen)
            }
        }
        adapter = ProductoAdapter(
            emptyList(),
            onProductoClick = { producto ->
                val callback = onProductoSeleccionado
                if (callback != null) {
                    callback(producto.id, producto.nombre, producto.precio, producto.imagen)
                } else {
                    parentFragmentManager.setFragmentResult(
                        SeleccionProductoActivity.RESULT_BEBIDA,
                        androidx.core.os.bundleOf(
                            SeleccionProductoActivity.EXTRA_PRODUCTO_ID     to producto.id,
                            SeleccionProductoActivity.EXTRA_PRODUCTO_NOMBRE to producto.nombre,
                            SeleccionProductoActivity.EXTRA_PRODUCTO_PRECIO to producto.precio
                        )
                    )
                }
            },
            onNotaClick = notaCallback
        )
        binding.rvBebidas.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvBebidas.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.productos.observe(viewLifecycleOwner) { productos ->
            // Actualiza el RecyclerView
            adapter.updateData(productos)
            // Actualiza las sugerencias del buscador con los nombres reales de la BD
            buscadorAdapter.clear()
            buscadorAdapter.addAll(productos.map { it.nombre })
            buscadorAdapter.notifyDataSetChanged()
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.pbBebidasLoading.visibility = if (loading) View.VISIBLE else View.GONE
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