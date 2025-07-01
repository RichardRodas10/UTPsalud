package com.example.utpsalud.view.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.utpsalud.R
import com.example.utpsalud.databinding.ActivityBuscarBinding
import com.example.utpsalud.model.Usuario
import com.example.utpsalud.view.adapter.UsuarioAdapter
import com.example.utpsalud.viewmodel.BuscarViewModel

class BuscarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBuscarBinding
    private val viewModel: BuscarViewModel by viewModels()
    private lateinit var adapter: UsuarioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBuscarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarAdapter()
        observarViewModel()
        configurarBuscador()

        binding.iconInfo.setOnClickListener { finish() }
    }

    private fun configurarAdapter() {
        adapter = UsuarioAdapter(
            usuarios = emptyList(),
            estadoSolicitudes = mutableMapOf(),
            uidActual = "",
            esAdmin = false,
            onAgregar = { viewModel.enviarSolicitud(it.uid) },
            onCancelar = { viewModel.cancelarSolicitud(it.uid) },
            onConfirmar = { viewModel.aceptarSolicitud(it.uid) },
            onClickItem = { usuario ->
                val intent = Intent(this, PerfilActivity::class.java)
                intent.putExtra("uid", usuario.uid)
                startActivity(intent)
            }
        )
        binding.rvResultados.layoutManager = LinearLayoutManager(this)
        binding.rvResultados.adapter = adapter
    }

    private fun observarViewModel() {
        viewModel.usuarios.observe(this) { lista ->
            adapter.actualizarUsuarios(lista)
            binding.rvResultados.visibility = if (lista.isNotEmpty()) View.VISIBLE else View.GONE

            if (binding.editBuscar.text.toString().isEmpty()) {
                binding.tvNoResultados.visibility = View.GONE
            } else {
                binding.tvNoResultados.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewModel.estadoSolicitudes.observe(this) { estados ->
            adapter.actualizarEstadoSolicitudes(estados.toMutableMap())
        }

        viewModel.uidActual.observe(this) {
            adapter.uidActual = it
        }

        viewModel.esAdmin.observe(this) {
            adapter.esAdmin = it
        }

        viewModel.loading.observe(this) { cargando ->
            binding.progressBarBuscar.visibility = if (cargando) View.VISIBLE else View.GONE
            if (cargando) {
                binding.rvResultados.visibility = View.GONE
                binding.tvNoResultados.visibility = View.GONE
            }
        }

        viewModel.toast.observe(this) { mensaje ->
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarBuscador() {
        binding.editBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val texto = s.toString().trim()
                val icon = if (texto.isNotEmpty()) R.drawable.ic_clear else 0
                binding.editBuscar.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)
                viewModel.buscarUsuariosDebounce(texto)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.editBuscar.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = binding.editBuscar.compoundDrawables[2]
                drawableEnd?.let {
                    val bounds = it.bounds
                    val x = event.rawX.toInt()
                    val editTextRight = binding.editBuscar.right
                    val drawableWidth = bounds.width()
                    if (x >= (editTextRight - drawableWidth - binding.editBuscar.paddingEnd)) {
                        binding.editBuscar.text?.clear()
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }
}