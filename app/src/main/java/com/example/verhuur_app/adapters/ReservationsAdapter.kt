package com.example.verhuur_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.verhuur_app.databinding.ItemReservationBinding
import com.example.verhuur_app.model.Product
import com.example.verhuur_app.model.RentalPeriod
import com.example.verhuur_app.model.RentalStatus
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ReservationsAdapter(
    private val onItemClick: (Product) -> Unit,
    private val onEndRental: (Product, RentalPeriod) -> Unit
) : RecyclerView.Adapter<ReservationsAdapter.ViewHolder>() {
    
    private var reservations = listOf<Product>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
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
        val product = reservations[position]
        
        holder.binding.apply {
            productTitle.text = product.title
            productPrice.text = "€${product.price}/dag"
            
            val userRental = product.rentalPeriods.find { 
                it.rentedBy == currentUserId && 
                (it.status == "ACTIVE" || it.status == "PENDING")
            }
            
            userRental?.let { rental ->
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                rentalPeriod.text = "${dateFormat.format(rental.startDate)} - ${dateFormat.format(rental.endDate)}"
                
                rentalStatus.text = when(rental.status) {
                    "PENDING" -> "In aanvraag"
                    "ACTIVE" -> "Actief"
                    "COMPLETED" -> "Afgerond"
                    else -> "Afgewezen"
                }

                endRentalButton.visibility = if (rental.status == "ACTIVE" && rental.rentedBy == currentUserId) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                endRentalButton.setOnClickListener {
                    onEndRental(product, rental)
                }
            }
            
            root.setOnClickListener { onItemClick(product) }
        }
    }
    
    override fun getItemCount() = reservations.size
    
    inner class ViewHolder(
        val binding: ItemReservationBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(product: Product) {
            binding.apply {
                productTitle.text = product.title
                productPrice.text = "€${product.price}/dag"
                
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val userRental = product.rentalPeriods.find { 
                    it.rentedBy == currentUserId 
                }
                
                userRental?.let { rental ->
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val period = "${dateFormat.format(rental.startDate)} - ${dateFormat.format(rental.endDate)}"
                    rentalPeriod.text = period
                    
                    rentalStatus.text = when(rental.status) {
                        "PENDING" -> "In aanvraag"
                        "ACTIVE" -> "Actief"
                        "COMPLETED" -> "Afgerond"
                        "CANCELLED" -> "Afgewezen"
                        else -> "Afgewezen"
                    }

                    endRentalButton.visibility = if (rental.status == "ACTIVE" && product.userId == currentUserId) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                    endRentalButton.setOnClickListener {
                        onEndRental(product, rental)
                    }
                }
                
                root.setOnClickListener { onItemClick(product) }
            }
        }
    }
} 