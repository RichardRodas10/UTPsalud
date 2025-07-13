package com.example.utpsalud.view.fragment

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.utpsalud.R
import com.example.utpsalud.databinding.FragmentHomeBinding
import com.example.utpsalud.ui.home.HomeFragmentViewModel
import com.example.utpsalud.view.activity.BluetoothActivity
import com.example.utpsalud.view.activity.MedicionmanualActivity

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observa LiveData para mostrar el diálogo de instrucciones
        viewModel.mostrarInstrucciones.observe(viewLifecycleOwner) { mostrar ->
            if (mostrar) {
                showInstruccionesDialog()
                viewModel.instruccionesMostradas()
            }
        }

        binding.btnInstrucciones.setOnClickListener {
            viewModel.pedirMostrarInstrucciones()
        }

        binding.btnIniciarMedicion.setOnClickListener {
            val intent = Intent(requireContext(), BluetoothActivity::class.java)
            startActivity(intent)
        }

        binding.btnIniciarManual.setOnClickListener {
            val intent = Intent(requireContext(), MedicionmanualActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showInstruccionesDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_instrucciones, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<TextView>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}