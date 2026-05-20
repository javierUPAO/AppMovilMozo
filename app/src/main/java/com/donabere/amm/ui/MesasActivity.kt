package com.donabere.amm.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.donabere.amm.databinding.ActivityMesasBinding
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

        // Cargar mozoId desde SharedPreferences y pasarlo al ViewModel
        // (el login lo guarda ahí al autenticarse)
        val prefs      = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val usuarioId  = prefs.getString("usuarioId", "") ?: ""
        val mozoIdGuardado = prefs.getString("mozoId", "") ?: ""

        // Si ya tenemos el mozoId guardado lo usamos directo,
        // si no lo tenemos aún lo cargamos por usuarioId
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
        adapter = ItemMesaAdapter { mesa ->
            when (mesa.estado) {

                EstadoMesa.LIBRE -> {
                    // ── Bug 1 fix: leer mozoId desde el ViewModel, no desde prefs ──
                    val mozoId = viewModel.mozoIdRecuperado
                    if (mozoId.isEmpty()) {
                        Toast.makeText(
                            this,
                            "Cargando datos del mozo, intenta de nuevo.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@ItemMesaAdapter
                    }
                    startActivity(
                        CrearPedidoActivity.newIntent(this, listOf(mesa.id), mozoId)
                    )
                }

                EstadoMesa.OCUPADA -> {
                    Log.d("MesasActivity", "Buscando pedido para mesa: ${mesa.id}")

                    // ── Bug 2 fix: buscar por campo "mesasIds" que ahora es array ──
                    // Si guardaste como String "1,2" usa la 2da query de respaldo
                    db.collection("pedidos")
                        .whereArrayContains("mesasIds", mesa.id)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            // Pedido activo = cualquier estado distinto de BORRADOR o PAGADO
                            val estadosActivos = setOf(
                                "COMANDADO", "EN_PREPARACION",
                                "PENDIENTE_PREPARACION", "LISTO_PARA_ENTREGAR", "ENTREGADO"
                            )
                            val doc = snapshot.documents.firstOrNull {
                                it.getString("estado") in estadosActivos
                            }

                            if (doc != null) {
                                Log.d("MesasActivity", "Pedido encontrado: ${doc.id}")
                                abrirDetalle(doc.id, mesa.id)
                            } else {
                                // Respaldo: buscar por campo String (migración de datos viejos)
                                buscarPedidoPorStringLegacy(mesa.id)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("MesasActivity", "Error query: ${e.message}")
                            // Intentar con la query legacy antes de mostrar error
                            buscarPedidoPorStringLegacy(mesa.id)
                        }
                }

                else -> { /* otros estados futuros */ }
            }
        }

        binding.rvMesas.layoutManager = GridLayoutManager(this, 2)
        binding.rvMesas.adapter = adapter
    }

    /**
     * Respaldo para pedidos guardados con mesasIds como String "1,2"
     * (datos creados con la versión anterior del PedidoRepository).
     * Una vez migrados todos los datos se puede eliminar este método.
     */
    private fun buscarPedidoPorStringLegacy(mesaId: String) {
        val estadosActivos = setOf(
            "COMANDADO", "EN_PREPARACION",
            "PENDIENTE_PREPARACION", "LISTO_PARA_ENTREGAR", "ENTREGADO"
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
        // Observar cuando el mozoId se carga desde el repositorio
        // y guardarlo en prefs para próximas sesiones
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