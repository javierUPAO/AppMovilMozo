package com.donabere.amm.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.R
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.repository.PedidoRepository
import com.donabere.amm.ui.adapter.DetallePedidoAdapter
import com.donabere.amm.ui.adapter.PaginaMenuAdapter
import com.donabere.amm.viewmodel.PedidoViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText

class CrearPedidoActivity : AppCompatActivity() {

    // ─── Companion ────────────────────────────────────────────────────────────
    companion object {
        const val EXTRA_MESAS_IDS = "extra_mesas_ids"
        const val EXTRA_MOZO_ID   = "extra_mozo_id"

        fun newIntent(
            context: Context,
            mesasIds: List<String>,
            mozoId: String
        ): Intent = Intent(context, CrearPedidoActivity::class.java).apply {
            putStringArrayListExtra(EXTRA_MESAS_IDS, ArrayList(mesasIds))
            putExtra(EXTRA_MOZO_ID, mozoId)
        }
    }

    // ─── ViewModel ────────────────────────────────────────────────────────────
    private val viewModel: PedidoViewModel by viewModels {
        PedidoViewModel.Factory(PedidoRepository(), mozoId)
    }

    // Carrito (BottomSheet)
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var rvCarrito: RecyclerView
    private lateinit var tvTotal: TextView
    private lateinit var tvMesas: TextView
    private lateinit var tvCarritoVacio: TextView
    private lateinit var tvContadorCarrito: TextView
    private lateinit var btnEnviarCocina: View
    private lateinit var progressBar: ProgressBar

    private lateinit var carritoAdapter: DetallePedidoAdapter

    // ─── Datos ────────────────────────────────────────────────────────────────
    private lateinit var mesasIds: List<String>
    private var mozoId: String = ""
    private var ultimoEliminado: DetallePedido? = null

    // ─── Lifecycle ────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_pedido)

        mesasIds = intent.getStringArrayListExtra(EXTRA_MESAS_IDS) ?: arrayListOf()
        mozoId   = intent.getStringExtra(EXTRA_MOZO_ID) ?: ""

        initViews()
        setupMenu()
        setupCarrito()
        setupObservers()

        // Iniciar el borrador del pedido
        viewModel.iniciarPedido(mesasIds)
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    private fun initViews() {
        tvMesas            = findViewById(R.id.tv_mesas_label)
        tvTotal            = findViewById(R.id.tv_total_pedido)
        tvCarritoVacio     = findViewById(R.id.tv_carrito_vacio)
        tvContadorCarrito  = findViewById(R.id.tv_contador_carrito)
        rvCarrito          = findViewById(R.id.rv_carrito)
        btnEnviarCocina    = findViewById(R.id.btn_enviar_cocina)
        progressBar        = findViewById(R.id.progress_bar)

        tvMesas.text = if (mesasIds.size == 1)
            "Mesa ${mesasIds.first()}"
        else
            "Mesas ${mesasIds.joinToString(" + ")}"
    }

    private fun setupMenu() {
        val viewPager  = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.vp_menu)
        val tabLayout  = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tab_categorias)


        val adapter = PaginaMenuAdapter(this) { productoId, nombre, precio ->

            agregarAlCarrito(productoId, nombre, precio)
        }
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = listOf("Platos", "Bebidas")[pos]
        }.attach()
    }

    private fun setupCarrito() {
        val bottomSheet = findViewById<View>(R.id.bottom_sheet_carrito)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = resources.getDimensionPixelSize(R.dimen.carrito_peek_height) // 72dp
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        carritoAdapter = DetallePedidoAdapter(
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

        rvCarrito.layoutManager = LinearLayoutManager(this)
        rvCarrito.adapter = carritoAdapter

        // Swipe para eliminar (HU 3.2)
        ItemTouchHelper(
            DetallePedidoAdapter.SwipeToDeleteCallback(carritoAdapter) { detalle ->
                eliminarConUndo(detalle)
            }
        ).attachToRecyclerView(rvCarrito)

        btnEnviarCocina.setOnClickListener { confirmarPedido() }
    }

    // ─── Observers ────────────────────────────────────────────────────────────

    private fun setupObservers() {
        viewModel.detalles.observe(this) { detalles ->
            carritoAdapter.submitList(detalles)

            val vacio = detalles.isEmpty()
            tvCarritoVacio.visibility = if (vacio) View.VISIBLE else View.GONE
            rvCarrito.visibility      = if (vacio) View.GONE    else View.VISIBLE
            btnEnviarCocina.isEnabled = !vacio

            // Contador de ítems en la pestaña del carrito (badge)
            val totalItems = detalles.sumOf { it.cantidad }
            tvContadorCarrito.text = if (totalItems > 0) "🛒 $totalItems" else "🛒"

            // Total
            val total = detalles.sumOf { it.subtotal }
            tvTotal.text = "Total: S/ %.2f".format(total)

            // Expandir carrito automáticamente al agregar el primer producto
            if (detalles.size == 1 && bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            }
        }

        viewModel.uiState.observe(this) { state ->
            when (state) {
                is PedidoViewModel.UiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnEnviarCocina.isEnabled = false
                }
                is PedidoViewModel.UiState.Idle -> {
                    progressBar.visibility = View.GONE
                    btnEnviarCocina.isEnabled =
                        viewModel.detalles.value?.isNotEmpty() == true
                }
                is PedidoViewModel.UiState.PedidoEnviado -> {
                    progressBar.visibility = View.GONE
                    mostrarSnackbar("✅ Pedido enviado a cocina")
                    setResult(Activity.RESULT_OK)
                    rvCarrito.postDelayed({ finish() }, 1500)
                }
                is PedidoViewModel.UiState.Success -> {
                    progressBar.visibility = View.GONE
                    mostrarSnackbar(state.mensaje)
                    viewModel.resetUiState()
                }
                is PedidoViewModel.UiState.Error -> {
                    progressBar.visibility = View.GONE
                    btnEnviarCocina.isEnabled =
                        viewModel.detalles.value?.isNotEmpty() == true
                    mostrarSnackbar("⚠️ ${state.mensaje}", isError = true)
                    viewModel.resetUiState()
                }
            }
        }
    }

    // ─── Acciones ─────────────────────────────────────────────────────────────

    /**
     * Llamado desde PaginaMenuAdapter cuando el usuario toca un producto.
     * Si necesita nota, primero muestra el diálogo; si no, agrega directo.
     */
    fun agregarAlCarrito(
        productoId: String,
        nombre: String,
        precio: Double,
        cantidad: Int = 1,
        nota: String = ""
    ) {
        viewModel.agregarProducto(
            productoId     = productoId,
            nombreProducto = nombre,
            precioUnitario = precio,
            cantidad       = cantidad,
            nota           = nota
        )
    }

    private fun eliminarConUndo(detalle: DetallePedido) {
        ultimoEliminado = detalle
        viewModel.eliminarDetalle(detalle)

        Snackbar.make(
            findViewById(android.R.id.content),
            "${detalle.nombreProducto} eliminado",
            Snackbar.LENGTH_LONG
        ).setAction("Deshacer") {
            ultimoEliminado?.let { viewModel.restaurarDetalle(it) }
        }.show()
    }

    private fun confirmarPedido() {
        val cantidad = viewModel.detalles.value?.size ?: 0
        MaterialAlertDialogBuilder(this)
            .setTitle("Enviar a cocina")
            .setMessage("¿Confirmar pedido con $cantidad producto(s)?")
            .setPositiveButton("Enviar") { _, _ -> viewModel.confirmarPedido() }
            .setNegativeButton("Revisar", null)
            .show()
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
                viewModel.actualizarNota(detalle, etNota.text?.toString()?.trim() ?: "")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarSnackbar(mensaje: String, isError: Boolean = false) {
        val snack = Snackbar.make(
            findViewById(android.R.id.content),
            mensaje,
            Snackbar.LENGTH_LONG
        )
        if (isError) snack.setBackgroundTint(getColor(R.color.error_color))
        snack.show()
    }
}