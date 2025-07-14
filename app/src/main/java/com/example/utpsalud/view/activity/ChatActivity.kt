package com.example.utpsalud.view.activity

import android.content.Intent
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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.utpsalud.R
import com.example.utpsalud.model.ChatMessage
import com.example.utpsalud.view.adapter.ChatAdapter
import com.example.utpsalud.viewmodel.ChatViewModel
import de.hdodenhof.circleimageview.CircleImageView

class ChatActivity : AppCompatActivity() {

    private lateinit var iconBack: ImageView
    private lateinit var imagePerfil: CircleImageView
    private lateinit var textNombre: TextView
    private lateinit var recyclerMensajes: RecyclerView
    private lateinit var editMensaje: EditText
    private lateinit var btnSend: ImageButton

    private lateinit var chatAdapter: ChatAdapter

    private lateinit var progressBar: ProgressBar

    private val chatViewModel: ChatViewModel by viewModels()

    private var uidReceptor: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // UI
        iconBack = findViewById(R.id.iconBack)
        imagePerfil = findViewById(R.id.profileImageContacto)
        textNombre = findViewById(R.id.textNombreContacto)
        recyclerMensajes = findViewById(R.id.recyclerMensajes)
        editMensaje = findViewById(R.id.editMensaje)
        btnSend = findViewById(R.id.btnSend)

        // Datos del intent
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
            chatViewModel.marcarMensajesComoLeidos(uidReceptor!!)
        }

        btnSend.setOnClickListener {
            val texto = editMensaje.text.toString().trim()
            if (texto.isNotEmpty() && uidReceptor != null) {
                chatViewModel.enviarMensaje(uidReceptor!!, texto)
                editMensaje.text.clear()
            }
        }

        progressBar = findViewById(R.id.progressBar)
        recyclerMensajes.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        val iconOption: ImageView = findViewById(R.id.iconOption)

        iconOption.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view, 0, 0, R.style.CustomPopupMenu)
            popupMenu.menuInflater.inflate(R.menu.chat_menu, popupMenu.menu)

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
                        uidReceptor?.let { uid ->
                            chatViewModel.obtenerNumeroDeUsuario(uid) { numero ->
                                if (!numero.isNullOrEmpty()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero"))
                                    startActivity(intent)
                                } else {
                                    android.widget.Toast.makeText(this, "Número no disponible", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }
    }

    private fun setupRecyclerView() {
        // Obten la cadena Base64 o la URL de la imagen (en este caso es Base64)
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