package com.example.utpsalud

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.utpsalud.databinding.ActivityBluetoothBinding

class BluetoothActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBluetoothBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ocultar dispositivos al inicio
        binding.containerDispositivos.visibility = View.GONE
        // Mostrar/ocultar según el switch
        binding.switchBluetooth.setOnCheckedChangeListener { _, isChecked ->
            binding.containerDispositivos.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.textIniciar.setOnClickListener {
            startActivity(Intent(this, LecturaActivity::class.java))
            finish()
        }

        binding.textVolver.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }
}
