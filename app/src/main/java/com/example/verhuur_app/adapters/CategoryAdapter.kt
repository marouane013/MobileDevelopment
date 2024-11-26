package com.example.verhuur_app.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.verhuur_app.databinding.ItemCategoryBinding
import com.example.verhuur_app.model.Category

class CategoryAdapter(
    private val categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount() = categories.size

    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(category: Category) {
            binding.apply {
                categoryName.text = category.name
                categoryIcon.setImageResource(category.iconResId)
                // Optioneel: achtergrondkleur instellen
                try {
                    root.setBackgroundColor(Color.parseColor(category.color))
                } catch (e: IllegalArgumentException) {
                    // Fallback kleur als de parsing mislukt
                    root.setBackgroundColor(Color.GRAY)
                }
                root.setOnClickListener { onCategoryClick(category) }
            }
        }
    }
} 