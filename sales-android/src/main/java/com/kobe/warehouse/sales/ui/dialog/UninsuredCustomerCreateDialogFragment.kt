package com.kobe.warehouse.sales.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.api.CustomerApiService
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.databinding.DialogUninsuredCustomerCreateBinding
import com.kobe.warehouse.sales.utils.ApiClient
import com.kobe.warehouse.sales.utils.TokenManager
import kotlinx.coroutines.launch

/**
 * Dialog for creating an uninsured customer (STANDARD type)
 * Used for Comptant sales when customer is required (deferred payment, credit)
 *
 * Based on: uninsured-customer-form.component.ts
 *
 * Required fields:
 * - firstName
 * - lastName
 * - phone
 *
 * Optional fields:
 * - email
 *
 * Customer type: STANDARD
 */
class UninsuredCustomerCreateDialogFragment : DialogFragment() {

    private var _binding: DialogUninsuredCustomerCreateBinding? = null
    private val binding get() = _binding!!

    private lateinit var customerRepository: CustomerRepository
    private var onCustomerCreated: ((Customer) -> Unit)? = null

    private var isSaving = false

    companion object {
        const val TAG = "UninsuredCustomerCreateDialog"

        fun newInstance(onCustomerCreated: (Customer) -> Unit): UninsuredCustomerCreateDialogFragment {
            return UninsuredCustomerCreateDialogFragment().apply {
                this.onCustomerCreated = onCustomerCreated
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_PharmaSmart_Dialog_FullScreen)

        // Initialize repository
        val tokenManager = TokenManager(requireContext())
        val apiClient = ApiClient.create(tokenManager = tokenManager)
        val customerApi = apiClient.create(CustomerApiService::class.java)
        customerRepository = CustomerRepository(customerApi)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogUninsuredCustomerCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupValidation()
        setupListeners()
        setupAutoFocus()
    }

    /**
     * Auto-focus on first required field (Nom) when dialog opens
     */
    private fun setupAutoFocus() {
        binding.etFirstName.post {
            binding.etFirstName.requestFocus()
            // Show keyboard
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(binding.etFirstName, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun setupUI() {
        binding.toolbar.apply {
            title = "Nouveau client"
            setNavigationIcon(R.drawable.ic_close)
            setNavigationOnClickListener { dismiss() }
        }
    }

    private fun setupValidation() {
        // Real-time validation
        binding.etFirstName.addTextChangedListener {
            validateFirstName()
        }

        binding.etLastName.addTextChangedListener {
            validateLastName()
        }

        binding.etPhone.addTextChangedListener {
            validatePhone()
        }

        binding.etEmail.addTextChangedListener {
            validateEmail()
        }
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            if (validateForm()) {
                saveCustomer()
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun validateFirstName(): Boolean {
        val firstName = binding.etFirstName.text.toString().trim()
        return if (firstName.isEmpty()) {
            binding.tilFirstName.error = "Prénom obligatoire"
            false
        } else {
            binding.tilFirstName.error = null
            true
        }
    }

    private fun validateLastName(): Boolean {
        val lastName = binding.etLastName.text.toString().trim()
        return if (lastName.isEmpty()) {
            binding.tilLastName.error = "Nom obligatoire"
            false
        } else {
            binding.tilLastName.error = null
            true
        }
    }

    private fun validatePhone(): Boolean {
        val phone = binding.etPhone.text.toString().trim()
        return if (phone.isEmpty()) {
            binding.tilPhone.error = "Téléphone obligatoire"
            false
        } else {
            binding.tilPhone.error = null
            true
        }
    }

    private fun validateEmail(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        if (email.isEmpty()) {
            binding.tilEmail.error = null
            return true  // Email is optional
        }

        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
        return if (!emailPattern.matcher(email).matches()) {
            binding.tilEmail.error = "Email invalide"
            false
        } else {
            binding.tilEmail.error = null
            true
        }
    }

    private fun validateForm(): Boolean {
        val isFirstNameValid = validateFirstName()
        val isLastNameValid = validateLastName()
        val isPhoneValid = validatePhone()
        val isEmailValid = validateEmail()

        return isFirstNameValid && isLastNameValid && isPhoneValid && isEmailValid
    }

    private fun saveCustomer() {
        if (isSaving) return

        isSaving = true
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        val customer = Customer(
            firstName = binding.etFirstName.text.toString().trim(),
            lastName = binding.etLastName.text.toString().trim(),
            phone = binding.etPhone.text.toString().trim(),
            email = binding.etEmail.text.toString().trim().ifEmpty { null },
            type = "STANDARD"
        )

        lifecycleScope.launch {
            customerRepository.createUninsuredCustomer(customer).fold(
                onSuccess = { createdCustomer ->
                    isSaving = false
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    onCustomerCreated?.invoke(createdCustomer)
                    dismiss()
                },
                onFailure = { error ->
                    isSaving = false
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true

                    Toast.makeText(
                        requireContext(),
                        "Erreur: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
