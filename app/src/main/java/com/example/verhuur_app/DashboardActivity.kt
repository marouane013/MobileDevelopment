package com.example.verhuur_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import android.graphics.Color
import com.example.verhuur_app.databinding.ItemCategoryBinding

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Setup toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Setup categories RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.categories_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 2 columns

        // Create and set adapter with categories
        val categories = listOf(
            Category("Keuken", R.drawable.ic_kitchen, "#FF6D00"),
            Category("Tuin", R.drawable.ic_garden, "#00C853"),
            Category("Schoonmaak", R.drawable.ic_cleaning, "#2962FF"),
            Category("Gereedschap", R.drawable.ic_tools, "#FF3D00"),
            Category("Electronica", R.drawable.ic_electronics, "#6200EA"),
            Category("Sport", R.drawable.ic_sports, "#00BFA5")
        )

        recyclerView.adapter = CategoryAdapter(categories) { category ->
            // Handle category click (navigate to category items)
            // Implementation voor later
        }
    }
}

data class Category(
    val name: String,
    val iconResId: Int,
    val colorHex: String
)

class CategoryAdapter(
    private val categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.binding.apply {
            categoryName.text = category.name
            categoryIcon.setImageResource(category.iconResId)

            // Set card color
            categoryCard.setCardBackgroundColor(Color.parseColor(category.colorHex))

            // Add ripple effect on click
            categoryCard.setOnClickListener {
                onCategoryClick(category)
            }
        }
    }

    override fun getItemCount() = categories.size
}