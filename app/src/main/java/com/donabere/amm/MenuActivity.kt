package com.donabere.amm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.donabere.amm.databinding.ActivityMenuBinding
import com.donabere.amm.ui.adapter.PaginaMenuAdapter
import com.google.android.material.tabs.TabLayoutMediator

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding
    private val titulosPestanas = listOf("Platos", "Bebidas")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarPaginador()
    }

    private fun configurarPaginador() {
        val adaptador = PaginaMenuAdapter(this)
        binding.viewPagerPlatos.adapter = adaptador

        TabLayoutMediator(binding.tabLayoutCategorias, binding.viewPagerPlatos) { pestana, posicion ->
            pestana.text = titulosPestanas[posicion]
        }.attach()
    }
}