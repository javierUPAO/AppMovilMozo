package com.donabere.amm.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.donabere.amm.R
import com.donabere.amm.databinding.ActivityMainBinding
import com.donabere.amm.service.PedidoNotificacionesService
import com.donabere.amm.ui.fragment.ListaPedidosFragment
import com.donabere.amm.ui.fragment.MenuFragment
import com.donabere.amm.ui.fragment.MesasFragment
import com.donabere.amm.ui.fragment.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // HU 5.1 · Al conceder (o denegar) el permiso, intentamos arrancar el servicio.
    private val permisoNotifLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            iniciarServicioNotificaciones()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setupBottomNavigation(savedInstanceState == null)
        prepararNotificaciones()
    }

    // ─── HU 5.1 · Notificaciones de estado del pedido ─────────────────────────

    private fun prepararNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permisoNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            iniciarServicioNotificaciones()
        }
    }

    private fun iniciarServicioNotificaciones() {
        val mozoId = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("mozoId", null)
        if (!mozoId.isNullOrBlank()) {
            PedidoNotificacionesService.iniciar(this, mozoId)
        }
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
                R.id.nav_pedidos -> {
                    showFragment(ListaPedidosFragment(), "pedidos")
                    updateHeader(item.itemId)
                    true
                }
                R.id.nav_profile -> {
                    showFragment(ProfileFragment(), "profile")
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
            R.id.nav_pedidos -> R.string.header_pedidos_title to R.string.header_pedidos_subtitle
            R.id.nav_profile -> R.string.header_profile_title to R.string.header_profile_subtitle
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
