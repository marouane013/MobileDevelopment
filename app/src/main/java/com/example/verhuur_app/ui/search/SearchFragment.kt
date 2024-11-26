package com.example.verhuur_app.ui.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.verhuur_app.databinding.FragmentSearchBinding
import com.example.verhuur_app.ui.base.BaseFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class SearchFragment : BaseFragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

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
        binding.searchEditText.setOnEditorActionListener { _, _, _ ->
            performSearch()
            true
        }
    }

    private fun performSearch() {
        // Implementeer zoeklogica
        // Update markers op de kaart
        // Update resultaten in bottom sheet
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