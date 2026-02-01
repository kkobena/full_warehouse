package com.kobe.warehouse.sales.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.api.CustomerApiService
import com.kobe.warehouse.sales.data.api.TiersPayantApiService
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.data.model.TiersPayant
import com.kobe.warehouse.sales.data.repository.CustomerRepository
import com.kobe.warehouse.sales.data.repository.TiersPayantRepository
import com.kobe.warehouse.sales.databinding.DialogCarnetCustomerCreateBinding
import com.kobe.warehouse.sales.utils.ApiClient
import com.kobe.warehouse.sales.utils.TokenManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dialog for creating a Carnet customer
 *
 * Based on UX spec from expert_ux.agent.md (lines 190-207)
 *
 * Required fields:
 * - firstName
 * - lastName
 * - tiersPayant
 * - num (matricule)
 * - taux (coverage rate)
 *
 * Optional fields:
 * - phone
 * - dateNaiss
 */
class CarnetCustomerCreateDialogFragment : DialogFragment() {

    private var _binding: DialogCarnetCustomerCreateBinding? = null
    private val binding get() = _binding!!

    private lateinit var customerRepository: CustomerRepository
    private lateinit var tiersPayantRepository: TiersPayantRepository
    private var onCustomerCreated: ((Customer) -> Unit)? = null

    private var searchJob: Job? = null
    private var selectedTiersPayant: com.kobe.warehouse.sales.data.model.TiersPayant? = null
    private val tiersPayantsList = mutableListOf<com.kobe.warehouse.sales.data.model.TiersPayant>()

    companion object {
        const val TAG = "CarnetCustomerCreateDialog"

        fun newInstance(onCustomerCreated: (Customer) -> Unit): CarnetCustomerCreateDialogFragment {
            return CarnetCustomerCreateDialogFragment().apply {
                this.onCustomerCreated = onCustomerCreated
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_PharmaSmart_Dialog_FullScreen)

        // Initialize repositories
        val tokenManager = TokenManager(requireContext())
        val apiClient = ApiClient.create(tokenManager = tokenManager)
        val customerApi = apiClient.create(CustomerApiService::class.java)
        val tiersPayantApi = apiClient.create(TiersPayantApiService::class.java)
        customerRepository = CustomerRepository(customerApi)
        tiersPayantRepository = TiersPayantRepository(tiersPayantApi)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCarnetCustomerCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupTiersPayantAutoComplete()
        setupListeners()
        setupAutoFocus()
    }

    /**
     * Auto-focus on first required field (Nom) when dialog opens
     */
    private fun setupAutoFocus() {
        binding.etLastName.post {
            binding.etLastName.requestFocus()
            // Show keyboard
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(binding.etLastName, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = "Nouveau client Carnet"
            setNavigationIcon(R.drawable.ic_close)
            setNavigationOnClickListener { dismiss() }
        }
    }

    private fun setupTiersPayantAutoComplete() {
        // Auto-complete for tiers payant search
        binding.actvTiersPayant.addTextChangedListener { text ->
            searchJob?.cancel()
            val query = text.toString().trim()

            if (query.length >= 2) {
                searchJob = lifecycleScope.launch {
                    delay(300) // Debounce
                    searchTiersPayants(query)
                }
            }
        }

        binding.actvTiersPayant.setOnItemClickListener { _, _, position, _ ->
            selectedTiersPayant = tiersPayantsList[position]
            // Auto-fill taux if available from tiers payant default
            selectedTiersPayant?.let { tp ->
                // Taux could be pre-filled if tiers payant has a default rate
                // For now, leave empty for user input
            }
        }
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            validateAndSave()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Create tiers payant button
        binding.btnCreateTiersPayant.setOnClickListener {
            openTiersPayantCreateDialog()
        }
    }

    private fun searchTiersPayants(query: String) {
        lifecycleScope.launch {
            tiersPayantRepository.searchTiersPayants(query).fold(
                onSuccess = { results ->
                    tiersPayantsList.clear()
                    tiersPayantsList.addAll(results)

                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        results.map { it.name } // domain.model.TiersPayant has only 'name'
                    )
                    binding.actvTiersPayant.setAdapter(adapter)
                    binding.actvTiersPayant.showDropDown()
                },
                onFailure = { error ->
                    Toast.makeText(
                        requireContext(),
                        "Erreur recherche tiers payant: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun openTiersPayantCreateDialog() {
        val dialog = TiersPayantCreateDialogFragment.newInstance { createdTiersPayant ->
            // Tiers payant created, use it directly
            selectedTiersPayant = createdTiersPayant
            binding.actvTiersPayant.setText(createdTiersPayant.name, false)
        }
        dialog.show(parentFragmentManager, TiersPayantCreateDialogFragment.TAG)
    }

    private fun validateAndSave() {
        // Validate required fields
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val num = binding.etNum.text.toString().trim()
        val tauxText = binding.etTaux.text.toString().trim()

        // Validation
        var hasError = false

        if (firstName.isEmpty()) {
            binding.tilFirstName.error = "Prénom requis"
            hasError = true
        } else {
            binding.tilFirstName.error = null
        }

        if (lastName.isEmpty()) {
            binding.tilLastName.error = "Nom requis"
            hasError = true
        } else {
            binding.tilLastName.error = null
        }

        if (selectedTiersPayant == null) {
            binding.tilTiersPayant.error = "Tiers payant requis"
            hasError = true
        } else {
            binding.tilTiersPayant.error = null
        }

        if (num.isEmpty()) {
            binding.tilNum.error = "Numéro matricule requis"
            hasError = true
        } else {
            binding.tilNum.error = null
        }

        val taux = tauxText.toIntOrNull()
        if (taux == null || taux < 0 || taux > 100) {
            binding.tilTaux.error = "Taux invalide (0-100)"
            hasError = true
        } else {
            binding.tilTaux.error = null
        }

        if (hasError) {
            return
        }

        // Optional fields
        val phone = binding.etPhone.text.toString().trim().ifEmpty { null }
        val dateNaiss = binding.etDateNaiss.text.toString().trim().ifEmpty { null }

        // Create customer
        val tiersPayantId = selectedTiersPayant!!.id ?: run {
            Toast.makeText(requireContext(), "Erreur: ID tiers payant invalide", Toast.LENGTH_SHORT).show()
            return
        }

        createCarnetCustomer(
            firstName = firstName,
            lastName = lastName,
            tiersPayantId = tiersPayantId,
            num = num,
            taux = taux!!,
            phone = phone,
            dateNaiss = dateNaiss
        )
    }

    private fun createCarnetCustomer(
        firstName: String,
        lastName: String,
        tiersPayantId: Long,
        num: String,
        taux: Int,
        phone: String?,
        dateNaiss: String?
    ) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        lifecycleScope.launch {
            customerRepository.createCarnetCustomer(
                firstName = firstName,
                lastName = lastName,
                phone = phone,
                dateNaiss = dateNaiss,
                tiersPayantId = tiersPayantId,
                num = num,
                taux = taux
            ).fold(
                onSuccess = { customer ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        "Client Carnet créé avec succès",
                        Toast.LENGTH_SHORT
                    ).show()
                    onCustomerCreated?.invoke(customer)
                    dismiss()
                },
                onFailure = { error ->
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
        searchJob?.cancel()
        _binding = null
    }
}
