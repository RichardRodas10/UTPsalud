package com.example.utpsalud

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.utpsalud.databinding.ActivityEditardatosBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditardatosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditardatosBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var originalCelular: String = ""
    private var originalCorreo: String = ""
    private var originalEmergencia: String = ""

    private var esAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditardatosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.iconInfo.setOnClickListener { finish() }
        binding.btnCancelar.setOnClickListener { finish() }
        binding.btnContinuar.setOnClickListener { actualizarDatosUsuario() }

        cargarDatosUsuario()
    }

    private fun cargarDatosUsuario() {
        val user = auth.currentUser ?: return

        val uid = user.uid
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    esAdmin = document.getBoolean("esAdministrador") ?: false

                    originalCelular = document.getString("celular") ?: ""
                    originalCorreo = document.getString("correo") ?: user.email ?: ""
                    originalEmergencia = document.getString("celularEmergencia") ?: ""

                    binding.editSugerencia.setText(originalCelular)
                    binding.editCorreo.setText(originalCorreo)

                    if (esAdmin) {
                        binding.textEmergencia.visibility = View.GONE
                        binding.contenedorEmergencia.visibility = View.GONE
                    } else {
                        binding.textEmergencia.visibility = View.VISIBLE
                        binding.contenedorEmergencia.visibility = View.VISIBLE
                        binding.editNumeroEmergencia.setText(originalEmergencia)
                    }

                    configurarTextWatchers()
                    ocultarBotones()
                } else {
                    Toast.makeText(this, "Datos no encontrados", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar los datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarDatosUsuario() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val nuevoCelular = binding.editSugerencia.text.toString().trim()
        val nuevoCorreo = binding.editCorreo.text.toString().trim()
        val nuevoEmergencia = binding.editNumeroEmergencia.text.toString().trim()

        // Validaciones comunes
        if (nuevoCelular.length != 9 || !nuevoCelular.startsWith("9")) {
            Toast.makeText(this, "El número de celular debe tener 9 dígitos y empezar con 9", Toast.LENGTH_SHORT).show()
            return
        }

        if (!esAdmin) {
            if (nuevoEmergencia.length != 9 || !nuevoEmergencia.startsWith("9")) {
                Toast.makeText(this, "El número de emergencia debe tener 9 dígitos y empezar con 9", Toast.LENGTH_SHORT).show()
                return
            }

            if (nuevoEmergencia == nuevoCelular) {
                Toast.makeText(this, "El contacto de emergencia debe ser distinto al celular personal", Toast.LENGTH_SHORT).show()
                return
            }
        }

        if (nuevoCorreo.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(nuevoCorreo).matches()) {
            Toast.makeText(this, "Correo inválido", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("usuarios")
            .whereEqualTo("celular", nuevoCelular)
            .get()
            .addOnSuccessListener { documents ->
                val celularUsadoPorOtro = documents.any { it.id != user.uid }

                if (celularUsadoPorOtro) {
                    Toast.makeText(this, "Este número de celular ya está en uso por otro usuario", Toast.LENGTH_LONG).show()
                } else {
                    val datosActualizados = if (esAdmin) {
                        mapOf(
                            "celular" to nuevoCelular,
                            "correo" to nuevoCorreo
                        )
                    } else {
                        mapOf(
                            "celular" to nuevoCelular,
                            "correo" to nuevoCorreo,
                            "celularEmergencia" to nuevoEmergencia
                        )
                    }

                    db.collection("usuarios").document(user.uid)
                        .update(datosActualizados)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al actualizar los datos", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al verificar el número de celular", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configurarTextWatchers() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val celularActual = binding.editSugerencia.text.toString().trim()
                val correoActual = binding.editCorreo.text.toString().trim()
                val emergenciaActual = if (esAdmin) "" else binding.editNumeroEmergencia.text.toString().trim()

                val huboCambios = celularActual != originalCelular ||
                        correoActual != originalCorreo ||
                        emergenciaActual != originalEmergencia

                if (huboCambios) {
                    binding.btnContinuar.visibility = View.VISIBLE
                    binding.btnCancelar.visibility = View.VISIBLE
                } else {
                    ocultarBotones()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        binding.editSugerencia.addTextChangedListener(watcher)
        binding.editCorreo.addTextChangedListener(watcher)
        if (!esAdmin) {
            binding.editNumeroEmergencia.addTextChangedListener(watcher)
        }
    }

    private fun ocultarBotones() {
        binding.btnContinuar.visibility = View.GONE
        binding.btnCancelar.visibility = View.GONE
    }
}
