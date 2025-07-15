package com.example.utpsalud.view.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.utpsalud.R
import com.example.utpsalud.view.adapter.ChatAdapter
import com.example.utpsalud.viewmodel.ChatViewModel
import com.example.utpsalud.viewmodel.HomeViewModel
import de.hdodenhof.circleimageview.CircleImageView

class ChatActivity : AppCompatActivity() {

    private lateinit var iconBack: ImageView
    private lateinit var imagePerfil: CircleImageView
    private lateinit var textNombre: TextView
    private lateinit var recyclerMensajes: RecyclerView
    private lateinit var editMensaje: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var progressBar: ProgressBar

    private lateinit var chatAdapter: ChatAdapter

    private val chatViewModel: ChatViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()  // ViewModel para rol

    private var uidReceptor: String? = null
    private var numeroPendienteDeLlamar: String? = null

    private var soyAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        iconBack = findViewById(R.id.iconBack)
        imagePerfil = findViewById(R.id.profileImageContacto)
        textNombre = findViewById(R.id.textNombreContacto)
        recyclerMensajes = findViewById(R.id.recyclerMensajes)
        editMensaje = findViewById(R.id.editMensaje)
        btnSend = findViewById(R.id.btnSend)
        progressBar = findViewById(R.id.progressBar)

        uidReceptor = intent.getStringExtra("uid")
        val nombre = intent.getStringExtra("nombre") ?: ""
        val apellido = intent.getStringExtra("apellido") ?: ""
        val fotoPerfilBase64 = intent.getStringExtra("fotoPerfilBase64")

        val primerNombre = nombre.split(" ").firstOrNull() ?: ""
        val primerApellido = apellido.split(" ").firstOrNull() ?: ""
        textNombre.text = "$primerNombre $primerApellido"

        if (!fotoPerfilBase64.isNullOrEmpty()) {
            val bytes = Base64.decode(fotoPerfilBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            imagePerfil.setImageBitmap(bitmap)
        } else {
            imagePerfil.setImageResource(R.drawable.ic_account)
        }

        iconBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupRecyclerView()
        observarMensajes()

        uidReceptor?.let {
            chatViewModel.escucharMensajes(it)
            chatViewModel.marcarMensajesComoLeidos(it)
        }

        btnSend.setOnClickListener {
            val texto = editMensaje.text.toString().trim()
            if (texto.isNotEmpty() && uidReceptor != null) {
                chatViewModel.enviarMensaje(uidReceptor!!, texto)
                editMensaje.text.clear()
            }
        }

        recyclerMensajes.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        // Observar LiveData de rol admin
        homeViewModel.esAdmin.observe(this) { esAdmin ->
            soyAdmin = esAdmin
        }

        // Pedir rol del usuario actual
        homeViewModel.obtenerRolUsuarioActual()

        val iconOption: ImageView = findViewById(R.id.iconOption)
        iconOption.setOnClickListener { view ->
            uidReceptor?.let { uid ->

                val popupMenu = PopupMenu(this, view, 0, 0, R.style.CustomPopupMenu)

                // Inflar menú según rol ya obtenido en soyAdmin
                if (soyAdmin) {
                    popupMenu.menuInflater.inflate(R.menu.chat_menu, popupMenu.menu)
                } else {
                    popupMenu.menuInflater.inflate(R.menu.chat_menu_medico, popupMenu.menu)
                }

                // Aplicar estilo a los items del menú
                for (i in 0 until popupMenu.menu.size()) {
                    val menuItem = popupMenu.menu.getItem(i)
                    val spanString = android.text.SpannableString(menuItem.title)
                    spanString.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, spanString.length, 0)
                    spanString.setSpan(android.text.style.ForegroundColorSpan(android.graphics.Color.WHITE), 0, spanString.length, 0)
                    menuItem.title = spanString
                }

                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_ver_perfil -> {
                            val intent = Intent(this, PerfilActivity::class.java)
                            intent.putExtra("uid", uidReceptor)
                            startActivity(intent)
                            true
                        }
                        R.id.menu_llamar -> {
                            realizarLlamadaNormal(uid)
                            true
                        }
                        R.id.menu_llamar_emergencia -> {
                            realizarLlamadaEmergencia(uid)
                            true
                        }
                        else -> false
                    }
                }

                popupMenu.show()
            }
        }
    }

    private fun realizarLlamadaNormal(uid: String) {
        chatViewModel.obtenerNumeroDeUsuario(uid) { numero ->
            if (!numero.isNullOrEmpty()) {
                realizarLlamada(numero)
            } else {
                Toast.makeText(this, "Número no disponible", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun realizarLlamadaEmergencia(uid: String) {
        chatViewModel.obtenerNumeroEmergenciaDeUsuario(uid) { numero ->
            if (!numero.isNullOrEmpty()) {
                realizarLlamada(numero)
            } else {
                Toast.makeText(this, "Número no disponible", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun realizarLlamada(numero: String) {
        val numeroLimpio = numero.filter { it.isDigit() || it == '+' }
        if (numeroLimpio.isEmpty()) {
            Toast.makeText(this, "Número inválido para llamada", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$numeroLimpio"))
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error al iniciar llamada: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            numeroPendienteDeLlamar = numeroLimpio
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CALL_PHONE), 1001)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            numeroPendienteDeLlamar?.let { numero ->
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$numero"))
                startActivity(intent)
                numeroPendienteDeLlamar = null
            }
        } else {
            Toast.makeText(this, "Permiso para llamadas no concedido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        val fotoPerfilBase64 = intent.getStringExtra("fotoPerfilBase64") ?: ""
        chatAdapter = ChatAdapter(emptyList(), fotoPerfilBase64)
        recyclerMensajes.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun observarMensajes() {
        chatViewModel.mensajes.observe(this, Observer { lista ->
            progressBar.visibility = View.GONE
            recyclerMensajes.visibility = View.VISIBLE
            chatAdapter.actualizarMensajesConEncabezados(lista)
            recyclerMensajes.scrollToPosition(chatAdapter.itemCount - 1)
        })
    }
}
