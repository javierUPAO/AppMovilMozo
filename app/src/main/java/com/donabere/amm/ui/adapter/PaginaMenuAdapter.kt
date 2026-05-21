package com.donabere.amm.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.donabere.amm.ui.fragment.BebidasFragment
import com.donabere.amm.ui.fragment.PlatosFragment

class PaginaMenuAdapter : FragmentStateAdapter {

    private val onProductoSeleccionado: ((productoId: String, nombre: String, precio: Double, imagen: String) -> Unit)?

    // Constructor para Activity con callback (CrearPedidoActivity)
    constructor(
        activity: FragmentActivity,
        onProductoSeleccionado: ((productoId: String, nombre: String, precio: Double, imagen: String) -> Unit)? = null
    ) : super(activity) {
        this.onProductoSeleccionado = onProductoSeleccionado
    }

    // Constructor para Fragment con callback (MenuFragment si se necesita)
    constructor(
        fragment: Fragment,
        onProductoSeleccionado: ((productoId: String, nombre: String, precio: Double, imagen: String) -> Unit)? = null
    ) : super(fragment) {
        this.onProductoSeleccionado = onProductoSeleccionado
    }

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PlatosFragment.newInstance(onProductoSeleccionado)
            1 -> BebidasFragment.newInstance(onProductoSeleccionado)
            else -> throw IllegalArgumentException("Posición inválida: $position")
        }
    }
}