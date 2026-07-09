package com.donabere.amm.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.donabere.amm.databinding.DialogDividirCuentaBinding
import com.donabere.amm.repository.PedidoRepository
import com.donabere.amm.ui.adapter.CuentasAdapter
import com.donabere.amm.ui.adapter.CuentasDivisionAdapter
import com.donabere.amm.ui.adapter.CuentasResumenDivisionAdapter
import com.donabere.amm.ui.adapter.ProductosDividirAdapter
import com.donabere.amm.viewmodel.DividirCuentaViewModel
import com.google.android.material.snackbar.Snackbar

class DialogDividirCuentaFragment : DialogFragment() {

    private var _binding: DialogDividirCuentaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DividirCuentaViewModel by viewModels {
        DividirCuentaViewModel.Factory(PedidoRepository())
    }

    private lateinit var productosAdapter: ProductosDividirAdapter
    private lateinit var cuentasAdapter: CuentasDivisionAdapter

    private lateinit var resumenAdapter: CuentasResumenDivisionAdapter

    private var pedidoId: String = ""

    companion object {
        private const val ARG_PEDIDO_ID = "pedido_id"

        fun newInstance(pedidoId: String): DialogDividirCuentaFragment {
            return DialogDividirCuentaFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PEDIDO_ID, pedidoId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pedidoId = arguments?.getString(ARG_PEDIDO_ID) ?: ""
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDividirCuentaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupObservers()
        setupListeners()

        viewModel.cargarPedido(pedidoId)
    }

    private fun setupAdapters() {

        productosAdapter = ProductosDividirAdapter()

        cuentasAdapter = CuentasDivisionAdapter(
            onProductoDrop = { productoKey, cuentaId ->
                viewModel.asignarProductoACuenta(
                    productoKey = productoKey,
                    cuentaId = cuentaId
                )
            },
            onIncrementarDetalle = { cuentaId, detalle ->
                viewModel.incrementarDetalle(cuentaId, detalle)
            },
            onDecrementarDetalle = { cuentaId, detalle ->
                viewModel.decrementarDetalle(cuentaId, detalle)
            },
            onEliminarDetalle = { cuentaId, detalle ->
                viewModel.quitarDetalleDeCuenta(cuentaId, detalle)

                Snackbar.make(
                    binding.root,
                    "${detalle.nombreProducto} eliminado",
                    Snackbar.LENGTH_LONG
                ).setAction("Deshacer") {
                    viewModel.deshacerEliminacion()
                }.show()
            },
            onEliminarCuenta = { cuentaId ->
                viewModel.eliminarCuenta(cuentaId)
            }


        )

        binding.rvProductos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productosAdapter
        }

        binding.rvCuentas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cuentasAdapter
        }

        resumenAdapter = CuentasResumenDivisionAdapter()

        binding.rvResumenCuentas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = resumenAdapter}
    }

    private fun setupObservers() {

        viewModel.productos.observe(viewLifecycleOwner) { productos ->
            productosAdapter.submitList(productos)
        }

        viewModel.cuentas.observe(viewLifecycleOwner) { cuentas ->
            cuentasAdapter.submitList(cuentas)
            resumenAdapter.submitList(cuentas)
            val total = cuentas.sumOf { it.total }
            binding.tvTotalDividir.text = "S/. %.2f".format(total)
            binding.tvPersonasPagan.text = cuentas.size.toString()
        }

        viewModel.uiState.observe(viewLifecycleOwner) { mensaje ->
            mensaje?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()

                if (it == "División guardada") {
                    dismiss()
                }
            }
        }
    }

    private fun setupListeners() {

        binding.btnCerrar.setOnClickListener {
            dismiss()
        }

        binding.btnCancelar.setOnClickListener {
            dismiss()
        }

        binding.btnAgregarCuenta.setOnClickListener {
            viewModel.agregarCuenta()
        }

        binding.btnGuardarDivision.setOnClickListener {
            viewModel.guardarDivision()
        }

        binding.tvTabAsignar.setOnClickListener {
            mostrarAsignar()
        }

        binding.tvTabResumen.setOnClickListener {
            mostrarResumen()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun mostrarAsignar() {
        binding.layoutAsignar.visibility = View.VISIBLE
        binding.layoutResumen.visibility = View.GONE

        binding.tvTabAsignar.setTextColor(Color.parseColor("#F59E0B"))
        binding.tvTabAsignar.setTypeface(null, android.graphics.Typeface.BOLD)

        binding.tvTabResumen.setTextColor(Color.parseColor("#8A8A8A"))
        binding.tvTabResumen.setTypeface(null, android.graphics.Typeface.NORMAL)
    }

    private fun mostrarResumen() {
        binding.layoutAsignar.visibility = View.GONE
        binding.layoutResumen.visibility = View.VISIBLE

        binding.tvTabAsignar.setTextColor(Color.parseColor("#8A8A8A"))
        binding.tvTabAsignar.setTypeface(null, android.graphics.Typeface.NORMAL)

        binding.tvTabResumen.setTextColor(Color.parseColor("#F59E0B"))
        binding.tvTabResumen.setTypeface(null, android.graphics.Typeface.BOLD)
    }
}