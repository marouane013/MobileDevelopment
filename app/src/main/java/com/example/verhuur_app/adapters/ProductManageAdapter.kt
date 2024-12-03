package com.example.verhuur_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.verhuur_app.databinding.ItemManageProductBinding
import com.example.verhuur_app.model.Product

class ProductManageAdapter(
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : RecyclerView.Adapter<ProductManageAdapter.ProductViewHolder>() {

    private var products = listOf<Product>()

    inner class ProductViewHolder(val binding: ItemManageProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemManageProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.binding.apply {
            productTitle.text = product.title
            productPrice.text = "€${product.price}/dag"
            
            // Laad product afbeelding
            Glide.with(productImage)
                .load(product.imageUrl)
                .centerCrop()
                .into(productImage)

            // Stel knoppen in
            editButton.setOnClickListener { onEdit(product) }
            deleteButton.setOnClickListener { onDelete(product) }
        }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
} 