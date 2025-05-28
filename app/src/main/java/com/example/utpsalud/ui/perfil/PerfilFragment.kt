package com.example.utpsalud.ui.perfil

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

        loadUserPhotoFromBase64()

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun loadUserPhotoFromBase64() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        val db = FirebaseFirestore.getInstance()
        val userDocRef = db.collection("usuarios").document(userId)  // Ajusta el nombre de colección

        userDocRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val fotoBase64 = document.getString("fotoPerfilBase64")
                if (!fotoBase64.isNullOrEmpty()) {
                    // Decodificar Base64 y convertir a Bitmap
                    val decodedBytes = Base64.decode(fotoBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    binding.profileImage.setImageBitmap(bitmap)
                } else {
                    // Si no hay foto, poner imagen por defecto
                    binding.profileImage.setImageResource(R.drawable.ic_account)
                }
            } else {
                binding.profileImage.setImageResource(R.drawable.ic_account)
            }
        }.addOnFailureListener {
            // En caso de error, imagen por defecto
            binding.profileImage.setImageResource(R.drawable.ic_account)
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
