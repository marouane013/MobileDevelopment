package com.example.verhuur_app.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import androidx.fragment.app.Fragment
import java.util.Calendar

class CalendarFragment : Fragment() {
    private var isStartDate: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return CalendarView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            minDate = Calendar.getInstance().timeInMillis

            setOnDateChangeListener { _, year, month, day ->
                val parentDialog = parentFragment as? RentProductDialog
                if (isStartDate) {
                    parentDialog?.updateStartDate(year, month, day)
                } else {
                    parentDialog?.updateEndDate(year, month, day)
                }
            }
        }
    }

    companion object {
        fun newInstance(isStartDate: Boolean): CalendarFragment {
            return CalendarFragment().apply {
                this.isStartDate = isStartDate
            }
        }
    }
} 