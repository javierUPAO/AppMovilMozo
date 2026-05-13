package com.donabere.amm.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.R
import com.donabere.amm.ui.adapter.DetallePedidoReadOnlyAdapter
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
                putExtra(EXTRA_MESA_ID, mesaId)
            }
    }

    private val db         = FirebaseFirestore.getInstance()
    private val pedidosRef = db.collection("pedidos")
    private val mesasRef   = db.collection("mesas_estado")
    private val stockRef   = db.collection("stock")

    private lateinit var tvMesa:       TextView
    private lateinit var tvTotal:      TextView
    private lateinit var tvVacio:      TextView
    private lateinit var rvDetalles:   RecyclerView
    private lateinit var btnCobrar:    Button
    private lateinit var progressBar:  ProgressBar

    private lateinit var pedidoId: String
    private var mesaId: Int = -1

    private val moneyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_pedido)

        pedidoId = intent.getStringExtra(EXTRA_PEDIDO_ID) ?: run { finish(); return }
        mesaId   = intent.getIntExtra(EXTRA_MESA_ID, -1)

        tvMesa      = findViewById(R.id.tv_mesa_label)
        tvTotal     = findViewById(R.id.tv_total)
        tvVacio     = findViewById(R.id.tv_vacio)
        rvDetalles  = findViewById(R.id.rv_detalles)
        btnCobrar   = findViewById(R.id.btn_cobrar)
        progressBar = findViewById(R.id.progress_bar)

        tvMesa.text = "Mesa $mesaId"

        rvDetalles.layoutManager = LinearLayoutManager(this)

        cargarDetalles()

        btnCobrar.setOnClickListener { confirmarCobro() }
    }

    private fun cargarDetalles() {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val detallesSnap = pedidosRef
                    .document(pedidoId)
                    .collection("detalles")
                    .get()
                    .await()

                val items = detallesSnap.documents.map { doc ->
                    DetallePedidoReadOnlyAdapter.Item(
                        nombre   = doc.getString("nombreProducto") ?: "",
                        cantidad = doc.getLong("cantidad")?.toInt() ?: 0,
                        subtotal = doc.getDouble("subtotal") ?: 0.0
                    )
                }

                val total = items.sumOf { it.subtotal }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (items.isEmpty()) {
                        tvVacio.visibility    = View.VISIBLE
                        rvDetalles.visibility = View.GONE
                        btnCobrar.isEnabled   = false
                    } else {
                        tvVacio.visibility    = View.GONE
                        rvDetalles.visibility = View.VISIBLE
                        rvDetalles.adapter    = DetallePedidoReadOnlyAdapter(items)
                        tvTotal.text          = "Total: ${moneyFormat.format(total)}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Error al cargar pedido: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

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

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Marcar pedido como PAGADO
                pedidosRef.document(pedidoId)
                    .update("estado", "PAGADO")
                    .await()

                // 2. Liberar mesa en Firestore
                mesasRef.document(mesaId.toString())
                    .delete()
                    .await()

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "✅ Mesa $mesaId liberada",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    // Volver a la lista de mesas
                    rvDetalles.postDelayed({ finish() }, 1000)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnCobrar.isEnabled    = true
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Error al procesar cobro: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}