package com.donabere.amm.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.donabere.amm.databinding.ActivitySeleccionProductoBinding
import com.donabere.amm.ui.adapter.PaginaMenuAdapter
import com.google.android.material.tabs.TabLayoutMediator

class SeleccionProductoActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeleccionProductoBinding

    companion object {
        const val EXTRA_MESA_ID         = "extra_mesa_id"
        const val EXTRA_PRODUCTO_ID     = "extra_producto_id"     // String
        const val EXTRA_PRODUCTO_NOMBRE = "extra_producto_nombre" // String
        const val EXTRA_PRODUCTO_PRECIO = "extra_producto_precio" // Double

        const val RESULT_PLATO  = "result_plato"
        const val RESULT_BEBIDA = "result_bebida"

        fun newIntent(context: Context, mesaId: String): Intent =
            Intent(context, SeleccionProductoActivity::class.java).apply {
                putExtra(EXTRA_MESA_ID, mesaId)
            }

        /** Extrae el resultado. El ID ahora es String. */
        fun extraerResultado(data: Intent?): Triple<String, String, Double>? {
            data ?: return null
            val id     = data.getStringExtra(EXTRA_PRODUCTO_ID) ?: return null
            val nombre = data.getStringExtra(EXTRA_PRODUCTO_NOMBRE) ?: return null
            val precio = data.getDoubleExtra(EXTRA_PRODUCTO_PRECIO, 0.0)
            return Triple(id, nombre, precio)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeleccionProductoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Seleccionar producto"

        binding.toolbar.setNavigationOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        configurarPaginador()
        escucharSeleccion()
    }

    private fun configurarPaginador() {
        // Sin callback → los fragments usan setFragmentResult (modo legacy)
        val adaptador = PaginaMenuAdapter(this)
        binding.viewPagerProductos.adapter = adaptador

        TabLayoutMediator(binding.tabLayoutCategorias, binding.viewPagerProductos) { tab, pos ->
            tab.text = listOf("Platos", "Bebidas")[pos]
        }.attach()
    }

    private fun escucharSeleccion() {
        supportFragmentManager.setFragmentResultListener(RESULT_PLATO, this) { _, bundle ->
            devolverProducto(
                id     = bundle.getString(EXTRA_PRODUCTO_ID, ""),
                nombre = bundle.getString(EXTRA_PRODUCTO_NOMBRE, ""),
                precio = bundle.getDouble(EXTRA_PRODUCTO_PRECIO)
            )
        }
        supportFragmentManager.setFragmentResultListener(RESULT_BEBIDA, this) { _, bundle ->
            devolverProducto(
                id     = bundle.getString(EXTRA_PRODUCTO_ID, ""),
                nombre = bundle.getString(EXTRA_PRODUCTO_NOMBRE, ""),
                precio = bundle.getDouble(EXTRA_PRODUCTO_PRECIO)
            )
        }
    }

    // ID ahora es String — ya no getIntExtra / putIntExtra
    private fun devolverProducto(id: String, nombre: String, precio: Double) {
        if (id.isBlank() || nombre.isBlank()) return
        setResult(RESULT_OK, Intent().apply {
            putExtra(EXTRA_PRODUCTO_ID,     id)
            putExtra(EXTRA_PRODUCTO_NOMBRE, nombre)
            putExtra(EXTRA_PRODUCTO_PRECIO, precio)
        })
        finish()
    }
}