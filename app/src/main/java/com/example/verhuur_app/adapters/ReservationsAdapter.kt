package com.example.verhuur_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.verhuur_app.databinding.ItemReservationBinding
import com.example.verhuur_app.model.Product
import com.example.verhuur_app.model.RentalStatus
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ReservationsAdapter(
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<ReservationsAdapter.ViewHolder>() {
    
    private var reservations = listOf<Product>()
    
    fun updateReservations(newReservations: List<Product>) {
        reservations = newReservations
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReservationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(reservations[position])
    }
    
    override fun getItemCount() = reservations.size
    
    inner class ViewHolder(
        private val binding: ItemReservationBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(product: Product) {
            binding.apply {
                productTitle.text = product.title
                productPrice.text = "€${product.price}/dag"
                
                // Vind de huurperiode van de huidige gebruiker
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val userRental = product.rentalPeriods.find { 
                    it.rentedBy == currentUserId 
                }
                
                // Toon de huurperiode
                userRental?.let { rental ->
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val period = "${dateFormat.format(rental.startDate)} - ${dateFormat.format(rental.endDate)}"
                    rentalPeriod.text = period
                    
                    // Toon de status
                    rentalStatus.text = when(rental.status) {
                        "PENDING" -> "In aanvraag"
                        "ACTIVE" -> "Actief"
                        "COMPLETED" -> "Afgerond"
                        else -> "Onbekend"
                    }
                }
                
                root.setOnClickListener { onItemClick(product) }
            }
        }
    }
} 