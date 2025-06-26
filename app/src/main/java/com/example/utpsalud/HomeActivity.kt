package com.example.utpsalud

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.utpsalud.databinding.ActivityHomeBinding
import com.example.utpsalud.ui.historial.HistorialFragment
import com.example.utpsalud.ui.home.HomeFragment
import com.example.utpsalud.ui.home.ListapacientesFragment
import com.example.utpsalud.ui.perfil.PerfilFragment
import com.example.utpsalud.ui.sugerency.SugerencyFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var esAdmin: Boolean = false // Guardar si el usuario es administrador

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val uid = currentUser.uid
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        esAdmin = doc.getBoolean("esAdministrador") ?: false
                        if (esAdmin) {
                            replaceFragment(ListapacientesFragment())
                        } else {
                            replaceFragment(HomeFragment())
                        }
                    } else {
                        replaceFragment(HomeFragment())
                    }
                }
                .addOnFailureListener {
                    replaceFragment(HomeFragment())
                }
        } else {
            replaceFragment(HomeFragment())
        }

        // Configurar navegación superior
        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.navBuscar -> {
                    startActivity(Intent(this, BuscarActivity::class.java))
                    true
                }
                R.id.navUsuarios -> {
                    startActivity(Intent(this, UsuariosActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Configurar navegación inferior
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> {
                    if (esAdmin) {
                        replaceFragment(ListapacientesFragment())
                    } else {
                        replaceFragment(HomeFragment())
                    }
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
        if (currentFragment != null && currentFragment::class == newFragment::class) {
            return
        }
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, newFragment)
            .commit()
    }
}
