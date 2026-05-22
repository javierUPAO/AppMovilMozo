package com.donabere.amm.ui

import android.R
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.donabere.amm.databinding.DialogPagarCuentaBinding
import com.donabere.amm.model.Cuenta
import com.donabere.amm.repository.PedidoRepository
import com.donabere.amm.ui.adapter.CuentasAdapter
import com.donabere.amm.viewmodel.PedidoViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DialogPagarCuentaFragment : DialogFragment() {

    private var _binding: DialogPagarCuentaBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CuentasAdapter

    private var cuentaSeleccionada: Cuenta? = null
    private var pedidoId: String? = null
    private var mozoId: String?=null

    private fun obtenerMozoId(): String {
        val sharedPref = requireContext()
            .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        return sharedPref.getString("mozoId", "") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPagarCuentaBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val viewModel: PedidoViewModel by activityViewModels {
        PedidoViewModel.Factory(
            PedidoRepository(),
            obtenerMozoId()
        )
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        pedidoId = arguments?.getString(ARG_PEDIDO_ID) ?: return

        adapter = CuentasAdapter { cuenta ->

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirmar pago")
                .setMessage("¿Deseas pagar esta cuenta?")
                .setPositiveButton("Sí") { _, _ ->

                    viewModel.pagarCuenta(
                        pedidoId!!,
                        cuenta.id
                    ) {

                        viewModel.obtenerCuentas(pedidoId!!) { cuentas ->
                            adapter.submitList(cuentas)
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        binding.rvCuentas.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvCuentas.adapter = adapter

        // cargar cuentas iniciales
        viewModel.obtenerCuentas(pedidoId!!) { cuentas ->
            adapter.submitList(cuentas)
        }

        binding.btnCerrar.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog?.window?.setBackgroundDrawableResource(R.color.transparent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PEDIDO_ID = "pedido_id"

        fun newInstance(pedidoId: String): DialogPagarCuentaFragment {
            return DialogPagarCuentaFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PEDIDO_ID, pedidoId)
                }
            }
        }
    }
}