package com.example.verhuur_app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.verhuur_app.R
import com.example.verhuur_app.adapters.ProductManageAdapter
import com.example.verhuur_app.databinding.FragmentMyProductsBinding
import com.example.verhuur_app.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyProductsFragment : Fragment() {
    private var _binding: FragmentMyProductsBinding? = null
    private val binding get() = _binding!!
    private lateinit var productAdapter: ProductManageAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadMyProducts()
        
        binding.addProductFab.setOnClickListener {
            findNavController().navigate(R.id.action_myProductsFragment_to_addProductFragment)
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductManageAdapter(
            onEdit = { product ->
                // Navigate to edit product screen
                val bundle = Bundle().apply {
                    putString("productId", product.id)
                    putBoolean("isEditing", true)
                }
                findNavController().navigate(R.id.action_myProductsFragment_to_addProductFragment, bundle)
            },
            onDelete = { product ->
                deleteProduct(product.id)
            }
        )

        binding.recyclerView.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun loadMyProducts() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        FirebaseFirestore.getInstance().collection("products")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val products = documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java).apply { id = doc.id }
                }
                productAdapter.updateProducts(products)
                
                binding.emptyStateLayout.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
                binding.recyclerView.visibility = if (products.isEmpty()) View.GONE else View.VISIBLE
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteProduct(productId: String) {
        FirebaseFirestore.getInstance().collection("products")
            .document(productId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Product verwijderd", Toast.LENGTH_SHORT).show()
                loadMyProducts() // Refresh the list
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 