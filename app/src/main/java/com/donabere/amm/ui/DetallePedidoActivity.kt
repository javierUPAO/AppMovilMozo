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
    private lateinit var btnTransferir:     MaterialButton
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
        btnTransferir.setOnClickListener { abrirTransferirMesa() }
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    private fun bindViews() {
        tvMesa             = findViewById(R.id.tv_mesa_label)
        tvTotal            = findViewById(R.id.tv_total)
        tvVacio            = findViewById(R.id.tv_vacio)
        rvDetalles         = findViewById(R.id.rv_detalles)
        btnCobrar          = findViewById(R.id.btn_cobrar)
        btnDividir         = findViewById(R.id.btn_dividir)
        btnTransferir      = findViewById(R.id.btn_transferir)
        progressBar        = findViewById(R.id.progress_bar)
        llCuentasDivididas = findViewById(R.id.ll_cuentas_divididas)
        chipGroupCuentas   = findViewById(R.id.chip_group_cuentas)
    }

    // ── Cargar detalles ───────────────────────────────────────────────────────

    private fun cargarDetalles() {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Detalles del pedido (ahora están dentro de cuentas/{cuentaId}/detalles)
                val cuentasSnap = pedidosRef
                    .document(pedidoId)
                    .collection("cuentas")
                    .get()
                    .await()

                val todosLosDetalles = mutableListOf<DetallePedido>()

                for (cuentaDoc in cuentasSnap.documents) {
                    val detallesSnap = cuentaDoc.reference
                        .collection("detalles")
                        .get()
                        .await()

                    val detallesCuenta = detallesSnap.documents.map { doc ->
                        DetallePedido(
                            id             = doc.id,
                            productoId     = doc.getString("productoId")     ?: "",
                            nombreProducto = doc.getString("nombreProducto") ?: "",
                            precioUnitario = doc.getDouble("precioUnitario") ?: 0.0,
                            cantidad       = doc.getLong("cantidad")?.toInt() ?: 0,
                            nota           = doc.getString("nota")           ?: "",
                            anulado        = doc.getBoolean("anulado")       ?: false,
                            cuentaId       = cuentaDoc.id
                        )
                    }.filter { !it.anulado }

                    todosLosDetalles.addAll(detallesCuenta)
                }

                val total = todosLosDetalles.sumOf { it.subtotal }

                // 2. Cuentas divididas (si existen)
                val cuentas = cuentasSnap.documents.map { doc ->
                    Pair(
                        doc.getString("nombre")      ?: "Cuenta",
                        doc.getDouble("totalCuenta") ?: 0.0
                    )
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    detallesActuales = todosLosDetalles

                    if (todosLosDetalles.isEmpty()) {
                        tvVacio.visibility    = View.VISIBLE
                        rvDetalles.visibility = View.GONE
                        btnDividir.isEnabled  = false
                        btnCobrar.isEnabled   = false
                    } else {
                        tvVacio.visibility    = View.GONE
                        rvDetalles.visibility = View.VISIBLE
                        rvDetalles.adapter    = DetallePedidoReadOnlyAdapter(
                            todosLosDetalles.map { d ->
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

                // 2. Obtener mesas asociadas al pedido para liberarlas todas y desagruparlas
                val pedidoSnap = pedidosRef.document(pedidoId).get().await()
                val mesasIds = (pedidoSnap.get("mesasIds") as? List<*>)?.mapNotNull { it as? String } ?: listOf(mesaId)

                val batch = db.batch()
                mesasIds.forEach { mId ->
                    batch.update(
                        mesasRef.document(mId.trim()),
                        mapOf(
                            "estado" to "LIBRE",
                            "pedidoId" to null,
                            "grupoId" to null,
                            "mesasAgrupadas" to emptyList<String>()
                        )
                    )
                }
                batch.commit().await()

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

    // ── Transferencia ─────────────────────────────────────────────────────────

    private fun abrirTransferirMesa() {
        progressBar.visibility = View.VISIBLE
        btnTransferir.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener mesas libres
                val mesasSnap = mesasRef.whereEqualTo("estado", "LIBRE").get().await()
                val mesasLibres = mesasSnap.documents.mapNotNull { it.id }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnTransferir.isEnabled = true

                    if (mesasLibres.isEmpty()) {
                        mostrarError("No hay mesas libres disponibles.")
                        return@withContext
                    }

                    // Seleccionar mesa destino
                    val items = mesasLibres.toTypedArray()
                    MaterialAlertDialogBuilder(this@DetallePedidoActivity)
                        .setTitle("Transferir a Mesa")
                        .setItems(items) { _, which ->
                            val mesaDestinoId = items[which]
                            confirmarTransferencia(mesaDestinoId)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnTransferir.isEnabled = true
                    mostrarError("Error al cargar mesas libres: ${e.message}")
                }
            }
        }
    }

    private fun confirmarTransferencia(mesaDestinoId: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar Transferencia")
            .setMessage("¿Transferir el pedido de la Mesa $mesaId a la Mesa $mesaDestinoId?")
            .setPositiveButton("Confirmar") { _, _ -> procesarTransferencia(mesaDestinoId) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun procesarTransferencia(mesaDestinoId: String) {
        progressBar.visibility = View.VISIBLE
        btnTransferir.isEnabled = false
        btnCobrar.isEnabled    = false
        btnDividir.isEnabled   = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = com.donabere.amm.repository.PedidoRepository()
                val result = repository.transferirPedido(pedidoId, mesaId, mesaDestinoId)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (result.isSuccess) {
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            "✅ Pedido transferido a la Mesa $mesaDestinoId",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        rvDetalles.postDelayed({ finish() }, 1000)
                    } else {
                        btnTransferir.isEnabled = true
                        btnCobrar.isEnabled    = true
                        btnDividir.isEnabled   = true
                        val ex = result.exceptionOrNull()
                        mostrarError("Error al transferir pedido: ${ex?.message}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnTransferir.isEnabled = true
                    btnCobrar.isEnabled    = true
                    btnDividir.isEnabled   = true
                    mostrarError("Error inesperado: ${e.message}")
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