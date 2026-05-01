package com.donabere.amm

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.donabere.amm.adapter.MenuAdapter
import com.donabere.amm.databinding.ActivityMenuBinding
import com.donabere.amm.viewmodel.MenuViewModel

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding
    private val viewModel: MenuViewModel by viewModels()
    private lateinit var adapter: MenuAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeViewModel()

        viewModel.cargarMenu()
    }

    private fun setupRecyclerView() {
        adapter = MenuAdapter(emptyList()) { dish ->
            Toast.makeText(this, "Plato seleccionado: ${dish.title}", Toast.LENGTH_SHORT).show()
        }
        binding.rvMenu.layoutManager = LinearLayoutManager(this)
        binding.rvMenu.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.menu.observe(this) { dishes ->
            adapter.updateData(dishes)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.pbMenuLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
