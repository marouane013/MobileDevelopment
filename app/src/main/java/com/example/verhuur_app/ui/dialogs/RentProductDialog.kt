package com.example.verhuur_app.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.verhuur_app.databinding.DialogRentProductBinding
import com.google.android.material.tabs.TabLayoutMediator
import java.util.Calendar
import java.util.Date

class RentProductDialog : DialogFragment() {
    private var _binding: DialogRentProductBinding? = null
    private val binding get() = _binding!!
    private lateinit var startDate: Calendar
    private lateinit var endDate: Calendar
    
    private var onRentConfirmed: ((Date, Date) -> Unit)? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRentProductBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        startDate = Calendar.getInstance()
        endDate = Calendar.getInstance()
        
        setupViewPager()
        setupButtons()
    }
    
    private fun setupViewPager() {
        val pagerAdapter = DatePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "Start datum" else "Eind datum"
        }.attach()
    }
    
    private fun setupButtons() {
        binding.confirmButton.setOnClickListener {
            if (validateDates()) {
                onRentConfirmed?.invoke(startDate.time, endDate.time)
                dismiss()
            }
        }
        
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }
    
    private fun validateDates(): Boolean {
        if (endDate.before(startDate)) {
            Toast.makeText(context, "Einddatum moet na startdatum liggen", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
    
    fun setOnRentConfirmedListener(listener: (Date, Date) -> Unit) {
        onRentConfirmed = listener
    }

    fun updateStartDate(year: Int, month: Int, day: Int) {
        startDate.set(year, month, day)
        binding.viewPager.currentItem = 1 // Switch to end date tab
    }

    fun updateEndDate(year: Int, month: Int, day: Int) {
        endDate.set(year, month, day)
    }
}

class DatePagerAdapter(fragment: DialogFragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2
    
    override fun createFragment(position: Int): androidx.fragment.app.Fragment {
        return CalendarFragment.newInstance(position == 0)
    }
} 