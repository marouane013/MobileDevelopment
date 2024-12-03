package com.example.verhuur_app.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.verhuur_app.databinding.FragmentNotificationsBinding
import com.example.verhuur_app.ui.base.BaseFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.verhuur_app.model.RentalRequest
import android.widget.Toast
import com.example.verhuur_app.adapters.RentalRequestAdapter
import com.example.verhuur_app.model.RentalStatus
import java.util.Date

class NotificationsFragment : BaseFragment() {
    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var requestAdapter: RentalRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadRentalRequests()
    }

    private fun setupRecyclerView() {
        requestAdapter = RentalRequestAdapter(
            onAccept = { request -> handleRentalRequest(request, true) },
            onDecline = { request -> handleRentalRequest(request, false) }
        )
        
        binding.requestsRecyclerView.apply {
            adapter = requestAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun loadRentalRequests() {
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        
        db.collection("products")
            .whereEqualTo("userId", currentUser)
            .get()
            .addOnSuccessListener { products ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                
                val requests = mutableListOf<RentalRequest>()
                
                for (product in products) {
                    val rentalPeriods = product.get("rentalPeriods") as? List<Map<String, Any>> ?: continue
                    
                    for (period in rentalPeriods) {
                        if (period["status"] == RentalStatus.PENDING.name) {
                            val startTimestamp = period["startDate"] as com.google.firebase.Timestamp
                            val endTimestamp = period["endDate"] as com.google.firebase.Timestamp
                            
                            requests.add(
                                RentalRequest(
                                    productId = product.id,
                                    productTitle = product.getString("title") ?: "",
                                    startDate = startTimestamp.toDate(),
                                    endDate = endTimestamp.toDate(),
                                    renterId = period["rentedBy"] as String
                                )
                            )
                        }
                    }
                }
                
                requestAdapter.submitList(requests)
                updateEmptyState(requests.isEmpty())
            }
    }

    private fun handleRentalRequest(request: RentalRequest, isAccepted: Boolean) {
        if (!isAdded || _binding == null) return
        
        val db = FirebaseFirestore.getInstance()
        val newStatus = if (isAccepted) {
            RentalStatus.ACTIVE
        } else {
            RentalStatus.CANCELLED
        }
        
        db.collection("products").document(request.productId)
            .get()
            .addOnSuccessListener { document ->
                val rentalPeriods = document.get("rentalPeriods") as? List<Map<String, Any>> ?: return@addOnSuccessListener
                
                val updatedPeriods = rentalPeriods.map { period ->
                    val periodStartTimestamp = period["startDate"] as com.google.firebase.Timestamp
                    val periodEndTimestamp = period["endDate"] as com.google.firebase.Timestamp
                    
                    if (periodStartTimestamp.toDate() == request.startDate && 
                        periodEndTimestamp.toDate() == request.endDate && 
                        period["rentedBy"] == request.renterId) {
                        period + mapOf("status" to newStatus.name)
                    } else {
                        period
                    }
                }
                
                db.collection("products").document(request.productId)
                    .update("rentalPeriods", updatedPeriods)
                    .addOnSuccessListener {
                        val message = if (isAccepted) "Huurverzoek geaccepteerd" else "Huurverzoek afgewezen"
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        loadRentalRequests()
                    }
            }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (!isAdded || _binding == null) return
        
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.requestsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 