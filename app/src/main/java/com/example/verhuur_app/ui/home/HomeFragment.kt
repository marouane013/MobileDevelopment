package com.example.verhuur_app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.example.verhuur_app.adapters.CategoryAdapter
import com.example.verhuur_app.model.Category

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
    }

    private fun setupCategoriesRecyclerView() {
        val categories = listOf(
            Category("Gereedschap", R.drawable.ic_tools),
            Category("Tuin", R.drawable.ic_garden),
            Category("Keuken", R.drawable.ic_kitchen),
            Category("Elektronica", R.drawable.ic_electronics),
            Category("Sport", R.drawable.ic_sports),
            Category("Schoonmaak", R.drawable.ic_cleaning)
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
            addItemDecoration(HorizontalSpaceItemDecoration(8))
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

    fun rentProduct(productId: String, userId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("products").document(productId)
            .update("rentedBy", userId)
            .addOnSuccessListener {
                // Product succesvol gehuurd
            }
            .addOnFailureListener { e ->
                // Error bij huren
            }
    }

    fun stopRenting(productId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("products").document(productId)
            .update("rentedBy", null)
            .addOnSuccessListener {
                // Huur succesvol gestopt
            }
            .addOnFailureListener { e ->
                // Error bij stoppen huur
            }
    }
} 