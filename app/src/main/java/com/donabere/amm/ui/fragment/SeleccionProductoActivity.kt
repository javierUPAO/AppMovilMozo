package com.donabere.amm.ui

import android.app.Activity
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
        // Extras de entrada
        const val EXTRA_MESA_ID = "extra_mesa_id"

        // Extras de salida
        const val EXTRA_PRODUCTO_ID     = "extra_producto_id"
        const val EXTRA_PRODUCTO_NOMBRE = "extra_producto_nombre"
        const val EXTRA_PRODUCTO_PRECIO = "extra_producto_precio"

        // Fragment Result keys
        const val RESULT_PLATO  = "result_plato"
        const val RESULT_BEBIDA = "result_bebida"
        const val REQUEST_CODE  = 1001

        fun newIntent(context: Context, mesaId: Int): Intent =
            Intent(context, SeleccionProductoActivity::class.java).apply {
                putExtra(EXTRA_MESA_ID, mesaId)
            }

        fun extraerResultado(data: Intent?): Triple<Int, String, Double>? {
            data ?: return null
            val id     = data.getIntExtra(EXTRA_PRODUCTO_ID, -1)
            val nombre = data.getStringExtra(EXTRA_PRODUCTO_NOMBRE) ?: return null
            val precio = data.getDoubleExtra(EXTRA_PRODUCTO_PRECIO, 0.0)
            if (id == -1) return null
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
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        configurarPaginador()
        escucharSeleccion()
    }

    private fun configurarPaginador() {
        // Reutiliza el mismo PaginaMenuAdapter que MenuActivity
        val adaptador = PaginaMenuAdapter(this)
        binding.viewPagerProductos.adapter = adaptador

        TabLayoutMediator(binding.tabLayoutCategorias, binding.viewPagerProductos) { tab, pos ->
            tab.text = listOf("Platos", "Bebidas")[pos]
        }.attach()
    }

    private fun escucharSeleccion() {
        // Resultado de PlatosFragment
        supportFragmentManager.setFragmentResultListener(
            RESULT_PLATO, this
        ) { _, bundle ->
            val id     = bundle.getInt(EXTRA_PRODUCTO_ID)
            val nombre = bundle.getString(EXTRA_PRODUCTO_NOMBRE, "")
            val precio = bundle.getDouble(EXTRA_PRODUCTO_PRECIO)
            devolverProducto(id, nombre, precio)
        }

        // Resultado de BebidasFragment
        supportFragmentManager.setFragmentResultListener(
            RESULT_BEBIDA, this
        ) { _, bundle ->
            val id     = bundle.getInt(EXTRA_PRODUCTO_ID)
            val nombre = bundle.getString(EXTRA_PRODUCTO_NOMBRE, "")
            val precio = bundle.getDouble(EXTRA_PRODUCTO_PRECIO)
            devolverProducto(id, nombre, precio)
        }
    }

    private fun devolverProducto(id: Int, nombre: String, precio: Double) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_PRODUCTO_ID,     id)
            putExtra(EXTRA_PRODUCTO_NOMBRE, nombre)
            putExtra(EXTRA_PRODUCTO_PRECIO, precio)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}