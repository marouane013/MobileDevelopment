package com.example.verhuur_app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.verhuur_app.LoginActivity
import com.example.verhuur_app.databinding.FragmentProfileBinding
import com.example.verhuur_app.ui.base.BaseFragment
import com.google.firebase.auth.FirebaseAuth

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 