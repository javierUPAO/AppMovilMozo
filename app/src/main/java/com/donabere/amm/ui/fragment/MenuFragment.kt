package com.donabere.amm.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.donabere.amm.databinding.ActivityMenuBinding
import com.donabere.amm.ui.adapter.PaginaMenuAdapter
import com.google.android.material.tabs.TabLayoutMediator

class MenuFragment : Fragment() {

    private var _binding: ActivityMenuBinding? = null
    private val binding get() = _binding!!
    private val titulosPestanas = listOf("Platos", "Bebidas")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarPaginador()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun configurarPaginador() {
        val adaptador = PaginaMenuAdapter(this)
        binding.viewPagerPlatos.adapter = adaptador

        TabLayoutMediator(binding.tabLayoutCategorias, binding.viewPagerPlatos) { pestana, posicion ->
            pestana.text = titulosPestanas[posicion]
        }.attach()
    }
}
