package com.donabere.amm.ui.adapter

import android.content.ClipData
import android.content.ClipDescription
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.DragEvent
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.databinding.ItemMesaBinding
import com.donabere.amm.model.Mesa
import com.donabere.amm.model.enums.EstadoMesa
import com.donabere.amm.R

class ItemMesaAdapter(
    private val onMesaClick: (Mesa) -> Unit,
    private val onMesaDropped: (source: Mesa, target: Mesa) -> Unit
) : RecyclerView.Adapter<ItemMesaAdapter.MesaViewHolder>() {

    private var mesasList = listOf<Mesa>()

    fun submitList(mesas: List<Mesa>) {
        this.mesasList = mesas
        notifyItemRangeChanged(0, mesas.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MesaViewHolder {
        val binding = ItemMesaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MesaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MesaViewHolder, position: Int) {
        holder.bind(mesasList[position])
    }

    override fun getItemCount() = mesasList.size

    inner class MesaViewHolder(
        private val binding: ItemMesaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mesa: Mesa) {
            // Identificador visual de agrupación (ej. "Mesa 1+2" o "Mesa 1")
            val displayId = if (mesa.grupoId != null && mesa.mesasAgrupadas.isNotEmpty()) {
                val sortedNumbers = mesa.mesasAgrupadas
                    .map { it.replace("m", "").toIntOrNull() ?: 0 }
                    .sorted()
                "Mesa " + sortedNumbers.joinToString("+")
            } else {
                "Mesa ${mesa.id.replace("m", "")}"
            }
            binding.tvIdMesa.text = displayId

            // Calcular tiempo de espera y estados de alerta
            var strokeColor = Color.TRANSPARENT
            var strokeWidth = 0
            var diffMin = 0
            var isDelayed = false
            var isWarning = false

            if (mesa.estado == EstadoMesa.OCUPADA && mesa.ultimaAtencion != null) {
                val now = com.google.firebase.Timestamp.now().seconds
                val diffSec = now - mesa.ultimaAtencion.seconds
                diffMin = (diffSec / 60).toInt()
                if (diffMin >= 10) {
                    isDelayed = true
                    strokeColor = Color.parseColor("#E74C3C") // Rojo para Demora
                    strokeWidth = 6
                } else if (diffMin >= 5) {
                    isWarning = true
                    strokeColor = Color.parseColor("#F1C40F") // Amarillo para Próxima a demora
                    strokeWidth = 6
                }
            }

            if (!isDelayed && !isWarning && mesa.grupoId != null) {
                strokeColor = Color.parseColor("#4A90E2") // Azul para agrupación normal
                strokeWidth = 6
            }

            binding.root.setStrokeColor(ColorStateList.valueOf(strokeColor))
            binding.root.strokeWidth = strokeWidth

            // Habilitar Drag & Drop
            binding.root.setOnLongClickListener { view ->
                val clipItem = ClipData.Item(mesa.id)
                val dragData = ClipData(
                    mesa.id,
                    arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                    clipItem
                )
                val shadow = View.DragShadowBuilder(view)
                view.startDragAndDrop(dragData, shadow, mesa, 0)
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                true
            }

            binding.root.setOnDragListener { view, event ->
                val card = view as com.google.android.material.card.MaterialCardView
                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
                        event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                    }
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        val colorHover = if (mesa.estado == EstadoMesa.OCUPADA) "#E67E22" else "#2ECC71"
                        card.setStrokeColor(ColorStateList.valueOf(Color.parseColor(colorHover)))
                        card.strokeWidth = 8
                        true
                    }
                    DragEvent.ACTION_DRAG_EXITED -> {
                        card.setStrokeColor(ColorStateList.valueOf(strokeColor))
                        card.strokeWidth = strokeWidth
                        true
                    }
                    DragEvent.ACTION_DROP -> {
                        val sourceMesa = event.localState as? Mesa
                        if (sourceMesa != null && sourceMesa.id != mesa.id) {
                            onMesaDropped(sourceMesa, mesa)
                        }
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        card.setStrokeColor(ColorStateList.valueOf(strokeColor))
                        card.strokeWidth = strokeWidth
                        true
                    }
                    else -> false
                }
            }

            when (mesa.estado) {
                EstadoMesa.LIBRE -> {
                    binding.ivIconoMesa.setImageResource(R.drawable.ic_table_new)
                    binding.ivIconoMesa.clearColorFilter()
                    binding.tvIdMesa.setTextColor(Color.BLACK)
                    binding.tvCapacidad.text = ""
                    binding.tvCapacidadTexto.text = ""
                    binding.tvEstado.text = ""
                    binding.llAlertaDemora.visibility = View.GONE

                    binding.root.alpha = 1f
                    binding.root.isEnabled = true
                    binding.root.setOnClickListener { onMesaClick(mesa) }
                }

                EstadoMesa.OCUPADA -> {
                    binding.ivIconoMesa.setImageResource(R.drawable.ic_table_selected)
                    binding.tvIdMesa.setTextColor(Color.parseColor("#CACACA"))
                    binding.tvCapacidad.text = if (mesa.numClientes > 0)
                        "${mesa.numClientes}" else ""
                    binding.tvCapacidadTexto.text = if (mesa.numClientes > 0) "Personas" else ""
                    binding.tvEstado.text = "OCUPADA"
                    binding.tvEstado.setTextColor(Color.parseColor("#F5A623"))

                    if (isDelayed) {
                        binding.llAlertaDemora.visibility = View.VISIBLE
                        binding.ivAlertaDemora.setColorFilter(Color.parseColor("#E74C3C"))
                        binding.tvTiempoEspera.text = "DEMORA: ${diffMin} min"
                        binding.tvTiempoEspera.setTextColor(Color.parseColor("#E74C3C"))
                    } else if (isWarning) {
                        binding.llAlertaDemora.visibility = View.VISIBLE
                        binding.ivAlertaDemora.setColorFilter(Color.parseColor("#F1C40F"))
                        binding.tvTiempoEspera.text = "ESPERA: ${diffMin} min"
                        binding.tvTiempoEspera.setTextColor(Color.parseColor("#F1C40F"))
                    } else {
                        binding.llAlertaDemora.visibility = View.GONE
                    }

                    binding.root.alpha = 1f
                    binding.root.isEnabled = true
                    binding.root.setOnClickListener { onMesaClick(mesa) }
                }

                else -> {
                    binding.tvEstado.setTextColor(Color.GRAY)
                    binding.ivIconoMesa.setColorFilter(Color.GRAY)
                    binding.llAlertaDemora.visibility = View.GONE
                    binding.root.alpha = 0.5f
                    binding.root.isEnabled = false
                    binding.root.setOnClickListener(null)
                }
            }
        }
    }
}