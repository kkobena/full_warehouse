package com.kobe.warehouse.sales.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kobe.warehouse.sales.data.api.TiersPayantApiService
import com.kobe.warehouse.sales.data.model.TiersPayant
import com.kobe.warehouse.sales.data.repository.TiersPayantRepository
import com.kobe.warehouse.sales.databinding.FragmentAssureStep1Binding
import com.kobe.warehouse.sales.utils.ApiClient
import com.kobe.warehouse.sales.utils.TokenManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Step 1 of Assure Customer Creation
 * Client principal info + Principal tiers payant selection
 */
class AssureStep1Fragment : Fragment() {

    private var _binding: FragmentAssureStep1Binding? = null
    private val binding get() = _binding!!

    private lateinit var tiersPayantRepository: TiersPayantRepository
    private var tiersPayants: List<TiersPayant> = emptyList()
    private var selectedTiersPayant: TiersPayant? = null
    private var searchJob: Job? = null
    private var isSettingTiersPayant = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssureStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRepository()
        setupUI()
        setupValidation()
        setupListeners()
        setupAutoFocus()
    }

    private fun setupRepository() {
        val tokenManager = TokenManager(requireContext())
        val apiClient = ApiClient.create(tokenManager = tokenManager)
        val tiersPayantApi = apiClient.create(TiersPayantApiService::class.java)
        tiersPayantRepository = TiersPayantRepository(tiersPayantApi)
    }

    /**
     * Auto-focus on first required field (Nom) when fragment loads
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
        // Setup sexe dropdown
        val sexeOptions = arrayOf("Masculin", "Féminin")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sexeOptions)
        binding.actvSexe.setAdapter(adapter)
    }

    private fun setupValidation() {
        binding.etFirstName.addTextChangedListener { validateFirstName() }
        binding.etLastName.addTextChangedListener { validateLastName() }
        binding.etPhone.addTextChangedListener { validatePhone() }
        binding.etEmail.addTextChangedListener { validateEmail() }
        binding.etNum.addTextChangedListener { validateNum() }
        binding.etTaux.addTextChangedListener { validateTaux() }
    }

    private fun setupListeners() {
        // Tiers Payant search with debounce
        binding.actvTiersPayant.addTextChangedListener { text ->
            searchJob?.cancel()
            if (isSettingTiersPayant) return@addTextChangedListener
            val query = text?.toString()?.trim().orEmpty()
            if (query.length > 2) {
                searchJob = lifecycleScope.launch {
                    delay(300)  // Debounce
                    searchTiersPayants(query)
                }
            }
        }

        binding.actvTiersPayant.setOnItemClickListener { _, _, position, _ ->
            searchJob?.cancel()
            selectedTiersPayant = tiersPayants.getOrNull(position)
            if (selectedTiersPayant?.id == null || selectedTiersPayant?.id == -1L) {
                // "Create new tiers payant" option selected
                selectedTiersPayant = null
                binding.actvTiersPayant.setText("", false)
                openCreateTiersPayantDialog()
            } else {
                binding.tilTiersPayant.error = null
                binding.etNum.requestFocus()
            }
        }

        // Button to create new tiers payant
        binding.btnCreateTiersPayant.setOnClickListener {
            openCreateTiersPayantDialog()
        }
    }

    private suspend fun searchTiersPayants(query: String) {
        binding.progressBarTiersPayant.visibility = View.VISIBLE

        tiersPayantRepository.searchTiersPayants(search = query, type = "ASSURANCE").fold(
            onSuccess = { results ->
                binding.progressBarTiersPayant.visibility = View.GONE

                // Add "Create new" option
                tiersPayants = results + TiersPayant(
                    id = -1L,
                    name = "Créer un nouveau tiers payant..."
                )

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    tiersPayants.map { it.name }
                )
                binding.actvTiersPayant.setAdapter(adapter)
                binding.actvTiersPayant.showDropDown()
            },
            onFailure = { error ->
                binding.progressBarTiersPayant.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "Erreur de recherche: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun openCreateTiersPayantDialog() {
        val dialog = TiersPayantCreateDialogFragment.newInstance(
            categorie = "ASSURANCE"
        ) { createdTiersPayant ->
            // Tiers payant created, select it
            searchJob?.cancel()
            selectedTiersPayant = createdTiersPayant
            isSettingTiersPayant = true
            binding.actvTiersPayant.setText(createdTiersPayant.getDisplayName(), false)
            binding.actvTiersPayant.dismissDropDown()
            isSettingTiersPayant = false
            binding.tilTiersPayant.error = null
            binding.etNum.requestFocus()
        }
        dialog.show(parentFragmentManager, TiersPayantCreateDialogFragment.TAG)
    }

    fun validateForm(): Boolean {
        val isFirstNameValid = validateFirstName()
        val isLastNameValid = validateLastName()
        val isPhoneValid = validatePhone()
        val isEmailValid = validateEmail()
        val isTiersPayantValid = validateTiersPayant()
        val isNumValid = validateNum()
        val isTauxValid = validateTaux()

        if (!isFirstNameValid || !isLastNameValid || !isPhoneValid || !isEmailValid ||
            !isTiersPayantValid || !isNumValid || !isTauxValid) {
            Toast.makeText(requireContext(), "Veuillez corriger les erreurs", Toast.LENGTH_SHORT).show()
            return false
        }

        // Save data to parent dialog
        val parentDialog = parentFragment as? AssureCustomerCreateDialogFragment
        parentDialog?.customerData = AssureCustomerCreateDialogFragment.CustomerFormData(
            firstName = binding.etFirstName.text.toString().trim(),
            lastName = binding.etLastName.text.toString().trim(),
            phone = binding.etPhone.text.toString().trim().ifEmpty { null },
            email = binding.etEmail.text.toString().trim().ifEmpty { null },
            sexe = binding.actvSexe.text.toString().ifEmpty { null },
            datNaiss = null  // TODO: Add date picker
        )

        parentDialog?.principalTiersPayantData = AssureCustomerCreateDialogFragment.TiersPayantFormData(
            tiersPayantId = selectedTiersPayant!!.id!!,
            tiersPayantName = selectedTiersPayant!!.getDisplayName(),
            num = binding.etNum.text.toString().trim(),
            taux = binding.etTaux.text.toString().toInt()
        )

        return true
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
        // Phone is optional
        binding.tilPhone.error = null
        return true
    }

    private fun validateEmail(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        if (email.isEmpty()) {
            binding.tilEmail.error = null
            return true  // Optional
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

    private fun validateTiersPayant(): Boolean {
        return if (selectedTiersPayant == null || selectedTiersPayant?.id == null) {
            binding.tilTiersPayant.error = "Tiers payant obligatoire"
            false
        } else {
            binding.tilTiersPayant.error = null
            true
        }
    }

    private fun validateNum(): Boolean {
        val num = binding.etNum.text.toString().trim()
        return if (num.isEmpty()) {
            binding.tilNum.error = "Numéro d'assuré obligatoire"
            false
        } else {
            binding.tilNum.error = null
            true
        }
    }

    private fun validateTaux(): Boolean {
        val tauxText = binding.etTaux.text.toString().trim()
        if (tauxText.isEmpty()) {
            binding.tilTaux.error = "Taux de couverture obligatoire"
            return false
        }

        val taux = tauxText.toIntOrNull()
        return if (taux == null || taux < 5 || taux > 100) {
            binding.tilTaux.error = "Taux doit être entre 5 et 100"
            false
        } else {
            binding.tilTaux.error = null
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}
