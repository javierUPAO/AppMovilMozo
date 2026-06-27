package com.donabere.amm.ui.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.donabere.amm.R
import com.donabere.amm.model.DetallePedido
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class DialogAnulacionFragment : DialogFragment() {

    companion object {
        const val TAG = "DialogAnulacion"

        // Clave del resultado que escucha DetallePedidoActivity
        const val REQUEST_KEY  = "anulacion_request"
        const val RESULT_MOTIVO  = "motivo"
        const val RESULT_DETALLE_ID = "detalle_id"
        const val RESULT_CUENTA_ID  = "cuenta_id"

        private const val ARG_NOMBRE    = "nombre"
        private const val ARG_DETALLE_ID = "detalle_id"
        private const val ARG_CUENTA_ID  = "cuenta_id"

        fun newInstance(detalle: DetallePedido): DialogAnulacionFragment {
            return DialogAnulacionFragment().apply {
                arguments = bundleOf(
                    ARG_NOMBRE     to detalle.nombreProducto,
                    ARG_DETALLE_ID to detalle.id,
                    ARG_CUENTA_ID  to (detalle.cuentaId ?: "")
                )
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val nombreProducto = arguments?.getString(ARG_NOMBRE) ?: ""
        val detalleId      = arguments?.getString(ARG_DETALLE_ID) ?: ""
        val cuentaId       = arguments?.getString(ARG_CUENTA_ID) ?: ""

        val view = layoutInflater.inflate(R.layout.dialog_anulacion, null)

        val tilMotivo  = view.findViewById<TextInputLayout>(R.id.til_motivo)
        val etMotivo   = view.findViewById<TextInputEditText>(R.id.et_motivo)
        val tvError    = view.findViewById<TextView>(R.id.tv_error_motivo)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Anular \"$nombreProducto\"")
            .setView(view)
            .setNegativeButton("Cancelar") { _, _ -> dismiss() }
            .setPositiveButton("Anular") { _, _ ->
                // No hace nada aquí — lo manejamos abajo para validar antes de cerrar
            }
            .create()
            .also { dialog ->
                // Sobreescribir el click del positivo para validar ANTES de cerrar
                dialog.setOnShowListener {
                    dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                        val motivo = etMotivo.text?.toString()?.trim() ?: ""

                        if (motivo.isBlank()) {
                            tilMotivo.isErrorEnabled = true
                            tilMotivo.error = "El motivo es obligatorio"
                            tvError.visibility = View.GONE // til_motivo ya muestra el error
                            return@setOnClickListener
                        }

                        // Motivo válido → devolver resultado y cerrar
                        tilMotivo.isErrorEnabled = false
                        setFragmentResult(
                            REQUEST_KEY,
                            bundleOf(
                                RESULT_MOTIVO     to motivo,
                                RESULT_DETALLE_ID to detalleId,
                                RESULT_CUENTA_ID  to cuentaId
                            )
                        )
                        dismiss()
                    }
                }
            }
    }
}