package com.donabere.amm.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.donabere.amm.adapter.BebidasAdapter
import com.donabere.amm.databinding.FragmentBebidasBinding
import com.donabere.amm.viewmodel.BebidasViewModel

class BebidasFragment : Fragment() {

    private var _binding: FragmentBebidasBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BebidasViewModel by viewModels()
    private lateinit var adapter: BebidasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBebidasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        viewModel.cargarBebidas()
    }

    private fun setupRecyclerView() {
        adapter = BebidasAdapter(emptyList()) { }
        binding.rvBebidas.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvBebidas.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.bebidas.observe(viewLifecycleOwner) { bebidas ->
            adapter.updateData(bebidas)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.pbBebidasLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}