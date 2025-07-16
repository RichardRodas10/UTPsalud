package com.example.utpsalud.view.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.utpsalud.R
import com.example.utpsalud.databinding.FragmentPerfilBinding
import com.example.utpsalud.view.activity.SplashActivity
import com.example.utpsalud.viewmodel.PerfilFragmentViewModel
import java.io.ByteArrayOutputStream
import android.util.Base64
import com.example.utpsalud.view.activity.EditardatosActivity
import com.example.utpsalud.view.activity.LoadingActivity
import com.example.utpsalud.view.activity.LoginActivity
import com.google.android.material.snackbar.Snackbar

class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PerfilFragmentViewModel by viewModels()

    private val IMAGE_PICK_CODE = 1000
    private val LIMITE_CARACTERES = 100

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarContadorSugerencia()
        observarViewModel()

        viewModel.cargarDatosUsuario()

        binding.btnLogout.setOnClickListener { showLogoutDialog() }

        binding.textEditar.setOnClickListener {
            startActivity(Intent(requireContext(), EditardatosActivity::class.java))
        }

        binding.cameraIconPerfil.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }

        binding.btnDelete.setOnClickListener {
            mostrarDialogEliminar()
        }

        binding.btnEnviar.setOnClickListener {
            val sugerencia = binding.editSugerencia.text.toString().trim()

            if (sugerencia.isNotEmpty()) {
                // Desactiva el botón para evitar múltiples envíos
                binding.btnEnviar.isEnabled = false
                binding.btnEnviar.text = "Enviando..."

                viewModel.enviarSugerencia(sugerencia) { exito ->
                    // Reactiva el botón
                    binding.btnEnviar.isEnabled = true
                    binding.btnEnviar.text = "Enviar"

                    if (exito) {
                        binding.editSugerencia.text.clear()
                        Snackbar.make(binding.root, "¡Gracias por tu sugerencia!", Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(binding.root, "Error al enviar la sugerencia", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } else {
                Snackbar.make(binding.root, "Escribe una sugerencia primero", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarDatosUsuario()
    }

    private fun observarViewModel() {
        viewModel.userInfo.observe(viewLifecycleOwner) { user ->
            binding.textName.text = user.nombre
            binding.textApe.text = user.apellido
            binding.textEmail.text = user.correo
            binding.textDni.text = user.dni
            binding.textCelular.text = user.celular
            binding.textCelularEmergencia.text = user.celularEmergencia

            if (!user.fotoBase64.isNullOrEmpty()) {
                val decodedBytes = Base64.decode(user.fotoBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                binding.profileImage.setImageBitmap(bitmap)
            } else {
                binding.profileImage.setImageResource(R.drawable.ic_account)
            }

            if (user.esAdmin) {
                binding.textContactoEmergencia.visibility = View.GONE
                binding.contenedorContactoEm.visibility = View.GONE
            } else {
                binding.textContactoEmergencia.visibility = View.VISIBLE
                binding.contenedorContactoEm.visibility = View.VISIBLE
            }
        }

        viewModel.fotoActualizada.observe(viewLifecycleOwner) { fueExitosa ->
            if (fueExitosa) {
                Snackbar.make(binding.root, "Foto actualizada", Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, "Error al actualizar la foto", Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.eliminacionEstado.observe(viewLifecycleOwner) { estado ->
            when (estado) {
                is PerfilFragmentViewModel.EliminacionEstado.Cargando -> {
                    startActivity(Intent(requireContext(), LoadingActivity::class.java))
                }
                is PerfilFragmentViewModel.EliminacionEstado.Exito -> {
                    val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                        putExtra("cuenta_eliminada", true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                }
                is PerfilFragmentViewModel.EliminacionEstado.Error -> {
                    Snackbar.make(binding.root, estado.mensaje, Snackbar.LENGTH_LONG).show()
                }
            }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            val bitmap = imageUri?.let { decodeBitmapFromUri(it) }
            bitmap?.let {
                binding.profileImage.setImageBitmap(it)
                viewModel.actualizarFoto(it)
            }
        }
    }

    private fun decodeBitmapFromUri(uri: Uri): Bitmap? {
        val inputStream = requireActivity().contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        val width = originalBitmap.width
        val height = originalBitmap.height
        val newSize = 512

        return if (width > height) {
            val newHeight = (height.toFloat() / width.toFloat() * newSize).toInt()
            Bitmap.createScaledBitmap(originalBitmap, newSize, newHeight, true)
        } else {
            val newWidth = (width.toFloat() / height.toFloat() * newSize).toInt()
            Bitmap.createScaledBitmap(originalBitmap, newWidth, newSize, true)
        }
    }

    private fun showLogoutDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_logout, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.btnConfirmLogout).setOnClickListener {
            viewModel.signOut()
            startActivity(Intent(requireContext(), SplashActivity::class.java).apply {
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

    private fun mostrarDialogEliminar() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_eliminar, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.btnConfirmDesactivar).setOnClickListener {
            viewModel.desactivarCuenta()
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                putExtra("cuenta_desactivada", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            dialog.dismiss()
            requireActivity().finish()
        }

        dialogView.findViewById<Button>(R.id.btnConfirmDelete).setOnClickListener {
            viewModel.eliminarCuenta()
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnCancelDelete).setOnClickListener {
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