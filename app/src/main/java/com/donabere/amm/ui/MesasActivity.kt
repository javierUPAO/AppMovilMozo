package com.donabere.amm.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.donabere.amm.databinding.ActivityMesasBinding
import com.donabere.amm.ui.adapter.ItemMesaAdapter
import com.donabere.amm.viewmodel.MesasViewModel

class MesasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMesasBinding
    private val viewModel: MesasViewModel by viewModels()

    // Launcher que refresca mesas cuando regresa de CrearPedidoActivity
    private val crearPedidoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Siempre refrescar, haya confirmado pedido o no
        viewModel.fetchMesas()
    }

    private val adapter = ItemMesaAdapter { mesa ->
        if (mesa.status == 0) {
            crearPedidoLauncher.launch(
                CrearPedidoActivity.newIntent(
                    context  = this,
                    mesasIds = listOf(mesa.id),
                    mozoId   = 1
                )
            )
        } else {
            android.widget.Toast.makeText(
                this,
                "Mesa ${mesa.id} está ocupada",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMesasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeViewModel()
        viewModel.fetchMesas()
    }

    private fun setupRecyclerView() {
        binding.rvMesas.layoutManager = LinearLayoutManager(this)
        binding.rvMesas.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.mesas.observe(this) { mesas ->
            adapter.submitList(mesas)
            binding.rvMesas.visibility = View.VISIBLE
            binding.tvError.visibility = View.GONE
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.rvMesas.visibility = View.GONE
                binding.tvError.visibility = View.GONE
            }
        }

        viewModel.error.observe(this) { errorMessage ->
            binding.tvError.text = errorMessage
            binding.tvError.visibility = View.VISIBLE
            binding.rvMesas.visibility = View.GONE
        }
    }
}