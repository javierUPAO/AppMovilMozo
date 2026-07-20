package com.donabere.amm.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.R
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.model.enums.EstadoPedido
import com.donabere.amm.repository.PedidoRepository
import com.donabere.amm.ui.adapter.DetallePedidoReadOnlyAdapter
import com.donabere.amm.ui.fragment.DialogAnulacionFragment
import com.donabere.amm.ui.fragment.SeleccionarMozoDialog
import com.donabere.amm.viewmodel.PedidoViewModel
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

    // ── Firebase ──────────────────────────────────────────────────────────────
    private val db         = FirebaseFirestore.getInstance()
    private val pedidosRef = db.collection("pedidos")
    private val mesasRef   = db.collection("mesas")

    // ── ViewModel ─────────────────────────────────────────────────────────────
    private lateinit var viewModel: PedidoViewModel

    // ── Vistas ────────────────────────────────────────────────────────────────
    private lateinit var tvMesa:             TextView
    private lateinit var tvTotal:            TextView
    private lateinit var tvVacio:            TextView
    private lateinit var chipEstado:         Chip
    private lateinit var rvDetalles:         RecyclerView
    private lateinit var btnCobrar:          MaterialButton
    private lateinit var btnDividir:         MaterialButton
    private lateinit var btnTransferir:      MaterialButton
    private lateinit var btnTransferirMozo:  MaterialButton
    private lateinit var btnAgregarPlato:    MaterialButton
    private lateinit var progressBar:        ProgressBar
    private lateinit var llCuentasDivididas: View
    private lateinit var chipGroupCuentas:   ChipGroup

    // ── Estado ────────────────────────────────────────────────────────────────
    private lateinit var pedidoId:       String
    private lateinit var mesaId:         String
    private var cuentaIdPrincipal:       String = ""   // ID de la cuenta activa del pedido
    private var detallesActuales:        List<DetallePedido> = emptyList()
    private var estadoPedido:            EstadoPedido = EstadoPedido.PENDIENTE_PREPARACION
    private var mozoIdActual:            String? = null
    private val modoEdicion get() =
        estadoPedido == EstadoPedido.COMANDADO ||
                estadoPedido == EstadoPedido.PENDIENTE_PREPARACION

    private val moneyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"))

    // ── ActivityResult para SeleccionProductoActivity ─────────────────────────
    private val seleccionProductoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val (productoId, nombre, precio) = SeleccionProductoActivity
            .extraerResultado(result.data) ?: return@registerForActivityResult

        viewModel.agregarProductoAPedidoActivo(
            pedidoId       = pedidoId,
            cuentaId       = cuentaIdPrincipal,
            productoId     = productoId,
            nombreProducto = nombre,
            precioUnitario = precio
        )
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_pedido)

        pedidoId = intent.getStringExtra(EXTRA_PEDIDO_ID) ?: run { finish(); return }
        mesaId   = intent.getStringExtra(EXTRA_MESA_ID)   ?: run { finish(); return }

        iniciarViewModel()
        bindViews()
        configurarRecyclerView()
        observarViewModel()
        registrarListenerAnulacion()

        tvMesa.text = "Mesa $mesaId"

        cargarDetalles()

        btnDividir.setOnClickListener    { abrirDividirCuenta() }

        btnCobrar.setOnClickListener     { confirmarCobro() }

        btnTransferir.setOnClickListener { abrirTransferirMesa() }
        btnTransferirMozo.setOnClickListener { abrirTransferirMozo() }
        btnAgregarPlato.setOnClickListener {
            seleccionProductoLauncher.launch(
                SeleccionProductoActivity.newIntent(this, mesaId)
            )
        }
    }

    // ── ViewModel ─────────────────────────────────────────────────────────────

    private fun iniciarViewModel() {
        val repository = PedidoRepository()
        viewModel = ViewModelProvider(
            this,
            PedidoViewModel.Factory(repository)
        )[PedidoViewModel::class.java]
    }

    private fun observarViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is PedidoViewModel.UiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                }
                is PedidoViewModel.UiState.Success -> {
                    progressBar.visibility = View.GONE
                    mostrarSnackbar(state.mensaje)
                    cargarDetalles()          // Recargar lista tras cada cambio
                    viewModel.resetUiState()
                }
                is PedidoViewModel.UiState.Error -> {
                    progressBar.visibility = View.GONE
                    mostrarSnackbar("❌ ${state.mensaje}")
                    viewModel.resetUiState()
                }
                else -> {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    private fun bindViews() {
        tvMesa             = findViewById(R.id.tv_mesa_label)
        tvTotal            = findViewById(R.id.tv_total)
        tvVacio            = findViewById(R.id.tv_vacio)
        chipEstado         = findViewById(R.id.chip_estado)
        rvDetalles         = findViewById(R.id.rv_detalles)
        btnCobrar          = findViewById(R.id.btn_cobrar)
        btnDividir         = findViewById(R.id.btn_dividir)
        btnTransferir      = findViewById(R.id.btn_transferir)
        btnTransferirMozo  = findViewById(R.id.btn_transferir_mozo)
        btnAgregarPlato    = findViewById(R.id.btn_agregar_plato)
        progressBar        = findViewById(R.id.progress_bar)
        llCuentasDivididas = findViewById(R.id.ll_cuentas_divididas)
        chipGroupCuentas   = findViewById(R.id.chip_group_cuentas)
    }

    private fun configurarRecyclerView() {
        rvDetalles.layoutManager = LinearLayoutManager(this)
    }

    // ── Cargar detalles ───────────────────────────────────────────────────────

    private fun cargarDetalles() {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Leer estado del pedido
                val pedidoSnap = pedidosRef.document(pedidoId).get().await()
                val estadoStr  = pedidoSnap.getString("estado") ?: ""
                val mozoIdStr  = pedidoSnap.getString("mozoId")
                val estado     = try {
                    EstadoPedido.valueOf(estadoStr)
                } catch (e: Exception) {
                    EstadoPedido.PENDIENTE_PREPARACION
                }

                // 2. Leer cuentas y detalles
                val cuentasSnap = pedidosRef
                    .document(pedidoId)
                    .collection("cuentas")
                    .get()
                    .await()

                // Guardar el ID de la primera cuenta (cuenta principal)
                val primeraCuenta = cuentasSnap.documents.firstOrNull()
                val cuentaId      = primeraCuenta?.id ?: ""

                val todosLosDetalles = mutableListOf<DetallePedido>()

                for (cuentaDoc in cuentasSnap.documents) {
                    val detallesSnap = cuentaDoc.reference
                        .collection("detalles")
                        .get()
                        .await()

                    val detallesCuenta = detallesSnap.documents.mapNotNull { doc ->
                        try {
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
                        } catch (e: Exception) { null }
                    }.filter { !it.anulado }

                    todosLosDetalles.addAll(detallesCuenta)
                }

                val total = todosLosDetalles.sumOf { it.subtotal }

                val cuentas = cuentasSnap.documents.map { doc ->
                    Pair(
                        doc.getString("nombre")      ?: "Cuenta",
                        doc.getDouble("totalCuenta") ?: 0.0
                    )
                }

                // Obtener ultimaAtencion de la mesa para el tiempo de espera
                val mesaSnap = mesasRef.document(mesaId).get().await()
                val ultimaAtencion = mesaSnap.getTimestamp("ultimaAtencion")

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    estadoPedido           = estado
                    cuentaIdPrincipal      = cuentaId
                    detallesActuales       = todosLosDetalles
                    mozoIdActual           = mozoIdStr

                    // Validar que solo el dueño del pedido pueda transferirlo
                    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val mozoLogueado = prefs.getString("mozoId", "")?.trim() ?: ""
                    
                    android.util.Log.d("DEBUG_TRANSFER", "mozoLogueado: '$mozoLogueado', mozoIdActual: '$mozoIdActual'")
                    
                    val esPropietario = mozoLogueado.isEmpty() || mozoLogueado == mozoIdActual
                    
                    if (!esPropietario) {
                        btnTransferirMozo.visibility = View.GONE
                        btnTransferir.visibility     = View.GONE
                        btnCobrar.visibility         = View.GONE
                        btnDividir.visibility        = View.GONE
                    } else {
                        btnTransferirMozo.visibility = View.VISIBLE
                        btnTransferir.visibility     = View.VISIBLE
                        btnCobrar.visibility         = View.VISIBLE
                        btnDividir.visibility        = View.GONE
                    }

                    // Calcular y mostrar tiempo transcurrido si corresponde
                    if (ultimaAtencion != null && estado != EstadoPedido.PAGADO) {
                        val now = com.google.firebase.Timestamp.now().seconds
                        val diffSec = now - ultimaAtencion.seconds
                        val diffMin = (diffSec / 60).toInt()
                        tvMesa.text = "Mesa $mesaId | Espera: $diffMin min"
                    } else {
                        tvMesa.text = "Mesa $mesaId"
                    }

                    actualizarChipEstado(estado)
                    actualizarModoUI(todosLosDetalles, total)

                    if (cuentas.isNotEmpty()) {
                        mostrarCuentasDivididas(cuentas)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    mostrarSnackbar("Error al cargar pedido: ${e.message}")
                }
            }
        }
    }

    // ── UI según estado ───────────────────────────────────────────────────────

    private fun actualizarChipEstado(estado: EstadoPedido) {
        chipEstado.text = when (estado) {
            EstadoPedido.COMANDADO              -> "Tomado"
            EstadoPedido.PENDIENTE_PREPARACION  -> "Pendiente de preparación"
            EstadoPedido.COCINA                 -> "En cocina"
            EstadoPedido.LISTO_PARA_ENTREGAR    -> "Listo para entregar"
            EstadoPedido.ATENDIDO               -> "Atendido"
            EstadoPedido.PAGADO                 -> "Pagado"
            else                                -> estado.name
        }
    }

    private fun actualizarModoUI(
        detalles: List<DetallePedido>,
        total: Double
    ) {
        if (detalles.isEmpty()) {
            tvVacio.visibility    = View.VISIBLE
            rvDetalles.visibility = View.GONE
            btnCobrar.visibility =  View.VISIBLE
            btnDividir.isEnabled  = false
            btnDividir.visibility = View.GONE
            btnCobrar.isEnabled   = true
        } else {
            btnCobrar.visibility =  View.GONE
            btnDividir.visibility = View.GONE
            tvVacio.visibility    = View.GONE
            rvDetalles.visibility = View.VISIBLE
            tvTotal.text          = "Total: ${moneyFormat.format(total)}"

            if (modoEdicion) {
                // Adapter editable con botón anular por ítem
                rvDetalles.adapter = DetalleEditableAdapter(
                    detalles   = detalles,
                    onIncrement = { detalle ->
                        viewModel.modificarCantidadEnPedidoActivo(
                            pedidoId      = pedidoId,
                            cuentaId      = detalle.cuentaId ?: cuentaIdPrincipal,
                            detalle       = detalle,
                            nuevaCantidad = detalle.cantidad + 1
                        )
                    },
                    onDecrement = { detalle ->
                        if (detalle.cantidad > 1) {
                            viewModel.modificarCantidadEnPedidoActivo(
                                pedidoId      = pedidoId,
                                cuentaId      = detalle.cuentaId ?: cuentaIdPrincipal,
                                detalle       = detalle,
                                nuevaCantidad = detalle.cantidad - 1
                            )
                        }
                        // cantidad == 1 → no hacer nada; para eliminar se usa btn_anular
                    },
                    onAnular = { detalle ->
                        DialogAnulacionFragment
                            .newInstance(detalle)
                            .show(supportFragmentManager, DialogAnulacionFragment.TAG)
                    }
                )
                btnAgregarPlato.visibility = View.VISIBLE
            } else {
                // Adapter solo lectura (comportamiento anterior)
                rvDetalles.adapter = DetallePedidoReadOnlyAdapter(
                    detalles.map { d ->
                        DetallePedidoReadOnlyAdapter.Item(
                            nombre   = d.nombreProducto,
                            cantidad = d.cantidad,
                            subtotal = d.subtotal
                        )
                    }
                )
                btnAgregarPlato.visibility = View.GONE
            }
        }
    }

    // ── Listener resultado anulación ──────────────────────────────────────────

    private fun registrarListenerAnulacion() {
        supportFragmentManager.setFragmentResultListener(
            DialogAnulacionFragment.REQUEST_KEY,
            this
        ) { _, bundle ->
            val motivo    = bundle.getString(DialogAnulacionFragment.RESULT_MOTIVO)    ?: return@setFragmentResultListener
            val detalleId = bundle.getString(DialogAnulacionFragment.RESULT_DETALLE_ID) ?: return@setFragmentResultListener
            val cuentaId  = bundle.getString(DialogAnulacionFragment.RESULT_CUENTA_ID)  ?: return@setFragmentResultListener

            val detalle = detallesActuales.find { it.id == detalleId } ?: return@setFragmentResultListener

            viewModel.anularProductoEnPedidoActivo(
                pedidoId = pedidoId,
                cuentaId = cuentaId,
                detalle  = detalle,
                motivo   = motivo
            )
        }
    }

    // ── Adapter interno editable ──────────────────────────────────────────────

    private inner class DetalleEditableAdapter(
        private val detalles: List<DetallePedido>,
        private val onIncrement: (DetallePedido) -> Unit,
        private val onDecrement: (DetallePedido) -> Unit,
        private val onAnular:    (DetallePedido) -> Unit
    ) : RecyclerView.Adapter<DetalleEditableAdapter.VH>() {

        private val moneyFmt = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"))

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = layoutInflater.inflate(
                R.layout.item_detalle_pedido_editable, parent, false
            )
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) =
            holder.bind(detalles[position])

        override fun getItemCount() = detalles.size

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvNombre   = v.findViewById<TextView>(R.id.tv_nombre_producto)
            private val tvNota     = v.findViewById<TextView>(R.id.tv_nota)
            private val tvPrecio   = v.findViewById<TextView>(R.id.tv_precio_unitario)
            private val tvCantidad = v.findViewById<TextView>(R.id.tv_cantidad)
            private val tvSubtotal = v.findViewById<TextView>(R.id.tv_subtotal)
            private val btnMas     = v.findViewById<ImageButton>(R.id.btn_mas)
            private val btnMenos   = v.findViewById<ImageButton>(R.id.btn_menos)
            private val btnAnular  = v.findViewById<ImageButton>(R.id.btn_anular)

            fun bind(d: DetallePedido) {
                tvNombre.text   = d.nombreProducto
                tvCantidad.text = d.cantidad.toString()
                tvPrecio.text   = "${moneyFmt.format(d.precioUnitario)}/u"
                tvSubtotal.text = moneyFmt.format(d.subtotal)

                if (d.nota.isBlank()) {
                    tvNota.visibility = View.GONE
                } else {
                    tvNota.visibility = View.VISIBLE
                    tvNota.text       = "📝 ${d.nota}"
                }

                btnMas.setOnClickListener    { onIncrement(d) }
                btnMenos.setOnClickListener  { onDecrement(d) }
                btnAnular.setOnClickListener { onAnular(d) }
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
                chipGroupCuentas.removeAllViews()
                cargarDetalles()
                mostrarSnackbar("División guardada correctamente")
            }
        )
    }

    // ── Chips de cuentas divididas ────────────────────────────────────────────

    private fun mostrarCuentasDivididas(cuentas: List<Pair<String, Double>>) {
        llCuentasDivididas.visibility = View.VISIBLE
        chipGroupCuentas.removeAllViews()
        cuentas.forEach { (nombre, total) ->
            val chip = Chip(this).apply {
                text        = "$nombre\n${moneyFormat.format(total)}"
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
            .setTitle("Cerrar cuenta - emergencia")
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
                pedidosRef.document(pedidoId).update("estado", "PAGADO").await()

                val pedidoSnap = pedidosRef.document(pedidoId).get().await()
                val mesasIds   = (pedidoSnap.get("mesasIds") as? List<*>)
                    ?.mapNotNull { it as? String } ?: listOf(mesaId)

                val batch = db.batch()
                mesasIds.forEach { mId ->
                    batch.update(
                        mesasRef.document(mId.trim()),
                        mapOf(
                            "estado"        to "LIBRE",
                            "pedidoId"      to null,
                            "grupoId"       to null,
                            "mesasAgrupadas" to emptyList<String>()
                        )
                    )
                }
                batch.commit().await()

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    mostrarSnackbar("✅ Mesa $mesaId liberada")
                    rvDetalles.postDelayed({ finish() }, 1000)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnCobrar.isEnabled    = true
                    btnDividir.isEnabled   = true
                    mostrarSnackbar("Error al procesar cobro: ${e.message}")
                }
            }
        }
    }

    // ── Transferencia ─────────────────────────────────────────────────────────

    private fun abrirTransferirMesa() {
        progressBar.visibility  = View.VISIBLE
        btnTransferir.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val mesasSnap  = mesasRef.whereEqualTo("estado", "LIBRE").get().await()
                val mesasLibres = mesasSnap.documents.mapNotNull { it.id }

                withContext(Dispatchers.Main) {
                    progressBar.visibility  = View.GONE
                    btnTransferir.isEnabled = true

                    if (mesasLibres.isEmpty()) {
                        mostrarSnackbar("No hay mesas libres disponibles.")
                        return@withContext
                    }

                    val items = mesasLibres.toTypedArray()
                    MaterialAlertDialogBuilder(this@DetallePedidoActivity)
                        .setTitle("Transferir a Mesa")
                        .setItems(items) { _, which ->
                            confirmarTransferencia(items[which])
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility  = View.GONE
                    btnTransferir.isEnabled = true
                    mostrarSnackbar("Error al cargar mesas libres: ${e.message}")
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
        progressBar.visibility  = View.VISIBLE
        btnTransferir.isEnabled = false
        btnCobrar.isEnabled     = false
        btnDividir.isEnabled    = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = PedidoRepository()
                val result     = repository.transferirPedido(pedidoId, mesaId, mesaDestinoId)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (result.isSuccess) {
                        mostrarSnackbar("✅ Pedido transferido a la Mesa $mesaDestinoId")
                        rvDetalles.postDelayed({ finish() }, 1000)
                    } else {
                        btnTransferir.isEnabled = true
                        btnCobrar.isEnabled     = true
                        btnDividir.isEnabled    = true
                        mostrarSnackbar("Error al transferir: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility  = View.GONE
                    btnTransferir.isEnabled = true
                    btnCobrar.isEnabled     = true
                    btnDividir.isEnabled    = true
                    mostrarSnackbar("Error inesperado: ${e.message}")
                }
            }
        }
    }

    // ── Transferencia Mozo ────────────────────────────────────────────────────

    private fun abrirTransferirMozo() {
        progressBar.visibility = View.VISIBLE
        btnTransferirMozo.isEnabled = false

        viewModel.obtenerMozosActivos { mozos ->
            progressBar.visibility = View.GONE
            btnTransferirMozo.isEnabled = true

            val mozosFiltrados = mozos.filter { it.id != mozoIdActual }

            if (mozosFiltrados.isEmpty()) {
                mostrarSnackbar("No hay otros mozos disponibles.")
                return@obtenerMozosActivos
            }

            SeleccionarMozoDialog.newInstance(mozosFiltrados) { mozoSeleccionado ->
                confirmarTransferenciaMozo(mozoSeleccionado)
            }.show(supportFragmentManager, "SeleccionarMozoDialog")
        }
    }

    private fun confirmarTransferenciaMozo(mozoDestino: com.donabere.amm.model.Mozo) {
        val totalFormateado = moneyFormat.format(detallesActuales.sumOf { it.subtotal })
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar Transferencia")
            .setMessage("¿Transferir este pedido de $totalFormateado al mozo ${mozoDestino.name}?")
            .setPositiveButton("Confirmar") { _, _ -> procesarTransferenciaMozo(mozoDestino.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun procesarTransferenciaMozo(mozoDestinoId: String) {
        progressBar.visibility = View.VISIBLE
        btnTransferirMozo.isEnabled = false
        btnTransferir.isEnabled = false
        btnCobrar.isEnabled = false
        btnDividir.isEnabled = false

        viewModel.transferirPedidoAMozo(pedidoId, mozoDestinoId) {
            progressBar.visibility = View.GONE
            mostrarSnackbar("✅ Pedido transferido correctamente")
            rvDetalles.postDelayed({ finish() }, 1000)
        }
    }

    // ── Util ──────────────────────────────────────────────────────────────────

    private fun mostrarSnackbar(msg: String) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show()
    }
}