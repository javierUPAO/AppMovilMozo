package com.donabere.amm.ui

import android.content.ClipDescription
import android.os.Bundle
import android.util.Log
import android.view.DragEvent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.donabere.amm.databinding.ActivityMesasBinding
import com.donabere.amm.model.Mesa
import com.donabere.amm.model.enums.EstadoMesa
import com.donabere.amm.ui.adapter.ItemMesaAdapter
import com.donabere.amm.viewmodel.MesasViewModel
import com.google.firebase.firestore.FirebaseFirestore

class MesasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMesasBinding
    private val viewModel: MesasViewModel by viewModels()
    private lateinit var adapter: ItemMesaAdapter

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMesasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs      = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val usuarioId  = prefs.getString("usuarioId", "") ?: ""
        val mozoIdGuardado = prefs.getString("mozoId", "") ?: ""

        if (mozoIdGuardado.isNotEmpty()) {
            viewModel.mozoIdRecuperado = mozoIdGuardado
            Log.d("MesasActivity", "mozoId desde prefs: $mozoIdGuardado")
        } else if (usuarioId.isNotEmpty()) {
            viewModel.cargarMozoId(usuarioId)
        }

        setupRecyclerView()
        observeViewModel()
        viewModel.fetchMesas()
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchMesas()
    }

    private fun setupRecyclerView() {
        adapter = ItemMesaAdapter(
            onMesaClick = onMesaClick@{ mesa ->
                when (mesa.estado) {
                    EstadoMesa.LIBRE -> {
                        val mozoId = viewModel.mozoIdRecuperado
                        if (mozoId.isEmpty()) {
                            Toast.makeText(
                                this,
                                "Cargando datos del mozo, intenta de nuevo.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@onMesaClick
                        }
                        val mesasParaPedido = if (mesa.grupoId != null) mesa.mesasAgrupadas else listOf(mesa.id)
                        startActivity(
                            CrearPedidoActivity.newIntent(this, mesasParaPedido, mozoId)
                        )
                    }

                    EstadoMesa.OCUPADA -> {
                        Log.d("MesasActivity", "Buscando pedido para mesa: ${mesa.id}")

                        db.collection("pedidos")
                            .whereArrayContains("mesasIds", mesa.id)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val estadosActivos = setOf(
                                    "COMANDADO", "COCINA",
                                    "PENDIENTE_PREPARACION", "PENDIENTE_CORRECCION_STOCK",
                                    "LISTO_PARA_ENTREGAR", "ATENDIDO"
                                )
                                val doc = snapshot.documents.firstOrNull {
                                    it.getString("estado") in estadosActivos
                                }

                                if (doc != null) {
                                    Log.d("MesasActivity", "Pedido encontrado: ${doc.id}")
                                    abrirDetalle(doc.id, mesa.id)
                                } else {
                                    buscarPedidoPorStringLegacy(mesa.id)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("MesasActivity", "Error query: ${e.message}")
                                buscarPedidoPorStringLegacy(mesa.id)
                            }
                    }

                    else -> { /* otros estados futuros */ }
                }
            },
            onMesaDropped = { source, target ->
                handleMesaDropped(source, target)
            }
        )

        binding.rvMesas.layoutManager = GridLayoutManager(this, 2)
        binding.rvMesas.adapter = adapter

        // Listener en la lista para detectar cuando se suelta una mesa en el fondo (separar)
        binding.rvMesas.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                }
                DragEvent.ACTION_DROP -> {
                    val sourceMesa = event.localState as? Mesa
                    if (sourceMesa != null && sourceMesa.grupoId != null) {
                        handleMesaSeparation(sourceMesa)
                    }
                    true
                }
                else -> true
            }
        }
    }

    private fun handleMesaDropped(source: Mesa, target: Mesa) {
        if (source.estado == EstadoMesa.LIBRE && target.estado == EstadoMesa.LIBRE) {
            mostrarConfirmacionAgrupacion(source, target)
        } else if (source.estado == EstadoMesa.LIBRE && target.estado == EstadoMesa.OCUPADA) {
            mostrarConfirmacionUnion(source, target)
        } else if (source.estado == EstadoMesa.OCUPADA && target.estado == EstadoMesa.LIBRE) {
            mostrarConfirmacionUnion(target, source)
        } else {
            Toast.makeText(
                this,
                "No se pueden fusionar dos pedidos activos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mostrarConfirmacionAgrupacion(mesa1: Mesa, mesa2: Mesa) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Agrupar Mesas")
            .setMessage("¿Desea agrupar la Mesa ${mesa1.id.replace("m", "")} y la Mesa ${mesa2.id.replace("m", "")}?")
            .setPositiveButton("Confirmar") { _, _ ->
                viewModel.agruparMesasLibres(mesa1, mesa2)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarConfirmacionUnion(mesaLibre: Mesa, mesaOcupada: Mesa) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Asociar Mesa")
            .setMessage("¿Desea añadir la Mesa ${mesaLibre.id.replace("m", "")} al pedido existente de la Mesa ${mesaOcupada.id.replace("m", "")}?")
            .setPositiveButton("Confirmar") { _, _ ->
                viewModel.agregarMesaAPedido(mesaLibre, mesaOcupada)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun handleMesaSeparation(mesa: Mesa) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Separar Mesa")
            .setMessage("¿Desea separar la Mesa ${mesa.id.replace("m", "")} del grupo actual?")
            .setPositiveButton("Separar") { _, _ ->
                viewModel.separarMesaDeGrupo(mesa)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun buscarPedidoPorStringLegacy(mesaId: String) {
        val estadosActivos = setOf(
            "COMANDADO", "COCINA",
            "PENDIENTE_PREPARACION", "PENDIENTE_CORRECCION_STOCK",
            "LISTO_PARA_ENTREGAR", "ATENDIDO"
        )
        db.collection("pedidos")
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull { doc ->
                    val mesasStr = doc.getString("mesasIds") ?: ""
                    val estado   = doc.getString("estado") ?: ""
                    mesaId in mesasStr.split(",").map { it.trim() } && estado in estadosActivos
                }

                if (doc != null) {
                    Log.d("MesasActivity", "Pedido encontrado (legacy): ${doc.id}")
                    abrirDetalle(doc.id, mesaId)
                } else {
                    Toast.makeText(
                        this,
                        "No se encontró un pedido activo para esta mesa.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("MesasActivity", "Error legacy: ${e.message}")
                Toast.makeText(this, "Error al buscar pedido: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun abrirDetalle(pedidoId: String, mesaId: String) {
        startActivity(
            DetallePedidoActivity.newIntent(
                context  = this,
                pedidoId = pedidoId,
                mesaId   = mesaId
            )
        )
    }

    private fun observeViewModel() {
        viewModel.mesas.observe(this) { mesas ->
            adapter.submitList(mesas)
        }
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = error
            } else {
                binding.tvError.visibility = View.GONE
            }
        }
        viewModel.mozoIdLiveData.observe(this) { mozoId ->
            if (mozoId.isNotEmpty()) {
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("mozoId", mozoId)
                    .apply()
                Log.d("MesasActivity", "mozoId guardado en prefs: $mozoId")
            }
        }
    }
}