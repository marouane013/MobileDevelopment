package com.example.verhuur_app.ui.base

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

open class BaseFragment : Fragment() {
    // Common navigation method that can be used by all fragments
    protected fun navigateTo(destinationId: Int) {
        findNavController().navigate(destinationId)
    }
} 