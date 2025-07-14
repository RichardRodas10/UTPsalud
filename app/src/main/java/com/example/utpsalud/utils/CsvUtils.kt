package com.example.utpsalud.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.example.utpsalud.model.MedicionManual
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

object CsvUtils {

    fun generarCsvHistorial(
        context: Context,
        listaMediciones: List<MedicionManual>,
        fecha: Long,
        nombreCompleto: String,
        dni: String
    ): File {
        val nombreArchivo = "Historial_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"

        val directorioDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!directorioDownloads.exists()) {
            directorioDownloads.mkdirs()
        }

        val archivo = File(directorioDownloads, nombreArchivo)

        val formatoFecha = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "PE"))
        val formatoHora = SimpleDateFormat("hh:mm a", Locale("es", "PE"))
        val fechaFormateada = formatoFecha.format(Date(fecha))

        try {
            FileOutputStream(archivo).use { fos ->
                fos.write("Historial de Mediciones\n".toByteArray(StandardCharsets.UTF_8))
                fos.write("Nombres:,$nombreCompleto\n".toByteArray(StandardCharsets.UTF_8))
                fos.write("DNI:,$dni\n".toByteArray(StandardCharsets.UTF_8))
                fos.write("Fecha:,$fechaFormateada\n\n".toByteArray(StandardCharsets.UTF_8))

                // Encabezados de las columnas
                fos.write("Hora,Frec. cardíaca,Oxígeno en sangre,Temperatura (°C),Estado de salud\n".toByteArray(StandardCharsets.UTF_8))

                // Filas de datos
                listaMediciones.forEach { medicion ->
                    val fila = listOf(
                        formatoHora.format(Date(medicion.fechaMedicion)),
                        medicion.frecuenciaCardiaca.toString(),
                        "${medicion.oxigenoSangre}%",
                        "%.1f".format(medicion.temperatura),
                        medicion.estadoSalud
                    ).joinToString(",")
                    fos.write((fila + "\n").toByteArray(StandardCharsets.UTF_8))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        MediaScannerConnection.scanFile(
            context,
            arrayOf(archivo.absolutePath),
            arrayOf("text/csv"),
            null
        )

        return archivo
    }
}