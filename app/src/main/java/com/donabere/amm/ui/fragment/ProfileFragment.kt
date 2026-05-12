package com.donabere.amm.ui.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.donabere.amm.databinding.FragmentProfileBinding
import com.donabere.amm.viewmodel.ProfileViewModel
import com.donabere.amm.utils.BiometricUtils

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val profileViewModel: ProfileViewModel by viewModels()
    private lateinit var sharedPreferences: SharedPreferences
    private var userEmail: String = ""

    private var letterprofile : String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("app_prefs", 0)
        userEmail = sharedPreferences.getString("user_email", "") ?: ""
        letterprofile = obtenerIniciales(userEmail)
        setupUI()
        setupObservers()
        setupListeners()
    }

    private fun obtenerIniciales(texto: String): String {
        return texto
            .take(2)
            .uppercase()
    }
    private fun setupUI() {
        // Mostrar email del usuario
        binding.tvUserEmail.text = userEmail
        binding.tvUserName.text = "Mi Perfil"
        binding.profileLetter.text=letterprofile
    }

    private fun setupListeners() {
        binding.btnRegisterFingerprint.setOnClickListener {
            if (!BiometricUtils.isBiometricAvailable(requireContext())) {
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = "Tu dispositivo no soporta autenticación biométrica"
                return@setOnClickListener
            }

            // Mostrar prompt biométrico PRIMERO
            BiometricUtils.showBiometricPrompt(
                requireActivity(),
                "Registrar Huella",
                "Toca tu huella para registrarla",
                "Cancelar",
                object : BiometricUtils.BiometricCallback {
                    override fun onAuthenticationSuccess() {
                        // Una vez autenticado, proceder con el registro
                        profileViewModel.registerFingerprint(userEmail)
                    }

                    override fun onAuthenticationError(errorMessage: String) {
                        binding.tvError.visibility = View.VISIBLE
                        binding.tvError.text = "Error: $errorMessage"
                    }

                    override fun onAuthenticationFailed() {
                        binding.tvError.visibility = View.VISIBLE
                        binding.tvError.text = "Huella no reconocida"
                    }
                }
            )
        }

        binding.btnDeleteFingerprint.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Huella")
                .setMessage("¿Estás seguro de que deseas eliminar tu huella registrada?")
                .setPositiveButton("Sí, eliminar") { _, _ ->
                    profileViewModel.deleteFingerprint(userEmail)
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun setupObservers() {
        profileViewModel.fingerprintStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                ProfileViewModel.FingerprintStatus.REGISTERED -> {
                    binding.tvFingerprintStatus.text = "✓ Huella Registrada"
                    binding.tvFingerprintStatus.setTextColor(
                        requireContext().getColor(android.R.color.holo_green_dark)
                    )
                    binding.btnRegisterFingerprint.visibility = View.GONE
                    binding.btnDeleteFingerprint.visibility = View.VISIBLE
                    binding.tvFingerprintDescription.text = "Tu huella está registrada y lista para usar en login"
                }
                ProfileViewModel.FingerprintStatus.NOT_REGISTERED -> {
                    binding.tvFingerprintStatus.text = "No Registrada"
                    binding.tvFingerprintStatus.setTextColor(
                        requireContext().getColor(android.R.color.holo_orange_dark)
                    )
                    binding.btnRegisterFingerprint.visibility = View.VISIBLE
                    binding.btnDeleteFingerprint.visibility = View.GONE
                    binding.tvFingerprintDescription.text = "Registra tu huella para entradas más rápidas"
                }
            }
        }

        profileViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnRegisterFingerprint.isEnabled = !loading
            binding.btnDeleteFingerprint.isEnabled = !loading
        }

        profileViewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = error
            }
        }

        profileViewModel.success.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                binding.tvSuccess.visibility = View.VISIBLE
                binding.tvSuccess.text = message
                // Desaparecer después de 3 segundos
                binding.root.postDelayed({
                    if (isAdded) {
                        binding.tvSuccess.visibility = View.GONE
                    }
                }, 3000)
            }
        }
    }
}
