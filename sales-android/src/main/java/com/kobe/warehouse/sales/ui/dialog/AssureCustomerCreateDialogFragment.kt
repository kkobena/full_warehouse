package com.kobe.warehouse.sales.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.api.CustomerApiService
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.databinding.DialogAssureCustomerCreateBinding
import com.kobe.warehouse.sales.ui.adapter.AssureFormStepAdapter
import com.kobe.warehouse.sales.utils.ApiClient
import com.kobe.warehouse.sales.utils.TokenManager
import kotlinx.coroutines.launch

/**
 * Dialog for creating an insured customer (ASSURE type) with steps
 *
 * Based on: assure-form-step.component.ts
 *
 * Steps:
 * 1. Client principal info + Principal tiers payant
 * 2. Ayants droit (optional - can be skipped for MVP)
 *
 * Simplified mobile version with essential fields
 */
class AssureCustomerCreateDialogFragment : DialogFragment() {

    private var _binding: DialogAssureCustomerCreateBinding? = null
    private val binding get() = _binding!!

    private lateinit var customerRepository: CustomerRepository
    private lateinit var stepAdapter: AssureFormStepAdapter
    private var onCustomerCreated: ((Customer) -> Unit)? = null

    private var isSaving = false

    // Data collected from steps
    var customerData: CustomerFormData? = null
    var principalTiersPayantData: TiersPayantFormData? = null

    companion object {
        const val TAG = "AssureCustomerCreateDialog"

        fun newInstance(onCustomerCreated: (Customer) -> Unit): AssureCustomerCreateDialogFragment {
            return AssureCustomerCreateDialogFragment().apply {
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
        _binding = DialogAssureCustomerCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupViewPager()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = "Nouveau client assuré"
            setNavigationIcon(R.drawable.ic_close)
            setNavigationOnClickListener { dismiss() }
        }
    }

    private fun setupViewPager() {
        stepAdapter = AssureFormStepAdapter(this)
        binding.viewPager.adapter = stepAdapter
        binding.viewPager.isUserInputEnabled = false  // Disable swipe, use buttons only

        // Setup tab layout with ViewPager
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "1. Client & Assurance"
                1 -> "2. Ayants droit"
                else -> "Step ${position + 1}"
            }
        }.attach()

        // Listen to page changes
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateNavigationButtons(position)
            }
        })
    }

    private fun setupListeners() {
        binding.btnPrevious.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem > 0) {
                binding.viewPager.currentItem = currentItem - 1
            }
        }

        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (validateCurrentStep(currentItem)) {
                if (currentItem < stepAdapter.itemCount - 1) {
                    binding.viewPager.currentItem = currentItem + 1
                } else {
                    // Last step, save
                    saveCustomer()
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun updateNavigationButtons(position: Int) {
        binding.btnPrevious.isEnabled = position > 0
        binding.btnPrevious.visibility = if (position > 0) View.VISIBLE else View.GONE

        if (position == stepAdapter.itemCount - 1) {
            binding.btnNext.text = "Créer"
            binding.btnNext.setIconResource(R.drawable.ic_save)
        } else {
            binding.btnNext.text = "Suivant"
            binding.btnNext.setIconResource(R.drawable.ic_arrow_forward)
        }
    }

    private fun validateCurrentStep(position: Int): Boolean {
        return when (position) {
            0 -> validateStep1()
            1 -> validateStep2()
            else -> true
        }
    }

    private fun validateStep1(): Boolean {
        // Get fragment and validate
        val fragment = childFragmentManager.findFragmentByTag("f0") as? AssureStep1Fragment
        return fragment?.validateForm() ?: false
    }

    private fun validateStep2(): Boolean {
        // Ayants droit is optional, so always valid
        return true
    }

    private fun saveCustomer() {
        if (isSaving) return

        // Validate all data collected
        if (customerData == null || principalTiersPayantData == null) {
            Toast.makeText(requireContext(), "Données incomplètes", Toast.LENGTH_SHORT).show()
            return
        }

        isSaving = true
        binding.progressBar.visibility = View.VISIBLE
        binding.btnNext.isEnabled = false

        // Build customer object
        val customer = Customer(
            firstName = customerData!!.firstName,
            lastName = customerData!!.lastName,
            phone = customerData!!.phone,
            email = customerData!!.email,
            sexe = customerData!!.sexe,
            datNaiss = customerData!!.datNaiss,
            type = "ASSURE",
            // Principal tiers payant
            tiersPayantId = principalTiersPayantData!!.tiersPayantId,
            num = principalTiersPayantData!!.num,
            taux = principalTiersPayantData!!.taux
        )

        lifecycleScope.launch {
            customerRepository.createAssureCustomer(customer).fold(
                onSuccess = { createdCustomer ->
                    isSaving = false
                    binding.progressBar.visibility = View.GONE
                    binding.btnNext.isEnabled = true

                    Toast.makeText(
                        requireContext(),
                        "Client assuré créé avec succès",
                        Toast.LENGTH_SHORT
                    ).show()

                    onCustomerCreated?.invoke(createdCustomer)
                    dismiss()
                },
                onFailure = { error ->
                    isSaving = false
                    binding.progressBar.visibility = View.GONE
                    binding.btnNext.isEnabled = true

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

    /**
     * Data class for customer form data from Step 1
     */
    data class CustomerFormData(
        val firstName: String,
        val lastName: String,
        val phone: String?,
        val email: String?,
        val sexe: String?,
        val datNaiss: String?
    )

    /**
     * Data class for tiers payant form data from Step 1
     */
    data class TiersPayantFormData(
        val tiersPayantId: Long,
        val tiersPayantName: String,
        val num: String,
        val taux: Int
    )
}
