package com.example.utpsalud.view.activity

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
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
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(emptyList())
        recyclerMensajes.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun observarMensajes() {
        chatViewModel.mensajes.observe(this, Observer { lista ->
            chatAdapter.actualizarMensajesConEncabezados(lista)
            recyclerMensajes.scrollToPosition(chatAdapter.itemCount - 1)
        })
    }
}