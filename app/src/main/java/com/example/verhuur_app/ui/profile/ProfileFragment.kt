package com.example.verhuur_app.ui.profile

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

            dialogView.findViewById<MaterialButton>(R.id.addAddressButton).setOnClickListener {
                // Handle add address logic
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

    private fun showAddAddressDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_address, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 