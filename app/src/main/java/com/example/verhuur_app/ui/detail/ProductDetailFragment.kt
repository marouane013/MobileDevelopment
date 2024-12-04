package com.example.verhuur_app.ui.detail

import android.content.Context
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.verhuur_app.R
import com.example.verhuur_app.databinding.FragmentProductDetailBinding
import com.example.verhuur_app.ui.base.BaseFragment
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast
import com.example.verhuur_app.model.RentalPeriod
import com.example.verhuur_app.model.RentalStatus
import com.example.verhuur_app.ui.dialogs.RentProductDialog
import java.util.*

class ProductDetailFragment : BaseFragment() {
    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private var mapView: MapView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup back button
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        // Setup OSM configuration
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osm", Context.MODE_PRIVATE)
        )
        
        // Initialize MapView
        mapView = binding.mapView
        mapView?.setTileSource(TileSourceFactory.MAPNIK)
        mapView?.setMultiTouchControls(true)
        
        // Load product data
        arguments?.let { args ->
            binding.apply {
                productTitle.text = args.getString("title")
                productDescription.text = args.getString("description")
                productPrice.text = "€${args.getString("price")}"
                productAddress.text = args.getString("address")
                
                // Load image using Glide
                args.getString("imageUrl")?.let { url ->
                    Glide.with(requireContext())
                        .load(url)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(productImage)
                }

                // Get location from address
                args.getString("address")?.let { address ->
                    getLocationFromAddress(address)
                }
            }
        }

        binding.rentButton.setOnClickListener {
            showRentDialog()
        }

        arguments?.getString("productId")?.let { productId ->
            val db = FirebaseFirestore.getInstance()
            db.collection("products").document(productId)
                .get()
                .addOnSuccessListener { document ->
                    // Bestaande product details laden
                    
                    // Update status
                    val rentalPeriods = document.get("rentalPeriods") as? List<Map<String, Any>> ?: listOf()
                    updateProductStatus(rentalPeriods)
                }
        }
    }

    private fun getLocationFromAddress(address: String) {
        val geocoder = Geocoder(requireContext())
        try {
            val addresses = geocoder.getFromLocationName(address, 1)
            if (addresses?.isNotEmpty() == true) {
                val location = addresses[0]
                updateMapLocation(location.latitude, location.longitude)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateMapLocation(latitude: Double, longitude: Double) {
        mapView?.apply {
            val mapController = controller
            val startPoint = GeoPoint(latitude, longitude)
            
            // Add marker
            val marker = Marker(this)
            marker.position = startPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            overlays.add(marker)
            
            // Move to location
            mapController.setZoom(15.0)
            mapController.setCenter(startPoint)
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showRentDialog() {
        val dialog = RentProductDialog()
        dialog.setOnRentConfirmedListener { startDate, endDate ->
            // Check beschikbaarheid voordat we het huurverzoek versturen
            arguments?.getString("productId")?.let { productId ->
                checkAvailability(productId, startDate, endDate)
            }
        }
        dialog.show(childFragmentManager, "RentDialog")
    }

    private fun checkAvailability(productId: String, startDate: Date, endDate: Date) {
        val db = FirebaseFirestore.getInstance()
        db.collection("products").document(productId)
            .get()
            .addOnSuccessListener { document ->
                val rentalPeriods = document.get("rentalPeriods") as? List<Map<String, Any>> ?: listOf()
                
                // Controleer of er overlappende actieve huurperiodes zijn
                val isAvailable = rentalPeriods.none { period ->
                    val periodStartDate = (period["startDate"] as com.google.firebase.Timestamp).toDate()
                    val periodEndDate = (period["endDate"] as com.google.firebase.Timestamp).toDate()
                    val status = period["status"] as? String
                    
                    status == RentalStatus.ACTIVE.name &&
                    startDate.before(periodEndDate) && endDate.after(periodStartDate)
                }
                
                if (isAvailable) {
                    rentProduct(startDate, endDate)
                } else {
                    Toast.makeText(context, "Product is niet beschikbaar in deze periode", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun rentProduct(startDate: Date, endDate: Date) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Je moet ingelogd zijn om te kunnen huren", Toast.LENGTH_SHORT).show()
            return
        }

        arguments?.getString("productId")?.let { productId ->
            val rentalPeriod = RentalPeriod(
                startDate = startDate,
                endDate = endDate,
                rentedBy = currentUser.uid,
                status = "PENDING"
            )

            val db = FirebaseFirestore.getInstance()
            db.collection("products").document(productId)
                .update("rentalPeriods", FieldValue.arrayUnion(rentalPeriod))
                .addOnSuccessListener {
                    Toast.makeText(context, "Huurverzoek verzonden", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateProductStatus(rentalPeriods: List<Map<String, Any>>) {
        val activeRental = rentalPeriods.find { period ->
            val status = period["status"] as? String
            status == RentalStatus.ACTIVE.name
        }
        
        binding.statusText.apply {
            when {
                activeRental != null -> {
                    text = "Verhuurd"
                    setBackgroundColor(resources.getColor(R.color.status_rented, null))
                }
                rentalPeriods.any { it["status"] as? String == RentalStatus.PENDING.name } -> {
                    text = "In aanvraag"
                    setBackgroundColor(resources.getColor(R.color.status_pending, null))
                }
                else -> {
                    text = "Beschikbaar"
                    setBackgroundColor(resources.getColor(R.color.status_available, null))
                }
            }
        }
        
        binding.rentButton.apply {
            isEnabled = activeRental == null
            alpha = if (isEnabled) 1.0f else 0.5f
        }
    }
} 