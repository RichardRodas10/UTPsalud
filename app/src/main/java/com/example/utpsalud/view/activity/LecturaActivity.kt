package com.example.utpsalud.view.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.*
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.utpsalud.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.viewModels
import com.example.utpsalud.viewmodel.MedicionesViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class LecturaActivity : AppCompatActivity() {

    // TextView y otros elementos
    private lateinit var textTemp: TextView
    private lateinit var textPresencia: TextView
    private lateinit var textAccelX: TextView
    private lateinit var textAccelY: TextView
    private lateinit var textAccelZ: TextView
    private lateinit var textGyroX: TextView
    private lateinit var textGyroY: TextView
    private lateinit var textGyroZ: TextView
    private lateinit var textTiempo: TextView
    private lateinit var textPPG: TextView

    // BLE
    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var service: BluetoothGattService
    private var notifyIterator: Iterator<UUID>? = null

    // Temporizadores y estado
    private val handler = Handler(Looper.getMainLooper())
    private var tiempoRestante = 20
    private var enMedicion = false
    private var dialog: AlertDialog? = null
    private lateinit var dialogText: TextView

    private val temperaturas = mutableListOf<Float>()
    private var ultimoHR: Int = 0
    private var ultimoSpO2: Int = 0
    private var hrRecibido = false
    private var spo2Recibido = false
    private var finalDialog: AlertDialog? = null

    private val timeoutProcesamientoHandler = Handler(Looper.getMainLooper())
    private val procesamientoTimeoutRunnable = Runnable {
        mostrarDialogoErrorProcesamiento()
    }

    private var tiempoUltimoDato = System.currentTimeMillis()
    private val monitorInactividad = object : Runnable {
        override fun run() {
            if (enMedicion && System.currentTimeMillis() - tiempoUltimoDato > 3000) {
                detenerMedicionPorInactividad()
            } else {
                handler.postDelayed(this, 1000)
            }
        }
    }

    // UUIDs
    private val serviceUUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb")
    private val startCharUUID = UUID.fromString("f37ac038-0c83-4f93-9bfa-109ec42d1c35")
    private val tempCharUUID = UUID.fromString("00002A6E-0000-1000-8000-00805f9b34fb")
    private val presenceCharUUID = UUID.fromString("00002AE2-0000-1000-8000-00805f9b34fb")
    private val accCharUUID = UUID.fromString("ad0c0caa-3d8c-4b59-a091-b3389da3a0df")
    private val gyrCharUUID = UUID.fromString("5da304df-e10c-4270-92da-75487e95094a")
    private val hrCharUUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
    private val spo2CharUUID = UUID.fromString("8b6ff090-c1a1-42e9-a4b9-3eefc6530c4b")
    private val ppgCharUUID = UUID.fromString("d44f8caa-fffb-4eb1-b733-7e5511d4ef1c")

    private val medicionesViewModel: MedicionesViewModel by viewModels()

    // PPG chart
    private lateinit var chartPpg: LineChart
    private lateinit var ppgDataSet: LineDataSet
    private lateinit var lineData: LineData
    private var ppgEntryCount = 0

    private val notifyCharacteristics = listOf(
        startCharUUID, tempCharUUID, presenceCharUUID,
        accCharUUID, gyrCharUUID, hrCharUUID, spo2CharUUID, ppgCharUUID
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lectura)

        // Inicialización de vistas
        textTemp = findViewById(R.id.textMedTemperatura)
        textPresencia = findViewById(R.id.textMedPresencia)
        textAccelX = findViewById(R.id.textMedAcelerometroX)
        textAccelY = findViewById(R.id.textMedAcelerometroY)
        textAccelZ = findViewById(R.id.textMedAcelerometroZ)
        textGyroX = findViewById(R.id.textMedGiroscopioX)
        textGyroY = findViewById(R.id.textMedGiroscopioY)
        textGyroZ = findViewById(R.id.textMedGiroscopioZ)
        textTiempo = findViewById(R.id.textMedTiempo)


        // Configurar gráfico PPG
        chartPpg = findViewById(R.id.chartPpg)
        ppgDataSet = LineDataSet(mutableListOf(), "Señal PPG").apply {
            color = Color.RED
            setDrawCircles(false)
            lineWidth = 2f
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
        }

        lineData = LineData(ppgDataSet)
        chartPpg.data = lineData

        chartPpg.isAutoScaleMinMaxEnabled = true

        chartPpg.xAxis.setDrawLabels(false)
        chartPpg.xAxis.setDrawGridLines(false)
        chartPpg.axisLeft.setDrawLabels(false)
        chartPpg.axisRight.setDrawLabels(false)

        chartPpg.description.isEnabled = false
        chartPpg.legend.isEnabled = false
        chartPpg.setTouchEnabled(false)
        chartPpg.setScaleEnabled(false)
        chartPpg.invalidate()


        conectarGattDesdePreferencias()
        mostrarDialogoEspera()
    }

    private fun agregarDatoPPG(valor: Float) {
        val entry = Entry(ppgEntryCount.toFloat(), valor)
        ppgDataSet.addEntry(entry)
        ppgEntryCount++

        chartPpg.data.notifyDataChanged()
        chartPpg.notifyDataSetChanged()
        chartPpg.setVisibleXRangeMaximum(100f)
        chartPpg.moveViewToX(ppgEntryCount.toFloat())
        chartPpg.invalidate()
    }


    @SuppressLint("MissingPermission")
    private fun conectarGattDesdePreferencias() {
        val prefs = getSharedPreferences("BLE_PREFS", Context.MODE_PRIVATE)
        val deviceAddress = prefs.getString("device_address", null) ?: return
        val bluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (ActivityCompat.checkSelfPermission(this@LecturaActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            service = gatt.getService(serviceUUID) ?: return
            notifyIterator = notifyCharacteristics.iterator()
            suscribirseSiguiente()
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            suscribirseSiguiente()
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val uuid = characteristic.uuid
            val bytes = characteristic.value

            when (uuid) {
                startCharUUID -> {
                    if (!enMedicion && bytes.isNotEmpty() && bytes[0].toInt() == 1) {
                        enMedicion = true
                        iniciarConteoRegresivo()
                    }
                }

                tempCharUUID -> {
                    val temp = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).short / 100.0f
                    val tempAjustada = temp + 14.5f
                    temperaturas.add(tempAjustada)
                    actualizarTexto(textTemp, "%.2f °C".format(tempAjustada))
                    tiempoUltimoDato = System.currentTimeMillis()
                }

                accCharUUID -> {
                    val f = parse3Floats(bytes)
                    actualizarTexto(textAccelX, "X: %.2f".format(f[0]))
                    actualizarTexto(textAccelY, "Y: %.2f".format(f[1]))
                    actualizarTexto(textAccelZ, "Z: %.2f".format(f[2]))
                    tiempoUltimoDato = System.currentTimeMillis()
                }

                gyrCharUUID -> {
                    val f = parse3Floats(bytes)
                    actualizarTexto(textGyroX, "X: %.2f".format(f[0]))
                    actualizarTexto(textGyroY, "Y: %.2f".format(f[1]))
                    actualizarTexto(textGyroZ, "Z: %.2f".format(f[2]))
                    tiempoUltimoDato = System.currentTimeMillis()
                }

                presenceCharUUID -> {
                    val presente = bytes.isNotEmpty() && bytes[0].toInt() == 1
                    actualizarTexto(textPresencia, if (presente) "Sí" else "No")
                }

                hrCharUUID -> {
                    if (bytes.size >= 2) {
                        ultimoHR = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                        hrRecibido = true
                        verificarDatosFinales()
                    }
                }

                spo2CharUUID -> {
                    if (bytes.size >= 2) {
                        ultimoSpO2 = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                        spo2Recibido = true
                        verificarDatosFinales()
                    }
                }

                ppgCharUUID -> {
                    if (bytes.size >= 4) {
                        val valorOriginal = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int.toFloat()

                        runOnUiThread {
                            agregarDatoPPG(valorOriginal)
                        }
                    }
                }

            }
        }
    }

    private fun suscribirseSiguiente() {
        val iterator = notifyIterator ?: return
        if (!iterator.hasNext()) return
        val uuid = iterator.next()
        val ch = service.getCharacteristic(uuid) ?: return suscribirseSiguiente()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
        bluetoothGatt?.setCharacteristicNotification(ch, true)
        val descriptor = ch.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        bluetoothGatt?.writeDescriptor(descriptor)
    }

    private fun iniciarConteoRegresivo() {
        runOnUiThread {
            dialog?.let {
                var contador = 3
                val runnable = object : Runnable {
                    override fun run() {
                        if (contador > 0) {
                            dialogText.text = "Iniciando en $contador..."
                            contador--
                            handler.postDelayed(this, 1000)
                        } else {
                            dialog?.dismiss()
                            iniciarMedicion()
                        }
                    }
                }
                handler.post(runnable)
            }
        }
    }

    private fun iniciarMedicion() {
        temperaturas.clear()
        hrRecibido = false
        spo2Recibido = false
        tiempoRestante = 20
        tiempoUltimoDato = System.currentTimeMillis()
        handler.post(tickRunnable)
        handler.post(monitorInactividad)
    }

    private fun detenerMedicionPorInactividad() {
        enMedicion = false
        handler.removeCallbacks(tickRunnable)
        handler.removeCallbacks(monitorInactividad)
        runOnUiThread {
            dialog?.dismiss()
            finalDialog?.dismiss()
            mostrarDialogoInterrupcion()
        }
    }

    private fun mostrarDialogoInterrupcion() {
        AlertDialog.Builder(this)
            .setTitle("Medición interrumpida")
            .setMessage("No se detectaron datos. Vuelva a colocar la pulsera y presione el botón para comenzar.")
            .setCancelable(false)
            .setPositiveButton("Aceptar") { _, _ ->
                mostrarDialogoEspera()
            }
            .show()
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            textTiempo.text = "$tiempoRestante seg."
            if (tiempoRestante > 0) {
                tiempoRestante--
                handler.postDelayed(this, 1000)
            } else {
                enMedicion = false
                mostrarDialogoFinal()
            }
        }
    }

    private fun parse3Floats(bytes: ByteArray): FloatArray {
        val result = FloatArray(3)
        if (bytes.size == 12) {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            result[0] = buffer.getFloat(0)
            result[1] = buffer.getFloat(4)
            result[2] = buffer.getFloat(8)
        }
        return result
    }

    private fun mostrarDialogoEspera() {
        runOnUiThread {
            dialog?.dismiss()
            val textView = TextView(this).apply {
                text = "Pulse el botón para comenzar"
                textSize = 18f
                setPadding(40, 40, 40, 40)
                setTextColor(resources.getColor(android.R.color.black, theme))
            }

            val builder = AlertDialog.Builder(this)
                .setView(textView)
                .setCancelable(true)

            dialog = builder.create()
            dialog?.show()
            dialogText = textView
        }
    }

    private fun mostrarDialogoFinal() {
        runOnUiThread {
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(50, 50, 50, 50)
            }

            val progressBar = ProgressBar(this).apply { isIndeterminate = true }
            val textView = TextView(this).apply {
                text = "Procesando datos finales..."
                textSize = 18f
                setPadding(0, 20, 0, 0)
                gravity = Gravity.CENTER
            }

            layout.addView(progressBar)
            layout.addView(textView)

            val builder = AlertDialog.Builder(this)
                .setView(layout)
                .setCancelable(false)

            finalDialog = builder.create()
            finalDialog?.show()

            // Inicia temporizador de 15 segundos para control de timeout
            timeoutProcesamientoHandler.postDelayed(procesamientoTimeoutRunnable, 15000)
        }
    }

    private fun mostrarDialogoErrorProcesamiento() {
        finalDialog?.dismiss()
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("Error de procesamiento")
                .setMessage("Hubo un problema al procesar los datos. Por favor, repita la medición.")
                .setCancelable(false)
                .setPositiveButton("Reintentar") { _, _ ->
                    mostrarDialogoEspera()
                }
                .show()
        }
    }


    private fun verificarDatosFinales() {
        if (hrRecibido && spo2Recibido) {
            val tempPromedio = if (temperaturas.isNotEmpty()) temperaturas.average().toFloat() else 0f

            // Verificación de valores inválidos o fuera de rango
            val hrValido = ultimoHR in 40..200
            val spo2Valido = ultimoSpO2 in 70..100

            // 🔸 Detener el timeout si ya llegaron los datos
            timeoutProcesamientoHandler.removeCallbacks(procesamientoTimeoutRunnable)

            if (!hrValido || !spo2Valido) {
                Log.w("LecturaActivity", "Valores fuera de rango: HR=$ultimoHR, SpO2=$ultimoSpO2")
                runOnUiThread {
                    finalDialog?.dismiss()
                    AlertDialog.Builder(this)
                        .setTitle("Medición inválida")
                        .setMessage(
                            "Los valores recibidos de frecuencia cardíaca o saturación de oxígeno no son válidos.\n\n" +
                                    "Frecuencia: $ultimoHR bpm\n" +
                                    "SpO₂: $ultimoSpO2 %\n\n" +
                                    "Por favor, repita la medición."
                        )
                        .setCancelable(false)
                        .setPositiveButton("Aceptar") { _, _ ->
                            mostrarDialogoEspera()
                        }
                        .show()
                }
                return
            }

            val fechaHoraActual = Calendar.getInstance()
            val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())
            val fecha = formatoFecha.format(fechaHoraActual.time)
            val hora = formatoHora.format(fechaHoraActual.time)

            val prefs = getSharedPreferences("BLE_PREFS", Context.MODE_PRIVATE).edit()
            prefs.putString("ultima_fecha", "Fecha: $fecha")
            prefs.putString("ultima_hora", "Hora: $hora")
            prefs.apply()

            medicionesViewModel.subirMedicion(
                frecuencia = ultimoHR,
                spo2 = ultimoSpO2,
                temperatura = tempPromedio,
                fecha = fecha,
                hora = hora
            )

            finalDialog?.dismiss()
            val intent = Intent(this, HomeActivity::class.java).apply {
                putExtra("frecuencia_cardiaca", ultimoHR)
                putExtra("saturacion_oxigeno", ultimoSpO2)
                putExtra("temperatura_promedio", tempPromedio)
                putExtra("fecha_medicion", fecha)
                putExtra("hora_medicion", hora)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            finish()
        }
    }


    private fun actualizarTexto(textView: TextView, texto: String) {
        runOnUiThread { textView.text = texto }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
    }

    override fun onBackPressed() {
        if (enMedicion) {
            Toast.makeText(this, "Espere a que finalice la medición", Toast.LENGTH_SHORT).show()
        } else {
            dialog?.dismiss()
            finalDialog?.dismiss()
            super.onBackPressed()
        }
    }


}
