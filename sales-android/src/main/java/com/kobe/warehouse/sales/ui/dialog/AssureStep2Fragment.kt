package com.kobe.warehouse.sales.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kobe.warehouse.sales.databinding.FragmentAssureStep2Binding

/**
 * Step 2 of Assure Customer Creation
 * Ayants droit (beneficiaries) - Optional
 *
 * Simplified MVP version: This step can be skipped
 * Full implementation will allow adding beneficiaries
 */
class AssureStep2Fragment : Fragment() {

    private var _binding: FragmentAssureStep2Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssureStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // MVP: No implementation yet
        // Future: Allow adding ayants droit (beneficiaries)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
