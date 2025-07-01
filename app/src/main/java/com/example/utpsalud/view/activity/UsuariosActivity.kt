package com.example.utpsalud.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.utpsalud.databinding.ActivityUsuariosBinding
import com.example.utpsalud.model.Usuario
import com.example.utpsalud.view.adapter.UsuarioAdapter
import com.example.utpsalud.viewmodel.UsuariosViewModel
import com.example.utpsalud.view.activity.PerfilActivity

class UsuariosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsuariosBinding
    private val viewModel: UsuariosViewModel by viewModels()

    private lateinit var adapterDisp: UsuarioAdapter
    private lateinit var adapterEnv: UsuarioAdapter
    private lateinit var adapterRec: UsuarioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsuariosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.iconBack.setOnClickListener { finish() }

        configurarAdapters()
        observarViewModel()

        viewModel.obtenerRolYUsuarios()
    }

    private fun configurarAdapters() {
        adapterDisp = UsuarioAdapter(
            usuarios = emptyList(),
            estadoSolicitudes = mutableMapOf(),
            uidActual = "",
            esAdmin = false,
            onAgregar = { viewModel.enviarSolicitud(it.uid) },
            onCancelar = { viewModel.cancelarSolicitud(it.uid) },
            onConfirmar = { viewModel.aceptarSolicitud(it.uid) },
            onClickItem = { navegarPerfil(it) }
        )
        adapterEnv = UsuarioAdapter(
            usuarios = emptyList(),
            estadoSolicitudes = mutableMapOf(),
            uidActual = "",
            esAdmin = false,
            onAgregar = {},
            onCancelar = { viewModel.cancelarSolicitud(it.uid) },
            onConfirmar = {},
            onClickItem = { navegarPerfil(it) }
        )
        adapterRec = UsuarioAdapter(
            usuarios = emptyList(),
            estadoSolicitudes = mutableMapOf(),
            uidActual = "",
            esAdmin = false,
            onAgregar = {},
            onCancelar = {},
            onConfirmar = { viewModel.aceptarSolicitud(it.uid) },
            onClickItem = { navegarPerfil(it) }
        )

        binding.rvUsuarios.layoutManager = LinearLayoutManager(this)
        binding.rvUsuarios.adapter = adapterDisp

        binding.rvSolicitudesEnviadas.layoutManager = LinearLayoutManager(this)
        binding.rvSolicitudesEnviadas.adapter = adapterEnv

        binding.rvSolicitudesRecibidas.layoutManager = LinearLayoutManager(this)
        binding.rvSolicitudesRecibidas.adapter = adapterRec
    }

    private fun observarViewModel() {
        viewModel.loading.observe(this) { mostrarCargando(it) }
        viewModel.toast.observe(this) { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }

        viewModel.uidActual.observe(this) {
            adapterDisp = adapterDisp.copy(uidActual = it)
            adapterEnv = adapterEnv.copy(uidActual = it)
            adapterRec = adapterRec.copy(uidActual = it)

            // Actualizar adapters en recyclers para refrescar cambios
            binding.rvUsuarios.adapter = adapterDisp
            binding.rvSolicitudesEnviadas.adapter = adapterEnv
            binding.rvSolicitudesRecibidas.adapter = adapterRec
        }

        viewModel.esAdmin.observe(this) {
            adapterDisp = adapterDisp.copy(esAdmin = it)
            adapterEnv = adapterEnv.copy(esAdmin = it)
            adapterRec = adapterRec.copy(esAdmin = it)

            binding.rvUsuarios.adapter = adapterDisp
            binding.rvSolicitudesEnviadas.adapter = adapterEnv
            binding.rvSolicitudesRecibidas.adapter = adapterRec
        }

        viewModel.estadoSolicitudes.observe(this) {
            adapterDisp = adapterDisp.copy(estadoSolicitudes = it.toMutableMap())
            adapterEnv = adapterEnv.copy(estadoSolicitudes = it.toMutableMap())
            adapterRec = adapterRec.copy(estadoSolicitudes = it.toMutableMap())

            binding.rvUsuarios.adapter = adapterDisp
            binding.rvSolicitudesEnviadas.adapter = adapterEnv
            binding.rvSolicitudesRecibidas.adapter = adapterRec
        }

        viewModel.listaDisponibles.observe(this) {
            adapterDisp = adapterDisp.copy(usuarios = it)
            binding.rvUsuarios.adapter = adapterDisp
            actualizarVisibilidad(binding.rvUsuarios, it)
        }

        viewModel.listaEnviadas.observe(this) {
            adapterEnv = adapterEnv.copy(usuarios = it)
            binding.rvSolicitudesEnviadas.adapter = adapterEnv
            actualizarVisibilidad(binding.rvSolicitudesEnviadas, it)
        }

        viewModel.listaRecibidas.observe(this) {
            adapterRec = adapterRec.copy(usuarios = it)
            binding.rvSolicitudesRecibidas.adapter = adapterRec
            actualizarVisibilidad(binding.rvSolicitudesRecibidas, it)
        }
    }

    private fun actualizarVisibilidad(view: View, lista: List<Usuario>) {
        view.visibility = if (lista.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun mostrarCargando(cargando: Boolean) {
        binding.progressBarUsuarios.visibility = if (cargando) View.VISIBLE else View.GONE
    }

    private fun navegarPerfil(usuario: Usuario) {
        val intent = Intent(this, PerfilActivity::class.java)
        intent.putExtra("uid", usuario.uid)
        startActivity(intent)
    }
}