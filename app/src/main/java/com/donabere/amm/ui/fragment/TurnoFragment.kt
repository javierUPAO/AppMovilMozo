package com.donabere.amm.ui.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.donabere.amm.databinding.FragmentTurnoBinding
import com.donabere.amm.viewmodel.TurnoViewModel

class TurnoFragment : Fragment() {

    private var _binding: FragmentTurnoBinding? = null
    private val binding get() = _binding!!

    private val turnoViewModel: TurnoViewModel by viewModels()

    private lateinit var sharedPreferences: SharedPreferences
    private var mozoId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentTurnoBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupListeners()

        sharedPreferences = requireContext().getSharedPreferences("app_prefs", 0)
        mozoId = sharedPreferences.getString("usuario_id", "") ?: ""
        Log.d("IdMozo","EL mozoId es : $mozoId")
        turnoViewModel.verificarTurno(mozoId)
        turnoViewModel.cargarUltimoTurno(mozoId)
    }
    private fun updateTurnoButtons(activo: Boolean) {

        val naranja = android.graphics.Color.parseColor("#F59E0B")
        val blanco = android.graphics.Color.WHITE

        // ABRIR
        if (activo) {
            binding.btnAbrirTurno.setBackgroundColor(blanco)
            binding.btnAbrirTurno.setStrokeWidth(4)
            binding.btnAbrirTurno.setStrokeColorResource(android.R.color.transparent)
            binding.btnAbrirTurno.setTextColor(android.graphics.Color.BLACK)
        } else {
            binding.btnAbrirTurno.setBackgroundColor(naranja)
            binding.btnAbrirTurno.setStrokeWidth(0)
            binding.btnAbrirTurno.setTextColor(android.graphics.Color.BLACK)
        }

        // CERRAR
        if (!activo) {
            binding.btnCerrarTurno.setBackgroundColor(blanco)
            binding.btnCerrarTurno.setStrokeWidth(4)
            binding.btnCerrarTurno.setStrokeColorResource(android.R.color.transparent)
            binding.btnCerrarTurno.setTextColor(android.graphics.Color.BLACK)
        } else {
            binding.btnCerrarTurno.setBackgroundColor(naranja)
            binding.btnCerrarTurno.setStrokeWidth(0)
            binding.btnCerrarTurno.setTextColor(android.graphics.Color.BLACK)
        }
    }


    private fun setupObservers() {

        turnoViewModel.success.observe(viewLifecycleOwner) { message ->

            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        turnoViewModel.ultimoTurno.observe(viewLifecycleOwner) { turno ->

            binding.totalMesas.text =
                turno?.totalMesas?.toString() ?: "0"

            binding.totalPedidos.text =
                turno?.totalPedidos?.toString() ?: "0"

            binding.totalCobrado.text =
                String.format("%.2f", turno?.totalVendido ?: 0.0)
        }

        turnoViewModel.turnoActivo.observe(viewLifecycleOwner) { turno ->

            val activo = turno != null
            binding.estadoTurno.text = if (activo) "Abierto" else "Cerrado"

            binding.btnAbrirTurno.isEnabled = !activo
            binding.btnCerrarTurno.isEnabled = activo
            updateTurnoButtons(activo)
        }

        turnoViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {

        binding.btnAbrirTurno.setOnClickListener {

            val turnoActual = turnoViewModel.getTurnoActual()

            if (turnoActual != null) {
                Toast.makeText(requireContext(), "Ya hay un turno abierto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            turnoViewModel.abrirTurno(mozoId)

        }

        binding.btnCerrarTurno.setOnClickListener {

            val turnoActual = turnoViewModel.getTurnoActual()

            if (turnoActual == null) {
                Toast.makeText(requireContext(), "No hay turno activo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            turnoViewModel.cerrarTurno(mozoId)

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}