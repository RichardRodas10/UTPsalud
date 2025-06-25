package com.example.utpsalud

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.utpsalud.databinding.ActivityHomeBinding
import com.example.utpsalud.ui.historial.HistorialFragment
import com.example.utpsalud.ui.home.HomeFragment
import com.example.utpsalud.ui.perfil.PerfilFragment
import com.example.utpsalud.ui.sugerency.SugerencyFragment

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cargar fragmento inicial
        replaceFragment(HomeFragment())

        // Configurar navegación inferior
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.navHistorial -> {
                    replaceFragment(HistorialFragment())
                    true
                }
                R.id.navChat -> {
                    replaceFragment(SugerencyFragment())
                    true
                }
                R.id.navPerfil -> {
                    replaceFragment(PerfilFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(newFragment: Fragment) {
        val currentFragment = supportFragmentManager.findFragmentById(binding.fragmentContainer.id)

        // Evitar reemplazo si el fragmento ya está activo,para no sobrecargar la pila de fragmentos
        if (currentFragment != null && currentFragment::class == newFragment::class) {
            return
        }

        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, newFragment)
            .commit()
    }

}
