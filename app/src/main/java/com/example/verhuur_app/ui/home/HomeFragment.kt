package com.example.verhuur_app.ui.home

import MyRequestsAdapter
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
import androidx.appcompat.app.AlertDialog
import android.widget.TextView
import com.example.verhuur_app.model.RentalPeriod
import android.graphics.Color
import android.text.format.DateFormat
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RatingBar
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FieldValue

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
        setupMyRequestsButton()
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
        reservationsAdapter = ReservationsAdapter(
            onItemClick = { product ->
                val bundle = Bundle().apply {
                    putString("productId", product.id)
                }
                findNavController().navigate(R.id.action_homeFragment_to_productDetailFragment, bundle)
            },
            onEndRental = { product, rentalPeriod ->
                showEndRentalConfirmationDialog(product, rentalPeriod)
            }
        )
        
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

    private fun setupMyRequestsButton() {
        binding.myRequestsButton.setOnClickListener {
            showMyRequestsDialog()
        }
    }

    private fun showMyRequestsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_my_requests, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.requestsRecyclerView)
        val noRequestsText = dialogView.findViewById<TextView>(R.id.noRequestsText)
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        // Load user's rental requests
        val currentUser = auth.currentUser?.uid
        currentUser?.let { uid ->
            FirebaseFirestore.getInstance().collection("products")
                .get()
                .addOnSuccessListener { documents ->
                    val requests = mutableListOf<Pair<Product, RentalPeriod>>()
                    
                    for (doc in documents) {
                        val product = doc.toObject(Product::class.java).apply { id = doc.id }
                        val rentalPeriods = product.rentalPeriods
                        
                        rentalPeriods.forEach { period ->
                            if (period.rentedBy == uid) {
                                requests.add(product to period)
                            }
                        }
                    }

                    if (requests.isEmpty()) {
                        noRequestsText.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        noRequestsText.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        
                        recyclerView.adapter = MyRequestsAdapter(requests) { product, rentalPeriod ->
                            showRentalRequestDetails(product, rentalPeriod)
                            dialog.dismiss()
                        }
                    }
                }
        }

        dialog.show()
    }

    private fun showRentalRequestDetails(product: Product, rentalPeriod: RentalPeriod) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rental_request_details, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.apply {
            findViewById<TextView>(R.id.productTitle).text = product.title
            findViewById<TextView>(R.id.productPrice).text = "€${product.price}/dag"
            findViewById<TextView>(R.id.productAddress).text = product.address

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val period = "${dateFormat.format(rentalPeriod.startDate)} - ${dateFormat.format(rentalPeriod.endDate)}"
            findViewById<TextView>(R.id.rentalPeriod).text = period

            val statusText = findViewById<TextView>(R.id.requestStatus)
            statusText.text = when (rentalPeriod.status) {
                "PENDING" -> "In aanvraag"
                "ACTIVE" -> "Actief"
                "COMPLETED" -> "Afgerond"
                "CANCELLED" -> "Afgewezen"
                else -> "Afgewezen"
            }
            
            statusText.setBackgroundColor(when (rentalPeriod.status) {
                "PENDING" -> Color.parseColor("#FFA000")
                "ACTIVE" -> Color.parseColor("#4CAF50")
                "COMPLETED" -> Color.parseColor("#757575")
                "CANCELLED" -> Color.parseColor("#F44336")
                else -> Color.parseColor("#F44336")
            })

            findViewById<MaterialButton>(R.id.viewProductButton).setOnClickListener {
                dialog.dismiss()
                val bundle = Bundle().apply {
                    putString("productId", product.id)
                }
                findNavController().navigate(R.id.action_homeFragment_to_productDetailFragment, bundle)
            }

            // Show rating layout only for completed rentals
            val ratingLayout = findViewById<LinearLayout>(R.id.ratingLayout)
            ratingLayout.visibility = if (rentalPeriod.status == "COMPLETED") View.VISIBLE else View.GONE

            findViewById<Button>(R.id.submitReviewButton)?.setOnClickListener {
                val rating = findViewById<RatingBar>(R.id.ratingBar).rating
                val review = findViewById<TextInputEditText>(R.id.reviewText).text.toString()
                
                submitReview(product.id, rentalPeriod, rating, review)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun submitReview(productId: String, rentalPeriod: RentalPeriod, rating: Float, review: String) {
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser
        
        db.collection("products").document(productId)
            .get()
            .addOnSuccessListener { document ->
                val product = document.toObject(Product::class.java)
                
                val reviewData = hashMapOf(
                    "productId" to productId,
                    "productTitle" to (product?.title ?: ""),
                    "productOwnerId" to (product?.userId ?: ""),
                    "renterId" to rentalPeriod.rentedBy,
                    "renterName" to (currentUser?.displayName ?: "Anoniem"),
                    "rating" to rating,
                    "review" to review,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                db.collection("reviews")
                    .add(reviewData)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Review geplaatst", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Fout bij plaatsen review: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun showEndRentalConfirmationDialog(product: Product, rentalPeriod: RentalPeriod) {
        AlertDialog.Builder(requireContext())
            .setTitle("Beëindig verhuur")
            .setMessage("Weet je zeker dat je deze verhuur wilt beëindigen?")
            .setPositiveButton("Ja") { _, _ ->
                endRental(product, rentalPeriod)
            }
            .setNegativeButton("Nee", null)
            .show()
    }

    private fun endRental(product: Product, rentalPeriod: RentalPeriod) {
        val db = FirebaseFirestore.getInstance()
        
        // Find and update the specific rental period
        val updatedRentalPeriods = product.rentalPeriods.map { period ->
            if (period == rentalPeriod) {
                period.copy(status = "COMPLETED")
            } else {
                period
            }
        }

        db.collection("products").document(product.id)
            .update("rentalPeriods", updatedRentalPeriods)
            .addOnSuccessListener {
                Toast.makeText(context, "Verhuur succesvol beëindigd", Toast.LENGTH_SHORT).show()
                loadUserReservations() // Reload the reservations
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Fout bij beëindigen verhuur: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 