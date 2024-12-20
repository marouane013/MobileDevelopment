package com.example.verhuur_app.ui.profile

import MyRequestsAdapter
import Review
import ReviewsAdapter
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.verhuur_app.LoginActivity
import com.example.verhuur_app.R
import com.example.verhuur_app.databinding.FragmentProfileBinding
import com.example.verhuur_app.ui.base.BaseFragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.textfield.TextInputEditText
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.Query
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.verhuur_app.model.Product
import com.example.verhuur_app.model.RentalPeriod
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : BaseFragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        
        setupUserProfile()
        setupLogoutButton()
        setupEditProfileButton()
        setupMyProductsButton()
        setupMyReviewsButton()
        setupMyRentalsButton()
    }

    private fun setupUserProfile() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            binding.apply {
                userEmail.text = user.email
                userName.text = user.displayName ?: "User"
                // Als je een profielfoto hebt:
                user.photoUrl?.let { uri ->
                    profileImage.setImageURI(uri)
                }
            }
        }
    }

    private fun setupLogoutButton() {
        binding.logoutButton.setOnClickListener {
            auth.signOut()
            // Navigeer terug naar LoginActivity
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun setupEditProfileButton() {
        binding.editProfileButton.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile_options, null)
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            val addAddressButton = dialogView.findViewById<MaterialButton>(R.id.addAddressButton)
            
            // Check if user has an address
            val userId = auth.currentUser?.uid
            userId?.let { uid ->
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .get()
                    .addOnSuccessListener { document ->
                        val existingAddress = document.getString("address")
                        addAddressButton.text = if (existingAddress != null) "Edit Address" else "Add Address"
                    }
            }

            dialogView.findViewById<MaterialButton>(R.id.addAddressButton).setOnClickListener {
                dialog.dismiss()
                showAddAddressDialog()
            }

            dialogView.findViewById<MaterialButton>(R.id.resetPasswordButton).setOnClickListener {
                // Handle reset password logic
                dialog.dismiss()
                resetPassword()
            }

            dialog.show()
        }
    }

    private fun setupMyProductsButton() {
        binding.myProductsButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_myProductsFragment)
        }
    }

    private fun setupMyReviewsButton() {
        binding.myReviewsButton.setOnClickListener {
            showMyReviewsDialog()
        }
    }

    private fun setupMyRentalsButton() {
        binding.myRentalsButton.setOnClickListener {
            showMyRentalsDialog()
        }
    }

    private fun showMyReviewsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_my_reviews, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.myReviewsRecyclerView)
        val noReviewsText = dialogView.findViewById<TextView>(R.id.noMyReviewsText)
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        val reviewsAdapter = ReviewsAdapter()
        recyclerView.adapter = reviewsAdapter

        // Load reviews for products owned by current user
        val currentUser = auth.currentUser?.uid
        currentUser?.let { uid ->
            FirebaseFirestore.getInstance().collection("reviews")
                .whereEqualTo("productOwnerId", uid)
                .get()
                .addOnSuccessListener { reviewDocuments ->
                    val reviews = reviewDocuments.mapNotNull { doc ->
                        doc.toObject(Review::class.java).copy(id = doc.id)
                    }
                    
                    reviewsAdapter.updateReviews(reviews)
                    
                    noReviewsText.visibility = if (reviews.isEmpty()) View.VISIBLE else View.GONE
                    recyclerView.visibility = if (reviews.isEmpty()) View.GONE else View.VISIBLE
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Fout bij ophalen reviews: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }

    private fun showAddAddressDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_address, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Get existing address if available
        val userId = auth.currentUser?.uid
        userId?.let { uid ->
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    val existingAddress = document.getString("address")
                    existingAddress?.let { address ->
                        // Parse existing address
                        val parts = address.split(",").map { it.trim() }
                        if (parts.size >= 3) {
                            val streetAndNumber = parts[0].split(" ")
                            val street = streetAndNumber.dropLast(1).joinToString(" ")
                            val number = streetAndNumber.last()
                            val city = parts[1]

                            dialogView.findViewById<TextInputEditText>(R.id.addressInput).setText(street)
                            dialogView.findViewById<TextInputEditText>(R.id.numberInput).setText(number)
                            dialogView.findViewById<TextInputEditText>(R.id.cityInput).setText(city)
                        }
                    }
                }
        }

        dialogView.findViewById<MaterialButton>(R.id.saveAddressButton).setOnClickListener {
            val straat = dialogView.findViewById<TextInputEditText>(R.id.addressInput).text.toString()
            val nummer = dialogView.findViewById<TextInputEditText>(R.id.numberInput).text.toString()
            val stad = dialogView.findViewById<TextInputEditText>(R.id.cityInput).text.toString()

            if (straat.isNotEmpty() && nummer.isNotEmpty() && stad.isNotEmpty()) {
                val fullAddress = "$straat $nummer, $stad, Belgium"
                saveAddressToFirebase(fullAddress)
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun saveAddressToFirebase(address: String) {
        val userId = auth.currentUser?.uid
        userId?.let {
            val userRef = FirebaseFirestore.getInstance().collection("users").document(it)
            userRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    // Update existing document
                    userRef.update("address", address)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Address saved successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to save address: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Create new document
                    val userData = mapOf("address" to address)
                    userRef.set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Address saved successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to save address: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }

    private fun resetPassword() {
        val email = auth.currentUser?.email
        email?.let {
            auth.sendPasswordResetEmail(it)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Password reset email sent", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun showMyRentalsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_my_rentals, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.myRentalsRecyclerView)
        val noRentalsText = dialogView.findViewById<TextView>(R.id.noRentalsText)
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        val rentalsAdapter = MyRequestsAdapter(emptyList()) { product, rentalPeriod -> 
            // Hergebruik MyRequestsAdapter voor consistente weergave
            showRentalDetails(product, rentalPeriod)
        }
        recyclerView.adapter = rentalsAdapter

        // Laad verhuurde producten van de huidige gebruiker
        val currentUser = auth.currentUser?.uid
        currentUser?.let { uid ->
            FirebaseFirestore.getInstance().collection("products")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { documents ->
                    val rentals = mutableListOf<Pair<Product, RentalPeriod>>()
                    
                    for (doc in documents) {
                        val product = doc.toObject(Product::class.java).apply { id = doc.id }
                        val activeRentals = product.rentalPeriods.filter { period ->
                            period.status == "ACTIVE" || period.status == "PENDING" || 
                            period.status == "COMPLETED"
                        }
                        activeRentals.forEach { period ->
                            rentals.add(product to period)
                        }
                    }

                    if (rentals.isEmpty()) {
                        noRentalsText.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        noRentalsText.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        (recyclerView.adapter as MyRequestsAdapter).updateRequests(rentals)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Fout bij ophalen verhuuringen: ${e.message}", 
                        Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }

    private fun showRentalDetails(product: Product, rentalPeriod: RentalPeriod) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rental_details, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.apply {
            findViewById<TextView>(R.id.productTitle).text = product.title
            findViewById<TextView>(R.id.renterName).text = "Gehuurd door: ${rentalPeriod.rentedBy}"
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val period = "${dateFormat.format(rentalPeriod.startDate)} - ${dateFormat.format(rentalPeriod.endDate)}"
            findViewById<TextView>(R.id.rentalPeriod).text = "Periode: $period"
            
            val statusText = findViewById<TextView>(R.id.statusText)
            statusText.text = when(rentalPeriod.status) {
                "PENDING" -> "In aanvraag"
                "ACTIVE" -> "Actief"
                "COMPLETED" -> "Afgerond"
                else -> "Afgewezen"
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 