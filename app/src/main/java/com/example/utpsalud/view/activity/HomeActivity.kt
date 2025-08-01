package com.example.utpsalud.view.activity

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.utpsalud.R
import com.example.utpsalud.databinding.ActivityHomeBinding
import com.example.utpsalud.view.fragment.ChatFragment
import com.example.utpsalud.view.fragment.HistorialFragment
import com.example.utpsalud.view.fragment.HistorialmedicoFragment
import com.example.utpsalud.view.fragment.HomeFragment
import com.example.utpsalud.view.fragment.ListapacientesFragment
import com.example.utpsalud.view.fragment.PerfilFragment
import com.example.utpsalud.viewmodel.ChatViewModel
import com.example.utpsalud.viewmodel.HomeViewModel
import com.google.firebase.firestore.ListenerRegistration

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var esAdmin: Boolean = false
    private lateinit var badgeCountText: TextView
    private lateinit var badgeIconImage: ImageView
    private var solicitudListener: ListenerRegistration? = null

    private val viewModel: HomeViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()  // <-- Nuevo ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.esAdmin.observe(this) { admin ->
            esAdmin = admin
            inicializarUI()
        }

        viewModel.obtenerRolUsuarioActual()

        // Verificar si se volvió desde una medición manual
        val desdeMedicion = intent.getBooleanExtra("desde_medicion", false)
        if (desdeMedicion) {
            mostrarDialogSiEsNuevaMedicion()
        }

        // Cargar contactos y mensajes no leídos en ChatViewModel
        chatViewModel.cargarContactos()

        // Observar badge de mensajes no leídos y actualizar badge en BottomNavigationView
        chatViewModel.totalMensajesNoLeidos.observe(this) { total ->
            val badge = binding.bottomNavigation.getOrCreateBadge(R.id.navChat)
            if (total > 0) {
                badge.isVisible = true
                badge.number = total
                badge.backgroundColor = getColor(R.color.button)
                badge.badgeTextColor = getColor(android.R.color.white)
            } else {
                badge.isVisible = false
                badge.clearNumber()
            }
        }
    }

    private fun mostrarDialogSiEsNuevaMedicion() {
        val prefs = getSharedPreferences("utp_salud_prefs", MODE_PRIVATE)
        val ultimaIdMostrada = prefs.getString("ultima_medicion_mostrada", "")

        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        db.collection("usuarios")
            .document(uid)
            .collection("mediciones")
            .orderBy("fechaMedicion", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                val doc = result.documents.firstOrNull()
                if (doc != null && doc.id != ultimaIdMostrada) {
                    val resultadoFC = doc.getString("resultadoFrecuenciaCardiaca") ?: return@addOnSuccessListener
                    val resultadoOxi = doc.getString("resultadoOxigeno") ?: return@addOnSuccessListener
                    val estado = doc.getString("estadoSalud") ?: return@addOnSuccessListener

                    val mensajeEstado = when (estado) {
                        "Saludable" -> "¡Buen trabajo! Tus signos vitales están en un rango saludable. Continúa con tu estilo de vida saludable."
                        "Crítico" -> "Tus signos vitales están en un rango crítico. Busca atención médica inmediata."
                        "Moderado" -> "Uno o ambos valores están fuera del rango ideal. Se recomienda descanso o consultar a un profesional."
                        else -> ""
                    }

                    mostrarDialogSalud(resultadoFC, resultadoOxi, estado, mensajeEstado)

                    // Guardar ID para no mostrarlo otra vez
                    prefs.edit().putString("ultima_medicion_mostrada", doc.id).apply()
                }
            }
    }

    private fun mostrarDialogSalud(
        resultadoFrecuencia: String,
        resultadoOxigeno: String,
        estadoSalud: String,
        mensaje: String
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_salud, null)

        val tvFrecuencia = dialogView.findViewById<TextView>(R.id.tvFrecuenciaCardiaca)
        val tvOxigeno = dialogView.findViewById<TextView>(R.id.tvOxigenoSangre)
        val tvEstado = dialogView.findViewById<TextView>(R.id.tvEstadoSalud)
        val tvMensaje = dialogView.findViewById<TextView>(R.id.tvMensajeEstadoSalud)
        val btnCerrar = dialogView.findViewById<TextView>(R.id.btnClose)

        // Mostrar resultados
        tvFrecuencia.text = "Frecuencia Cardíaca: $resultadoFrecuencia"
        tvOxigeno.text = "Oxígeno en Sangre: $resultadoOxigeno"
        tvEstado.text = "Estado de Salud: $estadoSalud"
        tvMensaje.text = mensaje

        // Color del estado de salud
        val colorEstado = when (estadoSalud) {
            "Crítico" -> Color.RED
            "Moderado" -> Color.parseColor("#FFA500") // Naranja
            "Saludable" -> ContextCompat.getColor(this, R.color.green)
            else -> Color.BLACK
        }
        tvEstado.setTextColor(colorEstado)

        // Crear diálogo respetando bordes
        val dialog = Dialog(this)
        dialog.setContentView(dialogView)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCerrar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun inicializarUI() {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        // Configuración Badge en menú
        val menu = binding.topAppBar.menu
        val navUsuariosItem = menu.findItem(R.id.navUsuarios)
        val actionView = layoutInflater.inflate(R.layout.badge_action_view, null)
        navUsuariosItem.actionView = actionView
        badgeCountText = actionView.findViewById(R.id.badgeCount)
        badgeIconImage = actionView.findViewById(R.id.iconImage)

        actionView.setOnClickListener {
            binding.topAppBar.menu.performIdentifierAction(R.id.navUsuarios, 0)
        }

        if (currentUser != null) {
            val uid = currentUser.uid

            if (esAdmin) {
                replaceFragment(ListapacientesFragment())
            } else {
                // Recoger datos enviados desde LecturaActivity
                val hr = intent.getIntExtra("frecuencia_cardiaca", -1)
                val spo2 = intent.getIntExtra("saturacion_oxigeno", -1)
                val temp = intent.getFloatExtra("temperatura_promedio", -1f)

                val homeFragment = HomeFragment()

                if (hr != -1 && spo2 != -1 && temp != -1f) {
                    val bundle = Bundle().apply {
                        putInt("frecuencia_cardiaca", hr)
                        putInt("saturacion_oxigeno", spo2)
                        putFloat("temperatura_promedio", temp)
                    }
                    homeFragment.arguments = bundle
                }

                replaceFragment(homeFragment)
            }

            escucharSolicitudesEnTiempoReal(uid)
        } else {
            replaceFragment(HomeFragment())
        }

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

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navHome -> {
                    if (esAdmin) {
                        replaceFragment(ListapacientesFragment())
                    } else {
                        // Volver a crear el HomeFragment con los datos si están disponibles
                        val hr = intent.getIntExtra("frecuencia_cardiaca", -1)
                        val spo2 = intent.getIntExtra("saturacion_oxigeno", -1)
                        val temp = intent.getFloatExtra("temperatura_promedio", -1f)

                        val homeFragment = HomeFragment().apply {
                            if (hr != -1 && spo2 != -1 && temp != -1f) {
                                arguments = Bundle().apply {
                                    putInt("frecuencia_cardiaca", hr)
                                    putInt("saturacion_oxigeno", spo2)
                                    putFloat("temperatura_promedio", temp)
                                }
                            }
                        }

                        replaceFragment(homeFragment)
                    }
                    true
                }
                R.id.navHistorial -> {
                    if (esAdmin) {
                        replaceFragment(HistorialmedicoFragment())
                    } else {
                        replaceFragment(HistorialFragment())
                    }
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
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

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
