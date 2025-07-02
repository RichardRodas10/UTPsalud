package com.example.utpsalud.view.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.utpsalud.viewmodel.SplashViewModel

class SplashActivity : AppCompatActivity() {

    // Creo la instancia del ViewModel para SplashActivity usando delegación by viewModels()
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Desactivo el modo noche para que la app siempre tenga modo claro
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        // Empiezo a observar los cambios que el ViewModel me notifique sobre el usuario
        observarViewModel()
        // Le pido al ViewModel que verifique si hay usuario logueado y si es admin
        viewModel.verificarUsuario()
    }

    private fun observarViewModel() {
        viewModel.usuarioEsAdmin.observe(this) { esAdmin ->
            val destino = when (esAdmin) {
                true -> HomeActivity::class.java      // Admin va a HomeActivity (puedes cambiar a AdminActivity si tienes)
                false -> HomeActivity::class.java      // Usuario normal también va a HomeActivity (puedes cambiar a otra actividad)
                null -> LoginActivity::class.java      // No logueado va a LoginActivity
            }
            startActivity(Intent(this, destino))
            finish() // Siempre cierro Splash para no regresar con atrás
        }
    }
}