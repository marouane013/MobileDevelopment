package com.example.verhuur_app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.verhuur_app.databinding.ItemRecentRentalBinding
import com.example.verhuur_app.models.RentalItem

class RecentItemsAdapter(
    private val items: List<RentalItem>,
    private val onItemClick: (RentalItem) -> Unit
) : RecyclerView.Adapter<RecentItemsAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemRecentRentalBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: RentalItem) {
            binding.apply {
                itemName.text = item.name
                itemCategory.text = item.category
                itemPrice.text = item.price
                itemImage.setImageResource(item.imageResId)
                root.setOnClickListener { onItemClick(item) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentRentalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
  