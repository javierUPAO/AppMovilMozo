package com.donabere.amm.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.R
import com.donabere.amm.model.Mozo
import com.google.android.material.button.MaterialButton

class SeleccionarMozoDialog : DialogFragment() {

    private lateinit var rvMozos: RecyclerView
    private lateinit var btnCancelar: MaterialButton

    private var mozos: List<Mozo> = emptyList()
    private var onMozoSeleccionado: ((Mozo) -> Unit)? = null

    companion object {
        fun newInstance(mozos: List<Mozo>, onSeleccionado: (Mozo) -> Unit): SeleccionarMozoDialog {
            val dialog = SeleccionarMozoDialog()
            dialog.mozos = mozos
            dialog.onMozoSeleccionado = onSeleccionado
            return dialog
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_seleccionar_mozo, container, false)
        
        rvMozos = view.findViewById(R.id.rv_mozos)
        btnCancelar = view.findViewById(R.id.btn_cancelar)

        rvMozos.layoutManager = LinearLayoutManager(requireContext())
        rvMozos.adapter = MozoAdapter(mozos) { mozo ->
            onMozoSeleccionado?.invoke(mozo)
            dismiss()
        }

        btnCancelar.setOnClickListener {
            dismiss()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private inner class MozoAdapter(
        private val list: List<Mozo>,
        private val onCLick: (Mozo) -> Unit
    ) : RecyclerView.Adapter<MozoAdapter.MozoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MozoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mozo, parent, false)
            return MozoViewHolder(view)
        }

        override fun onBindViewHolder(holder: MozoViewHolder, position: Int) {
            val mozo = list[position]
            holder.tvNombre.text = "${mozo.name} ${mozo.lastname}".trim()
            holder.itemView.setOnClickListener {
                onCLick(mozo)
            }
        }

        override fun getItemCount(): Int = list.size

        inner class MozoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvNombre: TextView = itemView.findViewById(R.id.tv_nombre_mozo)
        }
    }
}
