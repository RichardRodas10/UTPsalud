package com.example.utpsalud.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _esAdmin = MutableLiveData<Boolean>()
    val esAdmin: LiveData<Boolean> = _esAdmin

    fun obtenerRolUsuarioActual() {
        val uid = auth.currentUser?.uid ?: run {
            _esAdmin.value = false
            return
        }
        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val admin = doc.getBoolean("esAdministrador") ?: false
                _esAdmin.value = admin
            }
            .addOnFailureListener {
                _esAdmin.value = false
            }
    }
}