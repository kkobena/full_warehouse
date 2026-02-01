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
import com.kobe.warehouse.sales.data.api.TiersPayantApiService
import com.kobe.warehouse.sales.data.model.TiersPayant
import com.kobe.warehouse.sales.data.repository.TiersPayantRepository
import com.kobe.warehouse.sales.databinding.DialogTiersPayantCreateBinding
import com.kobe.warehouse.sales.utils.ApiClient
import com.kobe.warehouse.sales.utils.TokenManager
import kotlinx.coroutines.launch

/**
 * Dialog for creating a new tiers payant (insurance provider)
 *
 * Based on: form-tiers-payant.component.ts
 *
 * Simplified mobile version with essential fields:
 * - name * (required - short name)
 * - fullName * (required - full name)
 * - codeOrganisme (optional - organization code)
 * - telephone (optional)
 * - email (optional)
 * - categorie (CARNET or ASSURANCE - auto-filled from parameter)
 */
class TiersPayantCreateDialogFragment : DialogFragment() {

    private var _binding: DialogTiersPayantCreateBinding? = null
    private val binding get() = _binding!!

    private lateinit var tiersPayantRepository: TiersPayantRepository
    private var onTiersPayantCreated: ((TiersPayant) -> Unit)? = null
    private var categorie: String = "CARNET"  // Default category

    private var isSaving = false

    companion object {
        const val TAG = "TiersPayantCreateDialog"
        private const val ARG_CATEGORIE = "categorie"

        /**
         * Create new instance
         * @param categorie Category: "CARNET" or "ASSURANCE"
         * @param onTiersPayantCreated Callback when tiers payant is created
         */
        fun newInstance(
            categorie: String = "CARNET",
            onTiersPayantCreated: (TiersPayant) -> Unit
        ): TiersPayantCreateDialogFragment {
            return TiersPayantCreateDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORIE, categorie)
                }
                this.onTiersPayantCreated = onTiersPayantCreated
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_PharmaSmart_Dialog_FullScreen)

        // Get category from arguments
        categorie = arguments?.getString(ARG_CATEGORIE) ?: "CARNET"

        // Initialize repository
        val tokenManager = TokenManager(requireContext())
        val apiClient = ApiClient.create(tokenManager = tokenManager)
        val tiersPayantApi = apiClient.create(TiersPayantApiService::class.java)
        tiersPayantRepository = TiersPayantRepository(tiersPayantApi)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTiersPayantCreateBinding.inflate(inflater, container, false)
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
     * Auto-focus on first required field when dialog opens
     */
    private fun setupAutoFocus() {
        binding.etName.post {
            binding.etName.requestFocus()
            // Show keyboard
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(binding.etName, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun setupUI() {
        val categoryLabel = when (categorie) {
            "ASSURANCE" -> "Assurance"
            "CARNET" -> "Carnet"
            else -> categorie
        }

        binding.toolbar.apply {
            title = "Nouveau tiers payant ($categoryLabel)"
            setNavigationIcon(R.drawable.ic_close)
            setNavigationOnClickListener { dismiss() }
        }

        // Display category
        binding.tvCategorie.text = "Catégorie: $categoryLabel"
    }

    private fun setupValidation() {
        // Real-time validation
        binding.etName.addTextChangedListener {
            validateName()
        }

        binding.etFullName.addTextChangedListener {
            validateFullName()
        }

        binding.etCodeOrganisme.addTextChangedListener {
            validateCodeOrganisme()
        }

        binding.etTelephone.addTextChangedListener {
            validateTelephone()
        }

        binding.etEmail.addTextChangedListener {
            validateEmail()
        }
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            if (validateForm()) {
                saveTiersPayant()
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun validateName(): Boolean {
        val name = binding.etName.text.toString().trim()
        return if (name.isEmpty()) {
            binding.tilName.error = "Nom obligatoire"
            false
        } else {
            binding.tilName.error = null
            true
        }
    }

    private fun validateFullName(): Boolean {
        val fullName = binding.etFullName.text.toString().trim()
        return if (fullName.isEmpty()) {
            binding.tilFullName.error = "Nom complet obligatoire"
            false
        } else {
            binding.tilFullName.error = null
            true
        }
    }

    private fun validateCodeOrganisme(): Boolean {
        // Optional field, no validation needed
        binding.tilCodeOrganisme.error = null
        return true
    }

    private fun validateTelephone(): Boolean {
        // Optional field, no validation needed
        binding.tilTelephone.error = null
        return true
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
        val isNameValid = validateName()
        val isFullNameValid = validateFullName()
        val isCodeOrganismeValid = validateCodeOrganisme()
        val isTelephoneValid = validateTelephone()
        val isEmailValid = validateEmail()

        return isNameValid && isFullNameValid && isCodeOrganismeValid &&
               isTelephoneValid && isEmailValid
    }

    private fun saveTiersPayant() {
        if (isSaving) return

        isSaving = true
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        val tiersPayant = TiersPayant(
            name = binding.etName.text.toString().trim(),
            fullName = binding.etFullName.text.toString().trim(),
            codeOrganisme = binding.etCodeOrganisme.text.toString().trim().ifEmpty { null },
            telephone = binding.etTelephone.text.toString().trim().ifEmpty { null },
            email = binding.etEmail.text.toString().trim().ifEmpty { null },
            categorie = categorie,
            nbreBordereaux = 1  // Default value
        )

        lifecycleScope.launch {
            tiersPayantRepository.createTiersPayant(tiersPayant).fold(
                onSuccess = { createdTiersPayant: TiersPayant ->
                    isSaving = false
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true

                    Toast.makeText(
                        requireContext(),
                        "Tiers payant créé avec succès",
                        Toast.LENGTH_SHORT
                    ).show()

                    onTiersPayantCreated?.invoke(createdTiersPayant)
                    dismiss()
                },
                onFailure = { error: Throwable ->
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
