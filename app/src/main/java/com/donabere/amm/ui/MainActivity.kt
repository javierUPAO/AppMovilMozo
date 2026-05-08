package com.donabere.amm.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.donabere.amm.R
import com.donabere.amm.databinding.ActivityMainBinding
import com.donabere.amm.ui.fragment.MenuFragment
import com.donabere.amm.ui.fragment.MesasFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setupBottomNavigation(savedInstanceState == null)
    }

    private fun setupBottomNavigation(selectDefault: Boolean) {
        binding.bottomNav.setOnItemSelectedListener { item ->
            if (binding.bottomNav.selectedItemId == item.itemId) {
                return@setOnItemSelectedListener true
            }

            when (item.itemId) {
                R.id.nav_home -> {
                    showFragment(MesasFragment(), "mesas")
                    updateHeader(item.itemId)
                    true
                }
                R.id.nav_menu -> {
                    showFragment(MenuFragment(), "menu")
                    updateHeader(item.itemId)
                    true
                }
                else -> false
            }
        }

        if (selectDefault) {
            showFragment(MesasFragment(), "mesas")
            updateHeader(R.id.nav_home)
            binding.bottomNav.selectedItemId = R.id.nav_home
        } else {
            updateHeader(binding.bottomNav.selectedItemId)
        }
    }

    private fun updateHeader(itemId: Int) {
        val (titleRes, subtitleRes) = when (itemId) {
            R.id.nav_menu -> R.string.header_menu_title to R.string.header_menu_subtitle
            else -> R.string.header_mesas_title to R.string.header_mesas_subtitle
        }
        binding.headerTitle.setText(titleRes)
        binding.headerSubtitle.setText(subtitleRes)
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        val existing = supportFragmentManager.findFragmentByTag(tag)
        val target = existing ?: fragment
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragmentContainer, target, tag)
            .commit()
    }
}
