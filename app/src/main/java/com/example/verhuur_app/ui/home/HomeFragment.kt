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
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.Query
import android.view.View.VISIBLE
import com.example.verhuur_app.utils.HorizontalSpaceItemDecoration
import android.view.View.GONE
import com.example.verhuur_app.MainActivity

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
        
        setupRecyclerView()
        setupCategoriesRecyclerView()
        setupClickListeners()
        loadRecentProducts()
    }

    private fun setupClickListeners() {
        // Zoekbalk click
        binding.searchCard.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }

        // FAB click
        binding.addProductFab.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addProductFragment)
        }

        // Bekijk alles button
        binding.seeAllButton.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }
    }

    private fun loadRecentProducts() {
        val db = FirebaseFirestore.getInstance()
        db.collection("products")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(6)
            .get()
            .addOnSuccessListener { documents ->
                val productsList = documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java).apply { id = doc.id }
                }
                productAdapter.updateProducts(productsList)
                
                // Update de zichtbaarheid van de recyclerView
                binding.productsRecyclerView.visibility = if (productsList.isEmpty()) GONE else VISIBLE
                // Toon een leeg-state message indien nodig
                binding.emptyStateLayout?.visibility = if (productsList.isEmpty()) VISIBLE else GONE
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error loading products: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupCategoriesRecyclerView() {
        val categories = listOf(
            Category("Gereedschap", R.drawable.ic_tools, "#FF5722"),
            Category("Tuin", R.drawable.ic_garden, "#4CAF50"),
            Category("Keuken", R.drawable.ic_kitchen, "#2196F3"),
            Category("Elektronica", R.drawable.ic_electronics, "#9C27B0"),
            Category("Sport", R.drawable.ic_sports, "#FF9800"),
            Category("Schoonmaak", R.drawable.ic_cleaning, "#03A9F4")
        )

        val categoryAdapter = CategoryAdapter(categories) { category ->
            val bundle = Bundle().apply {
                putString("categoryName", category.name)
            }
            findNavController().navigate(R.id.action_homeFragment_to_categoryProductsFragment, bundle)
        }

        binding.categoriesRecyclerView.apply {
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            addItemDecoration(HorizontalSpaceItemDecoration(16))
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { product ->
            val bundle = Bundle().apply {
                putString("productId", product.id)
                putString("title", product.title)
                putString("description", product.description)
                putString("price", product.price.toString())
                putString("address", product.address)
                putString("imageUrl", product.imageUrl)
            }
            findNavController().navigate(R.id.action_homeFragment_to_productDetailFragment, bundle)
        }
        
        binding.productsRecyclerView.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 