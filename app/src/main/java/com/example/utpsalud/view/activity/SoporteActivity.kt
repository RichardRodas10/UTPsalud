package com.example.utpsalud.view.activity

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.utpsalud.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SoporteActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var editMensaje: EditText
    private lateinit var editCorreo: EditText
    private lateinit var btnEnviar: Button
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_soporte)

        // Inicializar Firebase Firestore
        db = FirebaseFirestore.getInstance()

        // Referencias UI
        btnBack = findViewById(R.id.btnBack)
        editMensaje = findViewById(R.id.editMensaje)
        editCorreo = findViewById(R.id.editCorreo)
        btnEnviar = findViewById(R.id.btnEnviar)

        // Volver al Login
        btnBack.setOnClickListener {
            finish() // Cierra esta actividad y vuelve a LoginActivity
        }

        btnEnviar.setOnClickListener { view ->
            ocultarTeclado(view)

            val mensaje = editMensaje.text.toString().trim()
            val correo = editCorreo.text.toString().trim()

            if (correo.isEmpty()) {
                Snackbar.make(view, "Ingresa un correo para soporte", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (mensaje.isEmpty()) {
                Snackbar.make(view, "Escribe un mensaje antes de enviar", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Verificar si el correo existe en Firestore (en colección "usuarios", por ejemplo)
            db.collection("usuarios")
                .whereEqualTo("email", correo)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        // No existe el correo, no enviar mensaje
                        Snackbar.make(view, "El correo no existe en el sistema", Snackbar.LENGTH_LONG).show()
                    } else {
                        // El correo existe, obtener uid del documento del usuario encontrado
                        val userDoc = querySnapshot.documents[0]
                        val uid = userDoc.id  // Asumo que el id del documento es el uid del usuario

                        // Preparar datos para guardar en Firestore
                        val soporteData = hashMapOf(
                            "uid" to uid,
                            "correo" to correo,
                            "mensaje" to mensaje,
                            "timestamp" to System.currentTimeMillis()
                        )
                        db.collection("soporte")
                            .add(soporteData)
                            .addOnSuccessListener {
                                Snackbar.make(view, "Mensaje enviado correctamente", Snackbar.LENGTH_LONG).show()
                                editMensaje.text.clear()
                                editCorreo.text.clear()
                            }
                            .addOnFailureListener { e ->
                                Snackbar.make(view, "Error al enviar mensaje: ${e.message}", Snackbar.LENGTH_LONG).show()
                            }
                    }
                }

                .addOnFailureListener { e ->
                    Snackbar.make(view, "Error al verificar correo: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
        }
    }

    private fun ocultarTeclado(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}