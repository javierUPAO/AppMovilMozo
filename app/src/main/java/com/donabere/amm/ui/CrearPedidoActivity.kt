package com.donabere.amm.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.R
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.repository.PedidoRepository
import com.donabere.amm.ui.adapter.DetallePedidoAdapter
import com.donabere.amm.viewmodel.PedidoViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.util.Locale

class CrearPedidoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MESAS_IDS = "extra_mesas_ids"
        const val EXTRA_MOZO_ID   = "extra_mozo_id"

        fun newIntent(context: Context, mesasIds: List<String>, mozoId: String): Intent =
            Intent(context, CrearPedidoActivity::class.java).apply {
                putExtra(EXTRA_MESAS_IDS, mesasIds.toString())
                putExtra(EXTRA_MOZO_ID, mozoId)
            }
    }

    private val viewModel: PedidoViewModel by viewModels {
        val repo = PedidoRepository()
        PedidoViewModel.Factory(repo, mozoId)
    }

    private val seleccionProductoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val id     = data?.getStringExtra(SeleccionProductoActivity.EXTRA_PRODUCTO_ID) ?: ""
            val nombre = data?.getStringExtra(SeleccionProductoActivity.EXTRA_PRODUCTO_NOMBRE) ?: ""
            val precio = data?.getDoubleExtra(SeleccionProductoActivity.EXTRA_PRODUCTO_PRECIO, 0.0) ?: 0.0

            if (id != "" && nombre.isNotBlank()) {
                viewModel.agregarProducto(
                    mesasIds       = mesasIds,
                    productoId     = id,
                    nombreProducto = nombre,
                    precioUnitario = precio
                )
            }
        }
    }

    private lateinit var rvDetalles: RecyclerView
    private lateinit var tvTotal: TextView
    private lateinit var tvMesas: TextView
    private lateinit var tvVacio: TextView
    private lateinit var btnAgregarProducto: Button
    private lateinit var btnConfirmar: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var adapter: DetallePedidoAdapter
    private val moneyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"))

    private lateinit var mesasIds: List<String>
    private var mozoId: String = ""

    private var ultimoEliminado: DetallePedido? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_pedido)

        mesasIds = intent.getStringArrayExtra(EXTRA_MESAS_IDS)?.toList() ?: listOf()
        mozoId   = intent.getStringExtra(EXTRA_MOZO_ID)?: ""

        initViews()
        setupRecyclerView()
        setupObservers()
        setupListeners()

        tvMesas.text = if (mesasIds.size == 1)
            "Mesa ${mesasIds.first()}"
        else
            "Mesas ${mesasIds.joinToString(" + ")}"
    }


    private fun initViews() {
        rvDetalles         = findViewById(R.id.rv_detalles_pedido)
        tvTotal            = findViewById(R.id.tv_total_pedido)
        tvMesas            = findViewById(R.id.tv_mesas_label)
        tvVacio            = findViewById(R.id.tv_pedido_vacio)
        btnAgregarProducto = findViewById(R.id.btn_agregar_producto)
        btnConfirmar       = findViewById(R.id.btn_confirmar_pedido)
        progressBar        = findViewById(R.id.progress_bar)
    }

    private fun setupRecyclerView() {
        adapter = DetallePedidoAdapter(
            onIncrement = { detalle ->
                viewModel.actualizarCantidad(detalle, detalle.cantidad + 1)
            },
            onDecrement = { detalle ->
                viewModel.actualizarCantidad(detalle, detalle.cantidad - 1)
            },
            onDelete = { detalle ->
                eliminarConUndo(detalle)
            },
            onEditNota = { detalle ->
                mostrarDialogoNota(detalle)
            }
        )

        rvDetalles.layoutManager = LinearLayoutManager(this)
        rvDetalles.adapter = adapter

        // Swipe to delete (HU 3.4)
        val swipeCallback = DetallePedidoAdapter.SwipeToDeleteCallback(adapter) { detalle ->
            eliminarConUndo(detalle)
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(rvDetalles)
    }

    private fun setupObservers() {
        viewModel.detalles.observe(this) { detalles ->
            adapter.submitList(detalles)

            val estaVacio = detalles.isEmpty()
            tvVacio.visibility     = if (estaVacio) View.VISIBLE else View.GONE
            rvDetalles.visibility  = if (estaVacio) View.GONE    else View.VISIBLE
            btnConfirmar.isEnabled = !estaVacio

            val total = detalles.sumOf { it.subtotal }
            tvTotal.text = "Total: ${moneyFormat.format(total)}"
        }

        viewModel.uiState.observe(this) { state ->
            when (state) {
                is PedidoViewModel.UiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnConfirmar.isEnabled = false
                }
                is PedidoViewModel.UiState.Idle -> {
                    progressBar.visibility = View.GONE
                    btnConfirmar.isEnabled = viewModel.detalles.value?.isNotEmpty() == true
                }
                is PedidoViewModel.UiState.PedidoEnviado -> {
                    progressBar.visibility = View.GONE
                    setResult(Activity.RESULT_OK)
                    mostrarSnackbar("✅ Pedido enviado a cocina", isError = false)
                    rvDetalles.postDelayed({ finish() }, 1500)
                }
                is PedidoViewModel.UiState.Success -> {
                    progressBar.visibility = View.GONE
                    mostrarSnackbar(state.mensaje, isError = false)
                    viewModel.resetUiState()
                }
                is PedidoViewModel.UiState.Error -> {
                    progressBar.visibility = View.GONE
                    btnConfirmar.isEnabled = true
                    mostrarSnackbar(state.mensaje, isError = true)
                    viewModel.resetUiState()
                }
            }
        }
    }

    private fun setupListeners() {
        btnAgregarProducto.setOnClickListener { abrirSeleccionProductos() }
        btnConfirmar.setOnClickListener { confirmarPedido() }
    }

    private fun abrirSeleccionProductos() {
        val intent = SeleccionProductoActivity.newIntent(
            context = this,
            mesaId  = mesasIds.first()
        )
        seleccionProductoLauncher.launch(intent)
    }

    private fun confirmarPedido() {
        val cantidad = viewModel.detalles.value?.size ?: 0
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar pedido")
            .setMessage("¿Enviar $cantidad producto(s) a cocina?")
            .setPositiveButton("Enviar") { _, _ ->
                viewModel.confirmarPedido()
            }
            .setNegativeButton("Revisar", null)
            .show()
    }

    private fun eliminarConUndo(detalle: DetallePedido) {
        ultimoEliminado = detalle
        viewModel.eliminarDetalle(detalle)

        Snackbar.make(
            rvDetalles,
            "${detalle.nombreProducto} eliminado",
            Snackbar.LENGTH_LONG
        ).setAction("Deshacer") {
            ultimoEliminado?.let { d ->
                viewModel.agregarProducto(
                    mesasIds       = mesasIds,
                    productoId     = d.productoId,
                    nombreProducto = d.nombreProducto,
                    precioUnitario = d.precioUnitario,
                    cantidad       = d.cantidad,
                    nota           = d.nota
                )
            }
        }.show()
    }

    private fun mostrarDialogoNota(detalle: DetallePedido) {
        val view   = layoutInflater.inflate(R.layout.dialog_nota_producto, null)
        val etNota = view.findViewById<TextInputEditText>(R.id.et_nota)
        etNota.setText(detalle.nota)
        etNota.filters = arrayOf(android.text.InputFilter.LengthFilter(100))

        MaterialAlertDialogBuilder(this)
            .setTitle("Nota para ${detalle.nombreProducto}")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevaNota = etNota.text?.toString()?.trim() ?: ""
                viewModel.actualizarNota(detalle, nuevaNota)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    private fun mostrarSnackbar(mensaje: String, isError: Boolean) {
        val rootView = findViewById<View>(android.R.id.content)
        val snack = Snackbar.make(rootView, mensaje, Snackbar.LENGTH_LONG)
        if (isError) snack.setBackgroundTint(getColor(R.color.error_color))
        snack.show()
    }
}