package com.example.utpsalud.view.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.utpsalud.R
import com.example.utpsalud.databinding.ActivityBluetoothBinding
import java.util.*

class BluetoothActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBluetoothBinding
    private val dataList = mutableListOf<HashMap<String, String>>()
    private val devicesList = mutableListOf<BluetoothDevice>()
    private lateinit var adapter: SimpleAdapter
    private lateinit var scanCallback: ScanCallback
    private var selectedDevice: BluetoothDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager.adapter

        val prefs = getSharedPreferences("BLE_PREFS", Context.MODE_PRIVATE)
        val savedName = prefs.getString("device_name", null)
        val savedAddress = prefs.getString("device_address", null)
        val fueVinculadoDesdeBluetoothActivity = prefs.getBoolean("vinculado_por_bluetoothactivity", false)

        // ✅ Solo salta si fue vinculado anteriormente desde esta misma pantalla
        if (savedName == "UTP+ Salud - Brazalete" && savedAddress != null && fueVinculadoDesdeBluetoothActivity) {
            val intent = Intent(this, LecturaActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Switch
        binding.switchBluetooth.isChecked = bluetoothAdapter?.isEnabled == true

        // Volver a Home
        binding.textVolver.setOnClickListener {
            finish()
        }

        // Iniciar conexión
        binding.textIniciar.setOnClickListener {
            val deviceName = binding.textDispositivoConectado.text.toString()
            if (deviceName.contains("UTP+ Salud - Brazalete")) {
                conectarDispositivo(selectedDevice!!)
            } else {
                Toast.makeText(this, "Selecciona 'UTP+ Salud - Brazalete' para conectar", Toast.LENGTH_SHORT).show()
            }
        }

        // Configura la lista
        adapter = SimpleAdapter(
            this,
            dataList,
            android.R.layout.simple_list_item_2,
            arrayOf("name", "address"),
            intArrayOf(android.R.id.text1, android.R.id.text2)
        )
        binding.btList.adapter = adapter

        // Selección de dispositivos
        binding.btList.setOnItemClickListener { _, _, position, _ ->
            selectedDevice = devicesList[position]
            binding.textDispositivoConectado.text = "Seleccionado: ${selectedDevice?.name ?: "Sin nombre"}"
        }

        // Switch Bluetooth
        binding.switchBluetooth.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                solicitarPermisosBluetooth()
            } else {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) return@setOnCheckedChangeListener

                bluetoothAdapter?.disable()
                binding.btList.visibility = View.GONE
                Toast.makeText(this, "Bluetooth desactivado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun solicitarPermisosBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permisosLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permisosLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private val permisosLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            habilitarBluetooth()
        } else {
            Toast.makeText(this, "Permiso requerido para escanear BLE", Toast.LENGTH_SHORT).show()
        }
    }

    private val btEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            escanearBLE()
        } else {
            Toast.makeText(this, "Bluetooth no activado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun habilitarBluetooth() {
        val bluetoothAdapter = getSystemService(BluetoothManager::class.java).adapter
        if (bluetoothAdapter?.isEnabled == false) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            btEnableLauncher.launch(intent)
        } else {
            escanearBLE()
        }
    }

    @SuppressLint("MissingPermission")
    private fun escanearBLE() {
        val bluetoothAdapter = getSystemService(BluetoothManager::class.java).adapter
        val scanner = bluetoothAdapter?.bluetoothLeScanner

        dataList.clear()
        devicesList.clear()
        adapter.notifyDataSetChanged()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = device.name ?: "Sin nombre"

                if (!devicesList.any { it.address == device.address }) {
                    devicesList.add(device)
                    dataList.add(hashMapOf("name" to name, "address" to device.address))
                    adapter.notifyDataSetChanged()
                    Log.d("BLE", "Dispositivo encontrado: $name (${device.address})")
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Toast.makeText(this@BluetoothActivity, "Error al escanear: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }

        scanner?.startScan(scanCallback)
        binding.btList.visibility = View.VISIBLE
    }

    @SuppressLint("MissingPermission")
    private fun conectarDispositivo(device: BluetoothDevice) {
        val bluetoothAdapter = getSystemService(BluetoothManager::class.java).adapter
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        scanner?.stopScan(scanCallback) // ✅ Detener escaneo al conectar

        device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    runOnUiThread {
                        Toast.makeText(this@BluetoothActivity, "Conectado con ${device.name}", Toast.LENGTH_SHORT).show()

                        val prefs = getSharedPreferences("BLE_PREFS", MODE_PRIVATE).edit()
                        prefs.putString("device_name", device.name)
                        prefs.putString("device_address", device.address)
                        prefs.putBoolean("vinculado_por_bluetoothactivity", true) // ✅ Guardar bandera
                        prefs.apply()

                        val intent = Intent(this@BluetoothActivity, LecturaActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    runOnUiThread {
                        Toast.makeText(this@BluetoothActivity, "Desconectado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()

        val bluetoothAdapter = getSystemService(BluetoothManager::class.java).adapter

        if (binding.switchBluetooth.isChecked && bluetoothAdapter?.isEnabled == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    escanearBLE()
                }
            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    escanearBLE()
                }
            }
        }
    }
}
