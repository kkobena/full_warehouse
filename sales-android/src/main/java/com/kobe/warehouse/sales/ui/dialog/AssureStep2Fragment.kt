package com.kobe.warehouse.sales.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.kobe.warehouse.sales.databinding.FragmentAssureStep2Binding
import com.kobe.warehouse.sales.utils.DateInputFormatter

/**
 * Step 2 of Assure Customer Creation
 * Ayants droit (beneficiaries) - Optional
 *
 * Fields (matching Angular ayant-droit-step.component.ts):
 * Required if filled: firstName, lastName, numAyantDroit
 * Optional: datNaiss, sexe
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
        setupUI()
        setupAutoFocus()
    }

    private fun setupUI() {
        val sexeOptions = arrayOf("Masculin", "Féminin")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sexeOptions)
        binding.actvSexe.setAdapter(adapter)

        DateInputFormatter.attach(binding.etDatNaiss)
    }

    private fun setupAutoFocus() {
        binding.etFirstName.post {
            binding.etFirstName.requestFocus()
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(binding.etFirstName, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    /**
     * Check if the form has any data filled.
     * If all required fields are empty, the step is considered skipped (valid).
     * If any required field is filled, all required fields must be valid.
     */
    fun validateForm(): Boolean {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val numAyantDroit = binding.etNumAyantDroit.text.toString().trim()

        val allEmpty = firstName.isEmpty() && lastName.isEmpty() && numAyantDroit.isEmpty()

        // If all empty, step is skipped — valid
        if (allEmpty) return true

        // If any is filled, all required fields must be filled
        var hasError = false

        if (firstName.isEmpty()) {
            binding.tilFirstName.error = "Nom obligatoire"
            hasError = true
        } else {
            binding.tilFirstName.error = null
        }

        if (lastName.isEmpty()) {
            binding.tilLastName.error = "Prénom obligatoire"
            hasError = true
        } else {
            binding.tilLastName.error = null
        }

        if (numAyantDroit.isEmpty()) {
            binding.tilNumAyantDroit.error = "Numéro assuré obligatoire"
            hasError = true
        } else {
            binding.tilNumAyantDroit.error = null
        }

        // Validate date not in the future
        val datNaissText = binding.etDatNaiss.text.toString().trim()
        if (DateInputFormatter.isFutureDate(datNaissText)) {
            binding.tilDatNaiss.error = "La date ne peut pas être dans le futur"
            hasError = true
        } else {
            binding.tilDatNaiss.error = null
        }

        return !hasError
    }

    /**
     * Returns true if the user has filled in the ayant droit form
     */
    fun hasData(): Boolean {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val numAyantDroit = binding.etNumAyantDroit.text.toString().trim()
        return firstName.isNotEmpty() || lastName.isNotEmpty() || numAyantDroit.isNotEmpty()
    }

    /**
     * Get ayant droit data collected from the form.
     * Returns null if no data was entered.
     */
    fun getAyantDroitData(): AyantDroitFormData? {
        if (!hasData()) return null

        val sexeText = binding.actvSexe.text.toString()
        val sexe = when (sexeText) {
            "Masculin" -> "M"
            "Féminin" -> "F"
            else -> null
        }

        return AyantDroitFormData(
            firstName = binding.etFirstName.text.toString().trim(),
            lastName = binding.etLastName.text.toString().trim(),
            numAyantDroit = binding.etNumAyantDroit.text.toString().trim(),
            datNaiss = DateInputFormatter.toIsoDate(binding.etDatNaiss.text.toString()),
            sexe = sexe
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class AyantDroitFormData(
        val firstName: String,
        val lastName: String,
        val numAyantDroit: String,
        val datNaiss: String?,
        val sexe: String?
    )
}
