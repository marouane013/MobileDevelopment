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
import com.example.verhuur_app.adapters.ReservationsAdapter
import com.example.verhuur_app.model.Category
import com.example.verhuur_app.model.RentalStatus

class HomeFragment : BaseFragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var reservationsAdapter: ReservationsAdapter
    private lateinit var categoryAdapter: CategoryAdapter

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
        
        setupCategoryAdapter()
        setupReservationsAdapter()
        loadUserReservations()
    }

    private fun setupCategoryAdapter() {
        val categories = listOf(
            Category("Tuin", R.drawable.img_garden),
            Category("Schoonmaak", R.drawable.img_cleaning),
            Category("Gereedschap", R.drawable.img_tools),
            Category("Electronica", R.drawable.img_electronics),
            Category("Sport", R.drawable.img_sports),
            Category("Keuken", R.drawable.img_kitchen)
        )

        categoryAdapter = CategoryAdapter(categories) { category ->
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

    private fun setupReservationsAdapter() {
        reservationsAdapter = ReservationsAdapter { product ->
            val bundle = Bundle().apply {
                putString("productId", product.id)
            }
            findNavController().navigate(R.id.action_homeFragment_to_productDetailFragment, bundle)
        }
        
        binding.reservationsRecyclerView.apply {
            adapter = reservationsAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun loadUserReservations() {
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        FirebaseFirestore.getInstance().collection("products")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                
                val reservations = documents.mapNotNull { doc ->
                    val product = doc.toObject(Product::class.java).also { it.id = doc.id }
                    val hasActiveRental = product.rentalPeriods.any { period ->
                        period.rentedBy == currentUser && 
                        (period.status == "ACTIVE" || period.status == "PENDING")
                    }
                    if (hasActiveRental) product else null
                }
                
                reservationsAdapter.updateReservations(reservations)
                binding.reservationsRecyclerView.visibility = 
                    if (reservations.isEmpty()) View.GONE else View.VISIBLE
                binding.noReservationsText.visibility = 
                    if (reservations.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 