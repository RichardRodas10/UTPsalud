import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.utpsalud.R
import com.example.utpsalud.model.MensajeSoporte
import java.text.SimpleDateFormat
import java.util.*

class MensajesSoporteAdapter(
    private val mensajes: List<MensajeSoporte>
) : RecyclerView.Adapter<MensajesSoporteAdapter.MensajeViewHolder>() {

    class MensajeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
        val tvMensaje: TextView = view.findViewById(R.id.tvMensaje)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MensajeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mensaje_soporte, parent, false)
        return MensajeViewHolder(view)
    }

    override fun onBindViewHolder(holder: MensajeViewHolder, position: Int) {
        val mensaje = mensajes[position]
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fechaStr = if (mensaje.timestamp > 0) sdf.format(Date(mensaje.timestamp)) else "Fecha no disponible"
        holder.tvFecha.text = fechaStr
        holder.tvMensaje.text = mensaje.mensaje
    }

    override fun getItemCount(): Int = mensajes.size
}