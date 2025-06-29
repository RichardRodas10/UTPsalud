package com.example.utpsalud

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.utpsalud.databinding.ActivityHomeBinding
import com.example.utpsalud.ui.chat.ChatFragment
import com.example.utpsalud.ui.historial.HistorialFragment
import com.example.utpsalud.ui.home.HomeFragment
import com.example.utpsalud.ui.home.ListapacientesFragment
import com.example.utpsalud.ui.perfil.PerfilFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var esAdmin: Boolean = false
    private lateinit var badgeCountText: TextView
    private lateinit var badgeIconImage: ImageView
    private var solicitudListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        // Cargar badge personalizado al ítem de menú
        val menu = binding.topAppBar.menu
        val navUsuariosItem = menu.findItem(R.id.navUsuarios)
        val actionView = layoutInflater.inflate(R.layout.badge_action_view, null)
        navUsuariosItem.actionView = actionView
        badgeCountText = actionView.findViewById(R.id.badgeCount)
        badgeIconImage = actionView.findViewById(R.id.iconImage)

        // Hacer clic sobre el icono personalizado
        actionView.setOnClickListener {
            binding.topAppBar.menu.performIdentifierAction(R.id.navUsuarios, 0)
        }

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
                        escucharSolicitudesEnTiempoReal(uid)
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

        // Toolbar (menú)
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

        // Bottom navigation
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
                    replaceFragment(ChatFragment())
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

    override fun onDestroy() {
        super.onDestroy()
        solicitudListener?.remove()
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

    private fun escucharSolicitudesEnTiempoReal(userId: String) {
        val db = FirebaseFirestore.getInstance()

        solicitudListener = db.collection("solicitudes")
            .whereEqualTo("receptorId", userId)
            .whereEqualTo("estado", "pendiente")
            .addSnapshotListener { snapshots, _ ->
                val cantidad = snapshots?.size() ?: 0
                if (cantidad > 0) {
                    badgeCountText.text = cantidad.toString()
                    badgeCountText.visibility = View.VISIBLE
                } else {
                    badgeCountText.visibility = View.GONE
                }
            }
    }
}