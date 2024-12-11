package com.example.verhuur_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.verhuur_app.databinding.ItemRentalRequestBinding
import com.example.verhuur_app.model.RentalRequest
import java.text.SimpleDateFormat
import java.util.Locale

class RentalRequestAdapter(
    private val onAccept: (RentalRequest) -> Unit,
    private val onDecline: (RentalRequest) -> Unit
) : ListAdapter<RentalRequest, RentalRequestAdapter.ViewHolder>(RentalRequestDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRentalRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemRentalRequestBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(request: RentalRequest) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            
            binding.apply {
                productTitle.text = request.productTitle
                dateRange.text = "${dateFormat.format(request.startDate)} - ${dateFormat.format(request.endDate)}"
                
                acceptButton.setOnClickListener { onAccept(request) }
                declineButton.setOnClickListener { onDecline(request) }
            }
        }
    }
}

class RentalRequestDiffCallback : DiffUtil.ItemCallback<RentalRequest>() {
    override fun areItemsTheSame(oldItem: RentalRequest, newItem: RentalRequest): Boolean {
        return oldItem.productId == newItem.productId && 
               oldItem.startDate == newItem.startDate && 
               oldItem.endDate == newItem.endDate
    }

    override fun areContentsTheSame(oldItem: RentalRequest, newItem: RentalRequest): Boolean {
        return oldItem == newItem
    }
} 