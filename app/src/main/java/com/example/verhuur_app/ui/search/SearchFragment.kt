package com.example.verhuur_app.ui.search

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.verhuur_app.databinding.FragmentSearchBinding
import com.example.verhuur_app.ui.base.BaseFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import com.google.firebase.firestore.FirebaseFirestore
import android.location.Geocoder
import com.example.verhuur_app.adapters.ProductAdapter
import java.io.IOException
import androidx.navigation.fragment.findNavController
import com.example.verhuur_app.R
import com.example.verhuur_app.model.Product
import com.google.firebase.firestore.Query
import android.view.inputmethod.EditorInfo
import android.util.Log
import java.util.*
import org.osmdroid.views.overlay.Polygon
import com.google.firebase.auth.FirebaseAuth

class SearchFragment : BaseFragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var productAdapter: ProductAdapter
    private var radiusOverlay: Polygon? = null
    private var currentRadius: Double = 5.0 // 5km default
    private lateinit var userLocation: GeoPoint

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMap()
        setupBottomSheet()
        setupSearch()
        loadUserLocation()
    }

    private fun setupMap() {
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
        }

    }

    private fun setupBottomSheet() {
        val bottomSheetView = binding.bottomSheet.root
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView)
        bottomSheetBehavior.apply {
            state = BottomSheetBehavior.STATE_COLLAPSED
            peekHeight = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)
        }
    }

    private fun setupSearch() {
        productAdapter = ProductAdapter { product ->
            val bundle = Bundle().apply {
                putString("productId", product.id)
            }
            findNavController().navigate(R.id.action_searchFragment_to_productDetailFragment, bundle)
        }

        binding.bottomSheet.productsRecyclerView.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(context)
        }

        binding.searchBar.searchEditText.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val searchText = textView.text.toString()
                searchLocationAndProducts(searchText)
                true
            } else {
                false
            }
        }

        setupCategoryChips()

        binding.searchBar.radiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentRadius = progress.toDouble()
                binding.searchBar.radiusText.text = "Afstand: $progress km"
                updateRadiusCircle()
                if (fromUser) {
                    searchNearbyProducts()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupCategoryChips() {
        val categories = listOf("Gereedschap", "Tuin", "Keuken", "Elektronica")
        categories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category
                isCheckable = true
            }
            binding.searchBar.categoryChipGroup.addView(chip)
        }
    }

    private fun searchProducts(query: String?) {
        val db = FirebaseFirestore.getInstance()
        var dbQuery: Query = db.collection("products")
        Log.d("SearchFragment", "Starting product search...")

        if (!query.isNullOrEmpty()) {
            dbQuery = dbQuery
                .whereGreaterThanOrEqualTo("title", query)
                .whereLessThanOrEqualTo("title", query + '\uf8ff')
        }

        val selectedCategories = binding.searchBar.categoryChipGroup
            .checkedChipIds
            .mapNotNull { id -> binding.searchBar.categoryChipGroup.findViewById<Chip>(id)?.text?.toString() }

        if (selectedCategories.isNotEmpty()) {
            dbQuery = dbQuery.whereIn("category", selectedCategories)
        }

        dbQuery.get()
            .addOnSuccessListener { documents ->
                binding.mapView.overlays.clear()
                Log.d("SearchFragment", "Found ${documents.size()} products")
                
                val productsList = documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java).apply {
                        id = doc.id
                        Log.d("SearchFragment", "Processing product: $title with address: $address")
                    }.also { product ->
                        addMarkerForProduct(product)
                    }
                }
                productAdapter.updateProducts(productsList)
                binding.mapView.invalidate()
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            }
            .addOnFailureListener { exception ->
                Log.e("SearchFragment", "Error searching products", exception)
                Toast.makeText(context, "Error searching products: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun addMarkerForProduct(product: Product) {
        val geocoder = Geocoder(requireContext())
        
        try {
            val addresses = geocoder.getFromLocationName(product.address, 1)
            
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val position = GeoPoint(address.latitude, address.longitude)
                
                val marker = Marker(binding.mapView).apply {
                    this.position = position
                    title = product.title
                    snippet = "€${product.price}/dag"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    
                    infoWindow = object : org.osmdroid.views.overlay.infowindow.InfoWindow(R.layout.marker_info_window, binding.mapView) {
                        override fun onOpen(item: Any?) {
                            closeAllInfoWindowsOn(binding.mapView)
                            val bundle = Bundle().apply {
                                putString("productId", product.id)
                            }
                            findNavController().navigate(
                                R.id.action_searchFragment_to_productDetailFragment,
                                bundle
                            )
                        }
                        override fun onClose() {}
                    }
                }
                
                marker.setOnMarkerClickListener { _, _ ->
                    marker.showInfoWindow()
                    true
                }
                
                binding.mapView.overlays.add(marker)
                Log.d("SearchFragment", "Added marker for ${product.title} at ${address.latitude}, ${address.longitude}")
            } else {
                Log.e("SearchFragment", "No location found for address: ${product.address}")
            }
        } catch (e: IOException) {
            Log.e("SearchFragment", "Error geocoding address: ${product.address}", e)
        }
    }

    

    private fun searchLocationAndProducts(searchText: String) {
        val geocoder = Geocoder(requireContext())
        
        try {
            val addresses = geocoder.getFromLocationName(searchText, 1)
            
            if (!addresses.isNullOrEmpty()) {
                val location = addresses[0]
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                
                // Verplaats de kaart naar de gezochte locatie
                binding.mapView.controller.animateTo(geoPoint)
                binding.mapView.controller.setZoom(15.0)
                
                // Zoek ook producten
                searchProducts(searchText)
            } else {
                // Als geen locatie gevonden is, zoek alleen producten
                searchProducts(searchText)
            }
        } catch (e: IOException) {
            Toast.makeText(context, "Kon locatie niet vinden: ${e.message}", Toast.LENGTH_SHORT).show()
            // Zoek wel nog steeds producten
            searchProducts(searchText)
        }
    }

    private fun isProductAvailable(product: Product, startDate: Date, endDate: Date): Boolean {
        return product.rentalPeriods.none { rentalPeriod ->
            (startDate.before(rentalPeriod.endDate) && endDate.after(rentalPeriod.startDate))
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadUserLocation() {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                
                val address = document.getString("address")
                if (address.isNullOrEmpty()) {
                    Toast.makeText(context, "Geen adres gevonden voor gebruiker", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                
                try {
                    val geocoder = Geocoder(requireContext(), Locale.getDefault())
                    val addresses = geocoder.getFromLocationName(address, 1)
                    
                    if (!addresses.isNullOrEmpty()) {
                        val location = addresses[0]
                        userLocation = GeoPoint(location.latitude, location.longitude)
                        
                        binding.mapView.controller.apply {
                            setCenter(userLocation)
                            setZoom(13.0)
                        }
                        
                        updateRadiusCircle()
                        searchNearbyProducts()
                    } else {
                        Toast.makeText(context, "Kon adres niet vinden op de kaart", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: IOException) {
                    Log.e("SearchFragment", "Geocoding error", e)
                    Toast.makeText(context, "Fout bij ophalen locatie: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener
                Log.e("SearchFragment", "Firestore error", e)
                Toast.makeText(context, "Fout bij ophalen gebruiker: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRadiusCircle() {
        if (!::userLocation.isInitialized) return
        
        // Verwijder bestaande radius overlay
        radiusOverlay?.let {
            binding.mapView.overlays.remove(it)
        }
        
        // Maak nieuwe cirkel punten
        val circlePoints = createCirclePoints(userLocation, currentRadius)
        
        // Maak nieuwe radius overlay
        radiusOverlay = Polygon().apply {
            points = circlePoints
            fillColor = 0x300000FF // Semi-transparant blauw
            strokeColor = 0xFF0000FF.toInt() // Blauw
            strokeWidth = 2f
        }
        
        binding.mapView.overlays.add(radiusOverlay)
        binding.mapView.invalidate()
    }

    private fun searchNearbyProducts() {
        val db = FirebaseFirestore.getInstance()
        db.collection("products")
            .get()
            .addOnSuccessListener { documents ->
                // Clear existing markers
                binding.mapView.overlays.clear()
                
                val nearbyProducts = documents.mapNotNull { doc ->
                    val product = doc.toObject(Product::class.java).apply { id = doc.id }
                    
                    // Check of product binnen straal valt
                    val geocoder = Geocoder(requireContext())
                    try {
                        val addresses = geocoder.getFromLocationName(product.address, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val productLocation = addresses[0]
                            val distance = calculateDistance(
                                userLocation.latitude, userLocation.longitude,
                                productLocation.latitude, productLocation.longitude
                            )
                            if (distance <= currentRadius) {
                                // Add marker for nearby product
                                addMarkerForProduct(product)
                                product
                            } else null
                        } else null
                    } catch (e: IOException) {
                        null
                    }
                }
                
                productAdapter.updateProducts(nearbyProducts)
                updateRadiusCircle() // Voeg de radius cirkel weer toe
                binding.mapView.invalidate() // Ververs de kaart
            }
    }

    private fun createCirclePoints(center: GeoPoint, radiusKm: Double): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        val numPoints = 60 // Aantal punten voor een vloeiende cirkel
        val radiusInMeters = radiusKm * 1000
        
        for (i in 0 until numPoints) {
            val bearing = (360.0 * i) / numPoints
            val point = calculatePointFromBearing(center, bearing, radiusInMeters)
            points.add(point)
        }
        points.add(points.first()) // Sluit de cirkel

        return points
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Radius van de aarde in km
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon/2) * Math.sin(dLon/2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
        
        return earthRadius * c // Afstand in km
    }

    private fun calculatePointFromBearing(center: GeoPoint, bearing: Double, distance: Double): GeoPoint {
        val earthRadius = 6371000.0 // Aarde radius in meters
        val bearingRad = Math.toRadians(bearing)
        val centerLatRad = Math.toRadians(center.latitude)
        val centerLonRad = Math.toRadians(center.longitude)
        val distRatio = distance / earthRadius
        
        val newLatRad = Math.asin(
            Math.sin(centerLatRad) * Math.cos(distRatio) +
            Math.cos(centerLatRad) * Math.sin(distRatio) * Math.cos(bearingRad)
        )
        
        val newLonRad = centerLonRad + Math.atan2(
            Math.sin(bearingRad) * Math.sin(distRatio) * Math.cos(centerLatRad),
            Math.cos(distRatio) - Math.sin(centerLatRad) * Math.sin(newLatRad)
        )
        
        return GeoPoint(
            Math.toDegrees(newLatRad),
            Math.toDegrees(newLonRad)
        )
    }
} 