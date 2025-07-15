package com.example.utpsalud.view.activity

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.utpsalud.databinding.ActivityLoginBinding
import com.example.utpsalud.viewmodel.LoginViewModel
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    // Aquí voy a usar ViewBinding para no buscar views con findViewById
    private lateinit var binding: ActivityLoginBinding
    // Uso ViewModel para separar lógica y mantener datos al rotar pantalla
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflo la vista usando ViewBinding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Empiezo a observar cambios del ViewModel (login estado)
        observarViewModel()

        // Configuro el botón de login para cuando le den clic
        binding.btnLogin.setOnClickListener {
            // Primero cierro el teclado para que no moleste
            ocultarTeclado(it)

            // Tomo los datos escritos en los inputs
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            // Desactivo el botón mientras se procesa el login (para evitar múltiples clics)
            binding.btnLogin.isEnabled = false

            // Le pido al ViewModel que haga login con esos datos
            viewModel.login(email, password)
        }

        // Link para ir a pantalla de registro si no tengo cuenta
        binding.registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish() // cierro esta actividad para no volver aquí con back
        }

        // Si vengo de registro exitoso, muestro snackbar confirmándolo
        val fueRegistroExitoso = intent.getBooleanExtra("registro_exitoso", false)
        if (fueRegistroExitoso) {
            Snackbar.make(findViewById(android.R.id.content), "Registro exitoso", Snackbar.LENGTH_LONG).show()
        }

        // Si vengo de eliminación de cuenta, muestro snackbar informativo
        val cuentaEliminada = intent.getBooleanExtra("cuenta_eliminada", false)
        if (cuentaEliminada) {
            Snackbar.make(findViewById(android.R.id.content), "Cuenta eliminada correctamente", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun observarViewModel() {
        // Aquí me suscribo para reaccionar cuando cambie el estado del login
        viewModel.loginEstado.observe(this) { estado ->
            when (estado) {
                is LoginViewModel.LoginEstado.Success -> {
                    // Reactivo el botón por si vuelve a esta pantalla en otra ocasión
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Iniciar sesión"

                    // Si el login fue exitoso, abro Home y cierro esta actividad
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                is LoginViewModel.LoginEstado.Error -> {
                    // Reactivo el botón para permitir intentar nuevamente
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Iniciar sesión"

                    // Si hubo error, muestro un mensaje al usuario con snackbar
                    Snackbar.make(binding.root, estado.mensaje, Snackbar.LENGTH_LONG).show()
                }
                is LoginViewModel.LoginEstado.Loading -> {
                    // Desactivo el botón mientras se está procesando el login
                    binding.btnLogin.isEnabled = false
                    binding.btnLogin.text = "Iniciando..."
                }
            }
        }
    }

    // Función para ocultar el teclado, la uso cuando ya no necesito que esté visible
    private fun ocultarTeclado(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // Esta función es clave para controlar cuando ocultar el teclado según dónde toque el usuario
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Solo me interesa el evento de tocar la pantalla
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus // obtengo el view que tiene foco ahora (normalmente EditText)
            if (view != null) {
                val outRect = Rect()
                // Obtengo el rectángulo visible del view con foco
                view.getGlobalVisibleRect(outRect)

                // Si el toque está fuera de ese rectángulo (es decir, fuera del EditText con foco)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    val newView = currentFocus
                    // Pero solo oculto teclado si el nuevo foco no es otro TextInputEditText (o sea, si tocó fuera)
                    if (newView !is com.google.android.material.textfield.TextInputEditText) {
                        view.clearFocus() // quito el foco del EditText actual
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        // oculto el teclado porque tocó fuera de un campo de texto
                        imm.hideSoftInputFromWindow(view.windowToken, 0)
                    }
                }
            }
        }
        // Por último dejo que el evento siga su curso normal
        return super.dispatchTouchEvent(ev)
    }
}