package com.example.utpsalud.view.activity

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.utpsalud.R
import com.example.utpsalud.databinding.ActivityHistorialBinding
import com.example.utpsalud.utils.CsvUtils
import com.example.utpsalud.utils.PdfUtils
import com.example.utpsalud.viewmodel.HistorialViewModel
import com.example.utpsalud.view.adapter.MedicionAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.snackbar.Snackbar

class HistorialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorialBinding
    private val historialViewModel: HistorialViewModel by viewModels()
    private lateinit var adapter: MedicionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = MedicionAdapter(emptyList())
        binding.recyclerMedicionesDia.layoutManager = LinearLayoutManager(this)
        binding.recyclerMedicionesDia.adapter = adapter

        binding.iconBack.setOnClickListener {
            finish()
        }

        val fechaSeleccionada = intent.getLongExtra("fechaMedicion", -1L)
        if (fechaSeleccionada == -1L) {
            finish()
            return
        }

        val fechaFormateada = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
            .format(Date(fechaSeleccionada))
        binding.textFechaSeleccionada.text = fechaFormateada

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("usuarios")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    val nombre = document.getString("nombre") ?: ""
                    val apellido = document.getString("apellido") ?: ""
                    val dni = document.getString("dni") ?: ""
                    binding.textNombreUsuario.text = "$nombre $apellido"
                    binding.textNombreUsuario.tag = dni
                }
                .addOnFailureListener {
                    binding.textNombreUsuario.text = ""
                }
        }

        historialViewModel.mediciones.observe(this) { lista ->
            adapter.actualizarLista(lista)
        }

        historialViewModel.obtenerMedicionesPorDia(fechaSeleccionada)

        binding.btnDescargar.setOnClickListener {
            mostrarDialogoDescarga(fechaSeleccionada)
        }
    }

    private fun mostrarDialogoDescarga(fechaSeleccionada: Long) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_descargar, null)
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        val alertDialog = dialogBuilder.create()

        val btnPDF = dialogView.findViewById<Button>(R.id.btnPDF)
        val btnCSV = dialogView.findViewById<Button>(R.id.btnConfirmDelete)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelDelete)

        btnPDF.setOnClickListener {
            alertDialog.dismiss()
            generarPDFHistorial(fechaSeleccionada)
        }

        btnCSV.setOnClickListener {
            alertDialog.dismiss()
            generarCSVHistorial(fechaSeleccionada)
        }

        btnCancelar.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }

    private fun generarPDFHistorial(fechaSeleccionada: Long) {
        val listaMediciones = historialViewModel.mediciones.value ?: emptyList()
        if (listaMediciones.isEmpty()) {
            Snackbar.make(binding.root, "No hay mediciones para generar PDF", Snackbar.LENGTH_LONG).show()
            return
        }

        val nombreCompleto = binding.textNombreUsuario.text.toString()
        val dni = binding.textNombreUsuario.tag?.toString() ?: ""

        try {
            val archivoPdf = PdfUtils.generarPdfHistorial(
                context = this,
                listaMediciones = listaMediciones,
                fecha = fechaSeleccionada,
                nombreCompleto = nombreCompleto,
                dni = dni
            )

            Snackbar.make(binding.root, "PDF generado correctamente", Snackbar.LENGTH_LONG).show()

            abrirPdf(archivoPdf)

        } catch (e: Exception) {
            Snackbar.make(binding.root, "Error al generar PDF: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun abrirPdf(archivoPdf: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", archivoPdf)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
        }

        val chooser = Intent.createChooser(intent, "Abrir PDF con...")

        try {
            startActivity(chooser)
        } catch (e: Exception) {
            Snackbar.make(binding.root, "No se pudo abrir el PDF", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun generarCSVHistorial(fechaSeleccionada: Long) {
        val listaMediciones = historialViewModel.mediciones.value ?: emptyList()
        if (listaMediciones.isEmpty()) {
            Snackbar.make(binding.root, "No hay mediciones para generar CSV", Snackbar.LENGTH_LONG).show()
            return
        }

        val nombreCompleto = binding.textNombreUsuario.text.toString()
        val dni = binding.textNombreUsuario.tag?.toString() ?: ""

        try {
            val archivoCsv = CsvUtils.generarCsvHistorial(
                context = this,
                listaMediciones = listaMediciones,
                fecha = fechaSeleccionada,
                nombreCompleto = nombreCompleto,
                dni = dni
            )

            Snackbar.make(binding.root, "CSV generado correctamente", Snackbar.LENGTH_LONG).show()

            // Abrir CSV con aplicación externa
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", archivoCsv)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
            }

            val chooser = Intent.createChooser(intent, "Abrir CSV con...")
            startActivity(chooser)

        } catch (e: Exception) {
            Snackbar.make(binding.root, "Error al generar CSV: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

}
