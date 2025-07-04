package com.example.utpsalud.view.activity

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.utpsalud.R
import de.hdodenhof.circleimageview.CircleImageView

class ChatActivity : AppCompatActivity() {

    private lateinit var iconBack: ImageView
    private lateinit var imagePerfil: CircleImageView
    private lateinit var textNombre: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        iconBack = findViewById(R.id.iconBack)
        imagePerfil = findViewById(R.id.profileImageContacto)
        textNombre = findViewById(R.id.textNombreContacto)

        val uid = intent.getStringExtra("uid")
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

        // Aquí podrás luego cargar los mensajes con el uid recibido
    }
}