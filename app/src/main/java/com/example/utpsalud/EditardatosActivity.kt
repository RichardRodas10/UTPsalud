package com.example.utpsalud

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.utpsalud.databinding.ActivityBluetoothBinding
import com.example.utpsalud.databinding.ActivityEditardatosBinding

class EditardatosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditardatosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditardatosBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}