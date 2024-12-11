package com.example.verhuur_app.ui.addproduct

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.verhuur_app.R
import com.example.verhuur_app.databinding.FragmentAddProductBinding
import com.example.verhuur_app.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AddProductFragment : Fragment() {
    private var _binding: FragmentAddProductBinding? = null
    private val binding get() = _binding!!
    private lateinit var currentPhotoPath: String
    private val categories = listOf("Gereedschap", "Tuin", "Keuken", "Elektronica", "Sport", "Schoonmaak")
    private var productId: String? = null
    private var isEditing = false

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            // Laad de gemaakte foto
            val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
            binding.productImageView.setImageBitmap(bitmap)
        }
    }

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            takePhoto()
        } else {
            // Toon een bericht dat camera permissie nodig is
            Toast.makeText(context, "Camera permissie is nodig om foto's te maken", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Check of we in edit mode zijn
        productId = arguments?.getString("productId")
        isEditing = arguments?.getBoolean("isEditing") ?: false
        
        if (isEditing) {
            binding.saveButton.text = "Product bijwerken"
            loadProductDetails()
        }
        
        setupCategoryDropdown()
        setupClickListeners()
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item, categories)
        (binding.categoryAutoComplete as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.takePictureButton.setOnClickListener {
            requestPermission.launch(android.Manifest.permission.CAMERA)
        }

        binding.saveButton.setOnClickListener {
            saveProduct()
        }
    }

    private fun takePhoto() {
        val photoFile = createImageFile()
        photoFile.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                it
            )
            takePicture.launch(photoURI)
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun saveProduct() {
        val title = binding.titleEditText.text.toString()
        val description = binding.descriptionEditText.text.toString()
        val price = binding.priceEditText.text.toString().toDoubleOrNull()
        val category = binding.categoryAutoComplete.text.toString()
        
        // Nieuwe adres opbouw
        val street = binding.streetEditText.text.toString()
        val houseNumber = binding.houseNumberEditText.text.toString()
        val city = binding.cityEditText.text.toString()
        // Voeg België automatisch toe
        val fullAddress = "$street $houseNumber, $city, Belgium"

        if (validateInput(title, description, price, category, street, houseNumber, city)) {
            val product = Product(
                title = title,
                description = description,
                price = price!!,
                category = category,
                address = fullAddress,  // Gebruik het volledige adres
                imageUrl = currentPhotoPath,
                userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            )
            saveProductToDatabase(product)
        }
    }

    private fun validateInput(
        title: String, 
        description: String, 
        price: Double?, 
        category: String, 
        street: String,
        houseNumber: String,
        city: String
    ): Boolean {
        if (title.isEmpty()) {
            binding.titleEditText.error = "Vul een titel in"
            return false
        }
        if (description.isEmpty()) {
            binding.descriptionEditText.error = "Vul een beschrijving in"
            return false
        }
        if (price == null) {
            binding.priceEditText.error = "Vul een geldige prijs in"
            return false
        }
        if (category.isEmpty() || category !in categories) {
            binding.categoryAutoComplete.error = "Kies een categorie"
            return false
        }
        if (street.isEmpty()) {
            binding.streetEditText.error = "Vul een straat in"
            return false
        }
        if (houseNumber.isEmpty()) {
            binding.houseNumberEditText.error = "Vul een huisnummer in"
            return false
        }
        if (city.isEmpty()) {
            binding.cityEditText.error = "Vul een stad in"
            return false
        }
        if (!::currentPhotoPath.isInitialized) {
            Toast.makeText(context, "Maak eerst een foto", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveProductToDatabase(product: Product) {
        val db = FirebaseFirestore.getInstance()
        
        if (isEditing && productId != null) {
            // Update existing product
            db.collection("products")
                .document(productId!!)
                .set(product)
                .addOnSuccessListener {
                    Toast.makeText(context, "Product bijgewerkt", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Fout bij bijwerken: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Add new product
            db.collection("products")
                .add(product)
                .addOnSuccessListener {
                    Toast.makeText(context, "Product toegevoegd", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Fout bij opslaan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadProductDetails() {
        productId?.let { id ->
            FirebaseFirestore.getInstance().collection("products")
                .document(id)
                .get()
                .addOnSuccessListener { document ->
                    val product = document.toObject(Product::class.java)
                    product?.let { 
                        binding.apply {
                            titleEditText.setText(it.title)
                            descriptionEditText.setText(it.description)
                            priceEditText.setText(it.price.toString())
                            categoryAutoComplete.setText(it.category)
                            // Split address
                            val addressParts = it.address.split(",")
                            if (addressParts.size >= 3) {
                                streetEditText.setText(addressParts[0].trim())
                                houseNumberEditText.setText(addressParts[1].trim())
                                cityEditText.setText(addressParts[2].trim())
                            }
                            // Load image
                            if (it.imageUrl.isNotEmpty()) {
                                currentPhotoPath = it.imageUrl
                                val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                                productImageView.setImageBitmap(bitmap)
                            }
                        }
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 