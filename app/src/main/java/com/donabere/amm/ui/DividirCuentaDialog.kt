package com.donabere.amm.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView
import com.donabere.amm.model.DetallePedido
import com.donabere.amm.ui.adapter.ItemDivisibleAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout


/**
 * Dialog de división de cuenta.
 *
 * Uso desde DetallePedidoActivity:
 *   DividirCuentaDialog.show(
 *       supportFragmentManager,
 *       pedidoId = "abc123",
 *       detalles = listOf(...)
 *   ) { cuentasGuardadas ->
 *       // recargar UI
 *   }
 */
//DEPRECADO
class DividirCuentaDialog : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "DividirCuentaDialog"
        private const val ARG_PEDIDO_ID = "pedido_id"

        // Los detalles se pasan via static para evitar límites de Bundle
        private var detallesTmp: List<DetallePedido> = emptyList()
        private var onGuardadoTmp: (() -> Unit)? = null

        fun show(
            fm: androidx.fragment.app.FragmentManager,
            pedidoId: String,
            detalles: List<DetallePedido>,
            onGuardado: () -> Unit
        ) {
            detallesTmp   = detalles
            onGuardadoTmp = onGuardado
            val dialog = DividirCuentaDialog().apply {
                arguments = Bundle().also { it.putString(ARG_PEDIDO_ID, pedidoId) }
            }
            dialog.show(fm, TAG)
        }
    }

    private val pedidoId: String by lazy {
        requireArguments().getString(ARG_PEDIDO_ID, "")
    }


    // ── Views ─────────────────────────────────────────────────────────────────
    private lateinit var tabLayout: TabLayout
    private lateinit var rvItems: RecyclerView
    private lateinit var llResumen: LinearLayout
    private lateinit var llPersonas: LinearLayout
    private lateinit var btnAgregarPersona: MaterialButton
    private lateinit var btnCancelar: Button
    private lateinit var btnGuardar: MaterialButton
    private lateinit var tvSinAsignar: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var itemsAdapter: ItemDivisibleAdapter

    // ── Lifecycle ─────────────────────────────────────────────────────────────
   /* override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.old_dialog_dividir_cuenta, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupTabs()
        setupRecyclerView()
        setupPersonas()
        setupObservers()
        setupListeners()
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun initViews(view: View) {
        tabLayout        = view.findViewById(R.id.tab_layout_division)
        rvItems          = view.findViewById(R.id.rv_items_division)
        llResumen        = view.findViewById(R.id.ll_resumen)
        llPersonas       = view.findViewById(R.id.ll_personas)
        btnAgregarPersona = view.findViewById(R.id.btn_agregar_persona)
        btnCancelar      = view.findViewById(R.id.btn_cancelar_division)
        btnGuardar       = view.findViewById(R.id.btn_guardar_division)
        tvSinAsignar     = view.findViewById(R.id.tv_sin_asignar)
        progressBar      = view.findViewById(R.id.pb_division)
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Asignar ítems"))
        tabLayout.addTab(tabLayout.newTab().setText("Resumen"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> { rvItems.visibility = View.VISIBLE; llResumen.visibility = View.GONE }
                    1 -> { rvItems.visibility = View.GONE;    llResumen.visibility = View.VISIBLE
                        actualizarResumen() }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        itemsAdapter = ItemDivisibleAdapter { detalleId, unidad, personaIndex ->
            viewModel.asignarItem(detalleId, unidad, personaIndex)
        }
        rvItems.layoutManager = LinearLayoutManager(requireContext())
        rvItems.adapter = itemsAdapter
    }

    /** Construye la lista de inputs de personas dinámicamente */
    private fun setupPersonas() {
        viewModel.personas.observe(viewLifecycleOwner) { personas ->
            llPersonas.removeAllViews()
            personas.forEach { persona ->
                llPersonas.addView(crearFilaPersona(persona))
            }
            // También actualizar el adapter de ítems con los nombres nuevos
            itemsAdapter.actualizarPersonas(personas)
        }
    }

    private fun setupObservers() {
        viewModel.items.observe(viewLifecycleOwner) { items ->
            itemsAdapter.submitList(items.toList())

            // Badge de ítems sin asignar
            val sinAsignar = items.count { it.cuentaPersonaIndex == null }
            tvSinAsignar.visibility = if (sinAsignar > 0) View.VISIBLE else View.GONE
            tvSinAsignar.text = "$sinAsignar ítem(s) sin asignar"

            // Si estamos en tab Resumen, actualizar
            if (tabLayout.selectedTabPosition == 1) actualizarResumen()
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DividirCuentaViewModel.UiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnGuardar.isEnabled   = false
                }
                is DividirCuentaViewModel.UiState.Guardado -> {
                    progressBar.visibility = View.GONE
                    onGuardadoTmp?.invoke()
                    dismiss()
                }
                is DividirCuentaViewModel.UiState.Error -> {
                    progressBar.visibility = View.GONE
                    btnGuardar.isEnabled   = true
                    tvSinAsignar.visibility = View.VISIBLE
                    tvSinAsignar.text = state.msg
                    viewModel.resetUiState()
                }
                else -> {
                    progressBar.visibility = View.GONE
                    btnGuardar.isEnabled   = true
                }
            }
        }
    }

    private fun setupListeners() {
        btnAgregarPersona.setOnClickListener { viewModel.agregarPersona() }
        btnCancelar.setOnClickListener { dismiss() }
        btnGuardar.setOnClickListener { viewModel.guardarDivision() }
    }

    // ── Fila de persona (input + botón eliminar) ──────────────────────────────

    private fun crearFilaPersona(persona: PersonaCuenta): View {
        val view = layoutInflater.inflate(R.layout.item_persona_division, llPersonas, false)

        val tvNumero = view.findViewById<TextView>(R.id.tv_numero_persona)
        val etNombre = view.findViewById<TextInputEditText>(R.id.et_nombre_persona)
        val btnEliminar = view.findViewById<ImageButton>(R.id.btn_eliminar_persona)

        tvNumero.text = (persona.index + 1).toString()
        etNombre.setText(persona.nombre)

        etNombre.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.renombrarPersona(persona.index, s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        btnEliminar.setOnClickListener {
            viewModel.eliminarPersona(persona.index)
        }

        return view
    }

    // ── Tab Resumen ───────────────────────────────────────────────────────────

    private fun actualizarResumen() {
        llResumen.removeAllViews()
        val personas = viewModel.personas.value ?: return

        personas.forEach { persona ->
            val total = viewModel.totalPersona(persona.index)
            val itemsPersona = viewModel.items.value
                ?.filter { it.cuentaPersonaIndex == persona.index }
                ?: emptyList()

            // Card de resumen por persona
            val card = layoutInflater.inflate(R.layout.item_resumen_persona, llResumen, false)
            card.findViewById<TextView>(R.id.tv_nombre_resumen).text = persona.nombre
            card.findViewById<TextView>(R.id.tv_total_resumen).text =
                "S/. %.2f".format(total)

            val tvItems = card.findViewById<TextView>(R.id.tv_items_resumen)
            tvItems.text = itemsPersona.joinToString("\n") {
                "• ${it.displayNombre} — S/. %.2f".format(it.precio)
            }.ifBlank { "Sin ítems asignados" }

            llResumen.addView(card)
        }
    }*/
}