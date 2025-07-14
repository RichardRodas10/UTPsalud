package com.example.utpsalud.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.os.Environment
import com.example.utpsalud.R
import com.example.utpsalud.model.MedicionManual
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfUtils {

    fun generarPdfHistorial(
        context: Context,
        listaMediciones: List<MedicionManual>,
        fecha: Long,
        nombreCompleto: String,
        dni: String
    ): File {
        // 1. Crear nombre del archivo PDF
        val nombreArchivo = "Historial_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"

        // 2. Ruta: carpeta Downloads pública
        val directorioDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!directorioDownloads.exists()) {
            directorioDownloads.mkdirs()
        }

        // 3. Archivo destino
        val archivo = File(directorioDownloads, nombreArchivo)

        // 4. Crear el PDF
        crearPdfHistorial(
            context = context,
            nombreCompleto = nombreCompleto,
            dni = dni,
            fechaMediciones = fecha,
            mediciones = listaMediciones,
            outputFile = archivo
        )

        return archivo
    }

    private fun crearPdfHistorial(
        context: Context,
        nombreCompleto: String,
        dni: String,
        fechaMediciones: Long,
        mediciones: List<MedicionManual>,
        outputFile: File
    ) {
        val pdfDocument = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842

        val formatoFecha = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "PE"))
        val formatoHora = SimpleDateFormat("hh:mm a", Locale("es", "PE"))

        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
            color = Color.BLACK
            isAntiAlias = true
        }

        val smallPaint = Paint().apply {
            textSize = 12f
            color = Color.BLACK
            isAntiAlias = true
        }

        val boldSmallPaint = Paint().apply {
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.BLACK
            isAntiAlias = true
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val bitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.utp_salud)
        val logoMaxWidth = 80
        val scale = logoMaxWidth.toFloat() / bitmap.width.toFloat()
        val logoHeight = (bitmap.height * scale).toInt()
        val logo = Bitmap.createScaledBitmap(bitmap, logoMaxWidth, logoHeight, true)
        canvas.drawBitmap(logo, 20f, 20f, null)

        // Posicionar el texto alineado verticalmente con el centro del logo
        val logoTop = 20f
        val logoCenterY = logoTop + logoHeight / 2f
        val titleOffsetY = (titlePaint.descent() + titlePaint.ascent()) / 2
        canvas.drawText(
            "UNIVERSIDAD TECNOLÓGICA DEL PERÚ S.A.C.",
            130f,
            logoCenterY - titleOffsetY,
            titlePaint
        )

        canvas.drawText("Nombres: $nombreCompleto", 20f, 110f, smallPaint)
        canvas.drawText("DNI: $dni", 20f, 130f, smallPaint)
        canvas.drawText("Fecha: ${formatoFecha.format(Date(fechaMediciones))}", 20f, 150f, smallPaint)

        val headers = listOf("Hora", "Frec. cardíaca", "Oxígeno en sangre", "Temperatura (°C)", "Estado de salud")
        val columnPositions = listOf(20f, 120f, 240f, 380f, 480f)

        for ((index, header) in headers.withIndex()) {
            canvas.drawText(header, columnPositions[index], 190f, boldSmallPaint)
        }

        val linePaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 1f
        }
        canvas.drawLine(20f, 195f, pageWidth - 20f, 195f, linePaint)

        var startY = 220f
        val rowHeight = 30f

        for ((index, medicion) in mediciones.withIndex()) {
            if (startY + rowHeight > pageHeight - 40) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                canvas.drawBitmap(logo, 20f, 20f, null)
                canvas.drawText("UNIVERSIDAD TECNOLOGICA DEL PERÚ S.A.C.", 130f, 60f, titlePaint)
                canvas.drawText("Nombres:", 20f, 110f, boldSmallPaint)
                canvas.drawText(nombreCompleto, 90f, 110f, smallPaint)

                canvas.drawText("DNI:", 20f, 130f, boldSmallPaint)
                canvas.drawText(dni, 60f, 130f, smallPaint)

                canvas.drawText("Fecha:", 20f, 150f, boldSmallPaint)
                canvas.drawText(formatoFecha.format(Date(fechaMediciones)), 70f, 150f, smallPaint)

                for ((idx, header) in headers.withIndex()) {
                    canvas.drawText(header, columnPositions[idx], 190f, boldSmallPaint)
                }
                canvas.drawLine(20f, 195f, pageWidth - 20f, 195f, linePaint)

                startY = 220f
            }

            canvas.drawText(formatoHora.format(Date(medicion.fechaMedicion)), columnPositions[0], startY, smallPaint)
            canvas.drawText("${medicion.frecuenciaCardiaca}", columnPositions[1], startY, smallPaint)
            canvas.drawText("${medicion.oxigenoSangre}%", columnPositions[2], startY, smallPaint)
            canvas.drawText("%.1f".format(medicion.temperatura), columnPositions[3], startY, smallPaint)
            canvas.drawText(medicion.estadoSalud, columnPositions[4], startY, smallPaint)

            startY += rowHeight
        }

        pdfDocument.finishPage(page)

        try {
            val fos = FileOutputStream(outputFile)
            pdfDocument.writeTo(fos)
            fos.flush()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        pdfDocument.close()

        MediaScannerConnection.scanFile(
            context,
            arrayOf(outputFile.absolutePath),
            arrayOf("application/pdf"),
            null
        )
    }
}