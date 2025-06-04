package com.example.utpsalud.ui.perfil

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.utpsalud.LoginActivity
import com.example.utpsalud.R
import com.example.utpsalud.databinding.FragmentPerfilBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

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
    }

    private fun configurarContadorSugerencia() {
        binding.editSugerencia.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val cantidad = s?.length ?: 0
                binding.txtContadorSugerencia.text = "$cantidad/$LIMITE_CARACTERES"

                val color = if (cantidad == LIMITE_CARACTERES) {
                    R.color.red // Puedes cambiar este color según tu diseño
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

                val nombre = document.getString("nombre") ?: "Nombre no disponible"
                val apellido = document.getString("apellido") ?: "Apellido no disponible"
                val correo = document.getString("correo") ?: user.email ?: "Correo no disponible"
                val dni = document.getString("dni") ?: "DNI no disponible"
                val celular = document.getString("celular") ?: "Celular no disponible"

                binding.textName.text = nombre
                binding.textApe.text = apellido
                binding.textEmail.text = correo
                binding.textDni.text = dni
                binding.textCelular.text = celular
            } else {
                binding.profileImage.setImageResource(R.drawable.ic_account)
                binding.textName.text = "Nombre no disponible"
                binding.textApe.text = "Apellido no disponible"
                binding.textEmail.text = user.email ?: "Correo no disponible"
                binding.textDni.text = "Dni no disponible"
                binding.textCelular.text = "Celular no disponible"
            }
        }.addOnFailureListener {
            binding.profileImage.setImageResource(R.drawable.ic_account)
            binding.textName.text = "Error al cargar"
            binding.textApe.text = "Error al cargar"
            binding.textEmail.text = user.email ?: "Correo no disponible"
            binding.textDni.text = "Error al cargar"
            binding.textCelular.text = "Error al cargar"
        }
    }

    private fun showLogoutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_logout, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmLogout)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelLogout)

        btnConfirm.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            dialog.dismiss()
            requireActivity().finish()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
