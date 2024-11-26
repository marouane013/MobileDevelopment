package com.example.verhuur_app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.verhuur_app.Category
import com.example.verhuur_app.CategoryAdapter
import com.example.verhuur_app.R
import com.example.verhuur_app.databinding.FragmentHomeBinding
import com.example.verhuur_app.models.RentalItem
import com.example.verhuur_app.ui.base.BaseFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.verhuur_app.adapters.ProductAdapter
import com.example.verhuur_app.databinding.ItemProductBinding
import com.example.verhuur_app.model.Product
import android.graphics.BitmapFactory
import java.io.File
import androidx.recyclerview.widget.RecyclerView

class HomeFragment : BaseFragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        
        setupWelcomeMessage()
        setupCategories()
        setupRecentItems()
        setupRecyclerView()
        loadProducts()
    }

    private fun setupWelcomeMessage() {
        auth.currentUser?.let { user ->
            binding.userNameText.text = user.displayName ?: "User"
        }
    }

    private fun setupCategories() {
        binding.categoriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = CategoryAdapter(getCategories()) { category ->
                // Handle category click
                Toast.makeText(context, "Selected: ${category.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecentItems() {
        binding.recentItemsRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = RecentItemsAdapter(getRecentItems()) { item ->
                Toast.makeText(
                    requireContext(),
                    "Selected: ${item.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter()
        binding.productsRecyclerView.apply {
            this.adapter = productAdapter
            this.layoutManager = LinearLayoutManager(context)
        }
    }

    private fun loadProducts() {
        val db = FirebaseFirestore.getInstance()
        db.collection("products")
            .get()
            .addOnSuccessListener { documents ->
                val productsList = documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)
                }
                productAdapter.updateProducts(productsList)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error loading products: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun getCategories() = listOf(
        Category("Kitchen", R.drawable.ic_kitchen, "#FF6D00"),
        Category("Garden", R.drawable.ic_garden, "#00C853"),
        Category("Cleaning", R.drawable.ic_cleaning, "#2962FF"),
        Category("Tools", R.drawable.ic_tools, "#FF3D00"),
        Category("Electronics", R.drawable.ic_electronics, "#6200EA"),
        Category("Sports", R.drawable.ic_sports, "#00BFA5")
    )

    private fun getRecentItems() = listOf(
        RentalItem("Professional Drill", "Tools", "€15/day", R.drawable.ic_tools),
        RentalItem("Lawn Mower", "Garden", "€25/day", R.drawable.ic_garden),
        RentalItem("Stand Mixer", "Kitchen", "€20/day", R.drawable.ic_kitchen),
        RentalItem("Pressure Washer", "Cleaning", "€30/day", R.drawable.ic_cleaning)
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 