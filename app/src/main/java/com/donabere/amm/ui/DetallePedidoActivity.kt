package com.donabere.amm.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.R
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.ui.adapter.DetallePedidoReadOnlyAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class DetallePedidoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PEDIDO_ID = "extra_pedido_id"
        const val EXTRA_MESA_ID   = "extra_mesa_id"

        fun newIntent(context: Context, pedidoId: String, mesaId: String): Intent =
            Intent(context, DetallePedidoActivity::class.java).apply {
                putExtra(EXTRA_PEDIDO_ID, pedidoId)
                putExtra(EXTRA_MESA_ID,   mesaId)
            }
    }

    private val db         = FirebaseFirestore.getInstance()
    private val pedidosRef = db.collection("pedidos")
    private val mesasRef   = db.collection("mesas")

    private lateinit var tvMesa:            TextView
    private lateinit var tvTotal:           TextView
    private lateinit var tvVacio:           TextView
    private lateinit var rvDetalles:        RecyclerView
    private lateinit var btnCobrar:         MaterialButton
    private lateinit var btnDividir:        MaterialButton
    private lateinit var progressBar:       ProgressBar
    private lateinit var llCuentasDivididas: View
    private lateinit var chipGroupCuentas:  ChipGroup

    private lateinit var pedidoId: String
    private lateinit var mesaId:   String

    /** Detalles cargados de Firestore, necesarios para pasarlos al dialog */
    private var detallesActuales: List<DetallePedido> = emptyList()

    private val moneyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"))

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_pedido)

        pedidoId = intent.getStringExtra(EXTRA_PEDIDO_ID) ?: run { finish(); return }
        mesaId   = intent.getStringExtra(EXTRA_MESA_ID)   ?: run { finish(); return }

        bindViews()
        tvMesa.text = "Mesa $mesaId"
        rvDetalles.layoutManager = LinearLayoutManager(this)

        cargarDetalles()

        btnDividir.setOnClickListener { abrirDividirCuenta() }
        btnCobrar.setOnClickListener  { confirmarCobro() }
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    private fun bindViews() {
        tvMesa             = findViewById(R.id.tv_mesa_label)
        tvTotal            = findViewById(R.id.tv_total)
        tvVacio            = findViewById(R.id.tv_vacio)
        rvDetalles         = findViewById(R.id.rv_detalles)
        btnCobrar          = findViewById(R.id.btn_cobrar)
        btnDividir         = findViewById(R.id.btn_dividir)
        progressBar        = findViewById(R.id.progress_bar)
        llCuentasDivididas = findViewById(R.id.ll_cuentas_divididas)
        chipGroupCuentas   = findViewById(R.id.chip_group_cuentas)
    }

    // ── Cargar detalles ───────────────────────────────────────────────────────

    private fun cargarDetalles() {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Detalles del pedido
                val detallesSnap = pedidosRef
                    .document(pedidoId)
                    .collection("detalles")
                    .get()
                    .await()

                val detalles = detallesSnap.documents.map { doc ->
                    DetallePedido(
                        id             = doc.id,
                        productoId     = doc.getString("productoId")     ?: "",
                        nombreProducto = doc.getString("nombreProducto") ?: "",
                        precioUnitario = doc.getDouble("precioUnitario") ?: 0.0,
                        cantidad       = doc.getLong("cantidad")?.toInt() ?: 0,
                        nota           = doc.getString("nota")           ?: "",
                        anulado        = doc.getBoolean("anulado")       ?: false
                    )
                }.filter { !it.anulado }

                val total = detalles.sumOf { it.subtotal }

                // 2. Cuentas divididas (si existen)
                val cuentasSnap = pedidosRef
                    .document(pedidoId)
                    .collection("cuentas")
                    .get()
                    .await()

                val cuentas = cuentasSnap.documents.map { doc ->
                    Pair(
                        doc.getString("nombre")      ?: "Cuenta",
                        doc.getDouble("totalCuenta") ?: 0.0
                    )
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    detallesActuales = detalles

                    if (detalles.isEmpty()) {
                        tvVacio.visibility    = View.VISIBLE
                        rvDetalles.visibility = View.GONE
                        btnDividir.isEnabled  = false
                        btnCobrar.isEnabled   = false
                    } else {
                        tvVacio.visibility    = View.GONE
                        rvDetalles.visibility = View.VISIBLE
                        rvDetalles.adapter    = DetallePedidoReadOnlyAdapter(
                            detalles.map { d ->
                                DetallePedidoReadOnlyAdapter.Item(
                                    nombre   = d.nombreProducto,
                                    cantidad = d.cantidad,
                                    subtotal = d.subtotal
                                )
                            }
                        )
                        tvTotal.text = "Total: ${moneyFormat.format(total)}"
                    }

                    // Mostrar cuentas divididas si ya se guardaron
                    if (cuentas.isNotEmpty()) {
                        mostrarCuentasDivididas(cuentas)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    mostrarError("Error al cargar pedido: ${e.message}")
                }
            }
        }
    }

    // ── Dialog dividir cuenta ─────────────────────────────────────────────────

    private fun abrirDividirCuenta() {
        if (detallesActuales.isEmpty()) return

        DividirCuentaDialog.show(
            fm        = supportFragmentManager,
            pedidoId  = pedidoId,
            detalles  = detallesActuales,
            onGuardado = {
                // Recargar para mostrar las cuentas nuevas
                chipGroupCuentas.removeAllViews()
                cargarDetalles()
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "División guardada correctamente",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        )
    }

    // ── Mostrar chips de cuentas ──────────────────────────────────────────────

    private fun mostrarCuentasDivididas(cuentas: List<Pair<String, Double>>) {
        llCuentasDivididas.visibility = View.VISIBLE
        chipGroupCuentas.removeAllViews()

        cuentas.forEach { (nombre, total) ->
            val chip = Chip(this).apply {
                text = "$nombre\n${moneyFormat.format(total)}"
                isClickable = false
                isCheckable = false
                setChipBackgroundColorResource(R.color.chip_unselected_bg)
                setTextColor(getColor(R.color.text_primary))
            }
            chipGroupCuentas.addView(chip)
        }
    }

    // ── Cobro ─────────────────────────────────────────────────────────────────

    private fun confirmarCobro() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar cuenta")
            .setMessage("¿Confirmar cobro y liberar Mesa $mesaId?")
            .setPositiveButton("Confirmar") { _, _ -> procesarCobro() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun procesarCobro() {
        progressBar.visibility = View.VISIBLE
        btnCobrar.isEnabled    = false
        btnDividir.isEnabled   = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Marcar pedido como PAGADO
                pedidosRef.document(pedidoId)
                    .update("estado", "PAGADO")
                    .await()

                // 2. Liberar mesa en Firestore (Hacemos UPDATE, no DELETE)
                mesasRef.document(mesaId.trim())
                    .update(
                        mapOf(
                            "estado" to "LIBRE"
                        )
                    )
                    .await()

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "✅ Mesa $mesaId liberada",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    rvDetalles.postDelayed({ finish() }, 1000)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnCobrar.isEnabled    = true
                    btnDividir.isEnabled   = true
                    mostrarError("Error al procesar cobro: ${e.message}")
                }
            }
        }
    }

    // ── Util ──────────────────────────────────────────────────────────────────

    private fun mostrarError(msg: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            msg,
            Snackbar.LENGTH_LONG
        ).show()
    }
}