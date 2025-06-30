package com.example.utpsalud.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.utpsalud.viewmodel.SplashViewModel
import com.example.utpsalud.view.HomeActivity


class SplashActivity : AppCompatActivity() {

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        observarViewModel()
        viewModel.verificarUsuario()
    }

    private fun observarViewModel() {
        viewModel.usuarioEsAdmin.observe(this) { esAdmin ->
            when (esAdmin) {
                true -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
                false -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
                null -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }
}