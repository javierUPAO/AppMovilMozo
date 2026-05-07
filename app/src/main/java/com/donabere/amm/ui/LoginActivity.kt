package com.donabere.amm.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.donabere.amm.databinding.ActivityLoginBinding
import com.donabere.amm.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnIngresar.setOnClickListener {
            val email = binding.etUsuario.text.toString()
            val contrasena = binding.etContrasena.text.toString()
            binding.tvError.visibility = View.GONE
            viewModel.login(email, contrasena)
        }

        viewModel.loginExitoso.observe(this) { exitoso ->
            if (exitoso) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnIngresar.isEnabled = !loading
        }

        viewModel.error.observe(this) { mensaje ->
            binding.tvError.visibility = View.VISIBLE
            binding.tvError.text = mensaje
        }
    }
}