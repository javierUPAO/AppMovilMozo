package com.donabere.amm.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.donabere.amm.ui.fragment.BebidasFragment
import com.donabere.amm.ui.fragment.PlatosFragment

class PaginaMenuAdapter : FragmentStateAdapter {

    // Constructor para Activity (MenuActivity, SeleccionProductoActivity)
    constructor(activity: FragmentActivity) : super(activity)

    // Constructor para Fragment (MenuFragment)
    constructor(fragment: Fragment) : super(fragment)

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PlatosFragment()
            1 -> BebidasFragment()
            else -> throw IllegalArgumentException("Posición inválida")
        }
    }
}