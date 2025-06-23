package com.example.utpsalud.ui.perfil

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.utpsalud.EditardatosActivity
import com.example.utpsalud.HomeActivity
import com.example.utpsalud.LoginActivity
import com.example.utpsalud.R
import com.example.utpsalud.databinding.FragmentPerfilBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val IMAGE_PICK_CODE = 1000

    companion object {
        private const val LIMITE_CARACTERES = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserInfo()
        configurarContadorSugerencia()

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        binding.cameraIconPerfil.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }

        binding.textEditar.setOnClickListener {
            startActivity(Intent(requireContext(), EditardatosActivity::class.java))
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            val bitmap = imageUri?.let { decodeBitmapFromUri(it) }

            bitmap?.let {
                binding.profileImage.setImageBitmap(it)
                val stream = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                val imageBytes = stream.toByteArray()
                val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)
                actualizarFotoEnFirestore(base64Image)
            }
        }
    }

    private fun decodeBitmapFromUri(uri: Uri): Bitmap? {
        val inputStream = requireActivity().contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        // Escala la imagen para evitar distorsión (ajustar tamaño si lo deseas)
        val width = originalBitmap.width
        val height = originalBitmap.height
        val newSize = 512 // Tamaño recomendado

        val scaledBitmap = if (width > height) {
            val newHeight = (height.toFloat() / width.toFloat() * newSize).toInt()
            Bitmap.createScaledBitmap(originalBitmap, newSize, newHeight, true)
        } else {
            val newWidth = (width.toFloat() / height.toFloat() * newSize).toInt()
            Bitmap.createScaledBitmap(originalBitmap, newWidth, newSize, true)
        }

        return scaledBitmap
    }

    private fun actualizarFotoEnFirestore(base64Image: String) {
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("usuarios").document(uid)
            .update("fotoPerfilBase64", base64Image)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Foto actualizada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al actualizar la foto", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configurarContadorSugerencia() {
        binding.editSugerencia.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val cantidad = s?.length ?: 0
                binding.txtContadorSugerencia.text = "$cantidad/$LIMITE_CARACTERES"

                val color = if (cantidad == LIMITE_CARACTERES) {
                    R.color.red
                } else {
                    R.color.gray_suave
                }

                binding.txtContadorSugerencia.setTextColor(
                    ContextCompat.getColor(requireContext(), color)
                )
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadUserInfo() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("usuarios").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val fotoBase64 = document.getString("fotoPerfilBase64")
                if (!fotoBase64.isNullOrEmpty()) {
                    val decodedBytes = Base64.decode(fotoBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    binding.profileImage.setImageBitmap(bitmap)
                } else {
                    binding.profileImage.setImageResource(R.drawable.ic_account)
                }

                binding.textName.text = document.getString("nombre") ?: "Nombre no disponible"
                binding.textApe.text = document.getString("apellido") ?: "Apellido no disponible"
                binding.textEmail.text = document.getString("correo") ?: user.email ?: "Correo no disponible"
                binding.textDni.text = document.getString("dni") ?: "DNI no disponible"
                binding.textCelular.text = document.getString("celular") ?: "Celular no disponible"
                binding.textCelularEmergencia.text = document.getString("celularEmergencia") ?: "Contacto no disponible"
            } else {
                mostrarCamposVacios(user.email)
            }
        }.addOnFailureListener {
            mostrarCamposVacios(user.email, esError = true)
        }
    }

    private fun mostrarCamposVacios(email: String?, esError: Boolean = false) {
        binding.profileImage.setImageResource(R.drawable.ic_account)
        binding.textName.text = if (esError) "Error al cargar" else "Nombre no disponible"
        binding.textApe.text = if (esError) "Error al cargar" else "Apellido no disponible"
        binding.textEmail.text = email ?: "Correo no disponible"
        binding.textDni.text = if (esError) "Error al cargar" else "DNI no disponible"
        binding.textCelular.text = if (esError) "Error al cargar" else "Celular no disponible"
        binding.textCelularEmergencia.text = if (esError) "Error al cargar" else "Contacto no disponible"
    }

    private fun showLogoutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_logout, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.btnConfirmLogout).setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            dialog.dismiss()
            requireActivity().finish()
        }

        dialogView.findViewById<Button>(R.id.btnCancelLogout).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //Actualiza los datos al volver de otro layout (después de editarlos)
    override fun onResume() {
        super.onResume()
        loadUserInfo()
    }
}
