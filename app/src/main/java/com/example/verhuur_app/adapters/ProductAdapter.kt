package com.example.verhuur_app.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.verhuur_app.databinding.ItemProductBinding
import com.example.verhuur_app.model.Product
import java.io.File

class ProductAdapter : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {
    
    private var products = listOf<Product>()

    // ViewHolder class
    inner class ProductViewHolder(private val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.apply {
                productTitle.text = product.title
                productPrice.text = "€${product.price}"
                productDescription.text = product.description
                productCategory.text = product.category
                productAddress.text = product.address
                
                // Laad de foto
                if (product.imageUrl.isNotEmpty()) {
                    val imgFile = File(product.imageUrl)
                    if (imgFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                        productImage.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
} 