package com.example.verhuur_app.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.verhuur_app.R
import com.example.verhuur_app.adapters.ProductAdapter
import com.example.verhuur_app.databinding.FragmentCategoryProductsBinding
import com.example.verhuur_app.model.Product
import com.google.firebase.firestore.FirebaseFirestore

class CategoryProductsFragment : Fragment() {
    private var _binding: FragmentCategoryProductsBinding? = null
    private val binding get() = _binding!!
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Haal categorie naam op uit arguments
        val categoryName = arguments?.getString("categoryName") ?: return
        
        // Setup toolbar met back button
        binding.toolbar.apply {
            title = categoryName
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }
        
        setupRecyclerView()
        loadCategoryProducts(categoryName)
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
            findNavController().navigate(R.id.action_categoryProductsFragment_to_productDetailFragment, bundle)
        }
        
        binding.recyclerView.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun loadCategoryProducts(categoryName: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("products")
            .whereEqualTo("category", categoryName)
            .get()
            .addOnSuccessListener { documents ->
                val productsList = documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java).apply { id = doc.id }
                }
                productAdapter.updateProducts(productsList)
                
                // Toon empty state als er geen producten zijn
                binding.emptyStateLayout.visibility = if (productsList.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (productsList.isEmpty()) View.GONE else View.VISIBLE
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error loading products: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 