package com.donabere.amm.ui.adapter

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.donabere.amm.ui.fragment.PlatosFragment
import com.donabere.amm.ui.fragment.BebidasFragment

class PaginaMenuAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2 

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PlatosFragment()
            1 -> BebidasFragment()
            else -> throw IllegalArgumentException("Posición inválida")
        }
    }
}