import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.verhuur_app.databinding.ItemMyRequestBinding
import com.example.verhuur_app.model.Product
import com.example.verhuur_app.model.RentalPeriod
import java.text.SimpleDateFormat
import java.util.Locale

class MyRequestsAdapter(
    private val requests: List<Pair<Product, RentalPeriod>>,
    private val onItemClick: (Product, RentalPeriod) -> Unit
) : RecyclerView.Adapter<MyRequestsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMyRequestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyRequestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (product, rentalPeriod) = requests[position]
        val status = rentalPeriod.status
        
        holder.binding.apply {
            productTitle.text = product.title
            // Add rental period dates
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val period = "${dateFormat.format(rentalPeriod.startDate)} - ${dateFormat.format(rentalPeriod.endDate)}"
            rentalPeriodText.text = period
            
            requestStatus.text = when (status) {
                "PENDING" -> "In aanvraag"
                "ACTIVE" -> "Actief"
                "COMPLETED" -> "Afgerond"
                "CANCELLED" -> "Afgewezen"
                else -> "Afgewezen"
            }
            
            // Set status color
            requestStatus.setBackgroundColor(when (status) {
                "PENDING" -> Color.parseColor("#FFA000") // Orange
                "ACTIVE" -> Color.parseColor("#4CAF50")  // Green
                "COMPLETED" -> Color.parseColor("#757575") // Gray
                "CANCELLED" -> Color.parseColor("#F44336") // Red
                else -> Color.parseColor("#F44336") // Red
            })
            
            root.setOnClickListener { onItemClick(product, rentalPeriod) }
        }
    }

    override fun getItemCount() = requests.size
} 