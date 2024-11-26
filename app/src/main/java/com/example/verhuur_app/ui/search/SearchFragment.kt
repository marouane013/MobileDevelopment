package com.example.verhuur_app.ui.search

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class SearchFragment : BaseFragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var productAdapter: ProductAdapter

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
        setupCategoryChips()
        setupSearch()
        setupRecyclerView()
    }

    private fun setupMap() {
        // OSM configuratie
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            
            // Centreer de kaart op een standaard locatie (bijvoorbeeld Amsterdam)
            controller.setCenter(GeoPoint(52.3676, 4.9041))
        }

        // Voeg zoekfunctionaliteit toe
        binding.searchEditText.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val searchText = textView.text.toString()
                searchLocationAndProducts(searchText)
                true
            } else {
                false
            }
        }

        // Voorbeeld: voeg markers toe voor producten
        addSampleMarkers()
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun setupCategoryChips() {
        val categories = listOf("Gereedschap", "Tuin", "Keuken", "Elektronica")
        categories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category
                isCheckable = true
            }
            binding.categoryChipGroup.addView(chip)
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchProducts(s?.toString())
            }
        })
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
            findNavController().navigate(R.id.action_searchFragment_to_productDetailFragment, bundle)
        }
        
        binding.productsRecyclerView.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun searchProducts(query: String?) {
        val db = FirebaseFirestore.getInstance()
        var dbQuery: Query = db.collection("products")

        // Voeg filters toe op basis van de zoekterm en geselecteerde categorieën
        if (!query.isNullOrEmpty()) {
            dbQuery = dbQuery
                .whereGreaterThanOrEqualTo("title", query)
                .whereLessThanOrEqualTo("title", query + '\uf8ff')
        }

        // Haal geselecteerde categorieën op
        val selectedCategories = binding.categoryChipGroup
            .checkedChipIds
            .mapNotNull { id -> binding.categoryChipGroup.findViewById<Chip>(id)?.text?.toString() }

        // Als er categorieën zijn geselecteerd, filter daarop
        if (selectedCategories.isNotEmpty()) {
            dbQuery = dbQuery.whereIn("category", selectedCategories)
        }

        // Voer de query uit
        dbQuery.get()
            .addOnSuccessListener { documents ->
                // Clear existing markers first
                binding.mapView.overlays.clear()
                
                val productsList = documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java).apply {
                        // Zorg ervoor dat het document ID wordt toegewezen
                        id = doc.id
                    }.also { product ->
                        // Voeg marker toe voor elk product
                        addMarkerForProduct(product)
                    }
                }
                productAdapter.updateProducts(productsList)
                binding.mapView.invalidate() // Ververs de kaart
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error searching products: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun addMarkerForProduct(product: Product) {
        val geocoder = Geocoder(requireContext())
        
        try {
            // Voeg land toe aan het adres voor betere resultaten
            val fullAddress = "${product.address}, Belgium" // of ", Nederland" afhankelijk van je locatie
            val addresses = geocoder.getFromLocationName(fullAddress, 1)
            
            if (!addresses.isNullOrEmpty()) {
                val location = addresses[0]
                val marker = Marker(binding.mapView).apply {
                    position = GeoPoint(location.latitude, location.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = product.title
                    snippet = "€${product.price}"
                    
                    setOnMarkerClickListener { marker, mapView ->
                        val bundle = Bundle().apply {
                            putString("productId", product.id)
                            putString("title", product.title)
                            putString("description", product.description)
                            putString("price", product.price.toString())
                            putString("address", product.address)
                            putString("imageUrl", product.imageUrl)
                        }
                        findNavController().navigate(R.id.action_searchFragment_to_productDetailFragment, bundle)
                        true
                    }
                }
                binding.mapView.overlays.add(marker)
            }
        } catch (e: IOException) {
            Log.e("SearchFragment", "Error geocoding address: ${product.address}", e)
        }
    }

    private fun addSampleMarkers() {
        val sampleLocations = listOf(
            Triple(52.3676, 4.9041, "Boormachine"),
            Triple(52.3706, 4.9041, "Grasmaaier"),
            Triple(52.3676, 4.9081, "Keukenmixer")
        )

        sampleLocations.forEach { (lat, lon, title) ->
            val marker = Marker(binding.mapView).apply {
                position = GeoPoint(lat, lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                this.title = title
            }
            binding.mapView.overlays.add(marker)
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
} 