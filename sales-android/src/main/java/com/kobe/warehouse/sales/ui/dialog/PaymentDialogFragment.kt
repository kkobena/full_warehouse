package com.kobe.warehouse.sales.ui.dialog

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.api.PaymentApiService
import com.kobe.warehouse.sales.data.model.Payment
import com.kobe.warehouse.sales.data.model.PaymentMode
import com.kobe.warehouse.sales.data.repository.PaymentRepository
import com.kobe.warehouse.sales.databinding.DialogPaymentBinding
import com.kobe.warehouse.sales.ui.adapter.PaymentModeAdapter
import com.kobe.warehouse.sales.ui.viewmodel.ComptantSaleViewModel
import com.kobe.warehouse.sales.ui.viewmodel.ISaleViewModel
import com.kobe.warehouse.sales.ui.viewmodel.UnifiedSaleViewModel
import com.kobe.warehouse.sales.utils.ApiClient
import com.kobe.warehouse.sales.utils.TokenManager

/**
 * Payment Dialog Fragment
 * Handles payment selection and processing with up to 2 payment modes
 *
 * Features:
 * - Up to 2 payment methods
 * - CASH + other: other takes remaining amount automatically
 * - 2 non-cash modes: manual amount input for each
 * - QR code display for mobile money
 * - Amount input validation
 */
class PaymentDialogFragment : DialogFragment() {

    private var _binding: DialogPaymentBinding? = null
    private val binding get() = _binding!!

    private lateinit var paymentModeAdapter: PaymentModeAdapter
    private lateinit var paymentRepository: PaymentRepository
    private lateinit var saleViewModel: ISaleViewModel

    private var totalAmount: Int = 0
    private var payrollAmount: Int = 0

    // Two payment modes maximum
    private var selectedMode1: PaymentMode? = null
    private var selectedMode2: PaymentMode? = null

    companion object {
        private const val ARG_TOTAL_AMOUNT = "total_amount"

        fun newInstance(totalAmount: Int): PaymentDialogFragment {
            return PaymentDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TOTAL_AMOUNT, totalAmount)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_Material3_DayNight_Dialog)

        arguments?.let {
            totalAmount = it.getInt(ARG_TOTAL_AMOUNT, 0)
        }

        // Get ViewModel from activity (try both ComptantSaleViewModel and UnifiedSaleViewModel)
        saleViewModel = try {
            ViewModelProvider(requireActivity())[ComptantSaleViewModel::class.java]
        } catch (e: Exception) {
            ViewModelProvider(requireActivity())[UnifiedSaleViewModel::class.java]
        }

        // Get amount to pay from currentSale
        val currentSale = saleViewModel.currentSale.value
        android.util.Log.d("PaymentDialog", "=== PAYMENT DIALOG INIT ===")
        android.util.Log.d("PaymentDialog", "currentSale = $currentSale")
        android.util.Log.d("PaymentDialog", "currentSale.id = ${currentSale?.id}")
        android.util.Log.d("PaymentDialog", "currentSale.salesAmount = ${currentSale?.salesAmount}")
        android.util.Log.d("PaymentDialog", "currentSale.payrollAmount = ${currentSale?.payrollAmount}")
        android.util.Log.d("PaymentDialog", "currentSale.discountAmount = ${currentSale?.discountAmount}")
        android.util.Log.d("PaymentDialog", "currentSale.partAssure = ${currentSale?.partAssure}")
        android.util.Log.d("PaymentDialog", "currentSale.natureVente = ${currentSale?.natureVente}")

        // Calculate payrollAmount with fallback
        payrollAmount = when {
            // For ASSURANCE/CARNET: use partAssure (client's part to pay)
            currentSale?.natureVente == "ASSURANCE" || currentSale?.natureVente == "CARNET" -> {
                currentSale.partAssure ?: (currentSale.salesAmount - (currentSale.discountAmount ?: 0))
            }
            // For COMPTANT: use payrollAmount if available, else calculate from salesAmount
            currentSale?.payrollAmount != null && currentSale.payrollAmount > 0 -> {
                currentSale.payrollAmount
            }
            // Fallback: calculate from salesAmount - discountAmount
            else -> {
                (currentSale?.salesAmount ?: 0) - (currentSale?.discountAmount ?: 0)
            }
        }

        android.util.Log.d("PaymentDialog", "Final payrollAmount = $payrollAmount")

        // Setup repository
        val tokenManager = TokenManager(requireContext())
        val retrofit = ApiClient.create(tokenManager = tokenManager)
        val paymentApiService = retrofit.create(PaymentApiService::class.java)
        paymentRepository = PaymentRepository(paymentApiService)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPaymentModes()
        setupListeners()
        updateTotalDisplay()
        updateVatDisplay()
        updateUIState()
    }

    /**
     * Setup payment modes RecyclerView
     */
    private fun setupPaymentModes() {
        paymentModeAdapter = PaymentModeAdapter { paymentMode ->
            selectPaymentMode(paymentMode)
        }

        binding.rvPaymentModes.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = paymentModeAdapter
        }

        // Load payment modes
        loadPaymentModes()
    }

    /**
     * Load payment modes from repository
     */
    private fun loadPaymentModes() {
        binding.progressBar.visibility = View.VISIBLE

        Thread {
            val result = kotlinx.coroutines.runBlocking {
                paymentRepository.getPaymentModes()
            }

            requireActivity().runOnUiThread {
                binding.progressBar.visibility = View.GONE

                result.fold(
                    onSuccess = { modes ->
                        paymentModeAdapter.submitList(modes)
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            requireContext(),
                            "Erreur de chargement: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }.start()
    }

    /**
     * Setup listeners
     */
    private fun setupListeners() {
        // Mode 1 amount change
        binding.etMode1Amount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                onMode1AmountChanged()
            }
        })

        // Mode 2 amount change
        binding.etMode2Amount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                onMode2AmountChanged()
            }
        })

        // Remove mode 1
        binding.btnRemoveMode1.setOnClickListener {
            removeMode1()
        }

        // Remove mode 2
        binding.btnRemoveMode2.setOnClickListener {
            removeMode2()
        }

        // Validate button
        binding.btnValidate.setOnClickListener {
            processPayment()
        }

        // Cancel button
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Legacy fields (kept for backward compatibility)
        binding.etAmountGiven.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                calculateChange()
                // Update payment modes grid visibility based on cash amount
                updatePaymentModesGridVisibility()
            }
        })
    }

    /**
     * Update payment modes grid visibility based on cash amount
     * Hide if cash amount covers the total, show otherwise
     */
    private fun updatePaymentModesGridVisibility() {
        // Only apply when single cash mode is selected
        if (selectedMode1?.isCash() == true && selectedMode2 == null) {
            val cashAmount = getCashAmountEntered()
            if (cashAmount >= payrollAmount) {
                binding.rvPaymentModes.visibility = View.GONE
            } else {
                binding.rvPaymentModes.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Select payment mode
     */
    private fun selectPaymentMode(paymentMode: PaymentMode) {
        // Check if already selected
        if (selectedMode1?.code == paymentMode.code || selectedMode2?.code == paymentMode.code) {
            Toast.makeText(requireContext(), "Mode déjà sélectionné", Toast.LENGTH_SHORT).show()
            return
        }

        // Check maximum 2 modes
        if (selectedMode1 != null && selectedMode2 != null) {
            Toast.makeText(requireContext(), "Maximum 2 modes de paiement", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if cash mode is already selected and covers the total
        if (selectedMode1?.isCash() == true) {
            val cashAmount = getCashAmountEntered()
            if (cashAmount >= payrollAmount) {
                Toast.makeText(requireContext(), "Le montant espèce couvre déjà le total", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Add to first available slot
        if (selectedMode1 == null) {
            selectedMode1 = paymentMode
        } else {
            // Adding second mode - preserve cash amount if cash is mode 1
            if (selectedMode1?.isCash() == true) {
                // Transfer cash amount from legacy field to mode1 field
                val cashAmount = binding.etAmountGiven.text.toString()
                if (cashAmount.isNotEmpty()) {
                    binding.etMode1Amount.setText(cashAmount)
                }
            }
            selectedMode2 = paymentMode
        }

        updateUIState()
    }

    /**
     * Get the cash amount entered by user
     */
    private fun getCashAmountEntered(): Int {
        // Check which field has the cash amount
        return when {
            selectedMode1?.isCash() == true && selectedMode2 == null -> {
                // Single cash mode - use legacy field
                binding.etAmountGiven.text.toString().toIntOrNull() ?: 0
            }
            selectedMode1?.isCash() == true -> {
                // Cash is mode 1
                binding.etMode1Amount.text.toString().toIntOrNull() ?: 0
            }
            selectedMode2?.isCash() == true -> {
                // Cash is mode 2
                binding.etMode2Amount.text.toString().toIntOrNull() ?: 0
            }
            else -> 0
        }
    }

    /**
     * Remove mode 1
     * If mode 2 exists, shift it to mode 1 position
     */
    private fun removeMode1() {
        if (selectedMode2 != null) {
            // If mode 2 is cash and will become mode 1 (single cash mode)
            // Transfer amount to legacy field
            if (selectedMode2?.isCash() == true) {
                val cashAmount = binding.etMode2Amount.text.toString()
                if (cashAmount.isNotEmpty()) {
                    binding.etAmountGiven.setText(cashAmount)
                }
            }
            // Shift mode 2 to mode 1
            selectedMode1 = selectedMode2
            selectedMode2 = null
            binding.etMode1Amount.setText(binding.etMode2Amount.text)
            binding.etMode2Amount.setText("")
        } else {
            // Single mode being removed
            if (selectedMode1?.isCash() == true) {
                // Keep the cash amount in legacy field (don't clear it)
                // Amount is already in etAmountGiven for single cash mode
            }
            selectedMode1 = null
            binding.etMode1Amount.setText("")
        }
        updateUIState()
    }

    /**
     * Remove mode 2
     */
    private fun removeMode2() {
        // If mode 1 is cash, transfer amount back to legacy field
        if (selectedMode1?.isCash() == true) {
            val cashAmount = binding.etMode1Amount.text.toString()
            if (cashAmount.isNotEmpty()) {
                binding.etAmountGiven.setText(cashAmount)
            }
        }
        selectedMode2 = null
        binding.etMode2Amount.setText("")
        updateUIState()
    }

    /**
     * Update UI state based on selected modes
     */
    private fun updateUIState() {
        // Update selected mode cards visibility
        if (selectedMode1 != null) {
            binding.selectedMode1Card.visibility = View.VISIBLE
            binding.tvSelectedMode1.text = selectedMode1!!.libelle
        } else {
            binding.selectedMode1Card.visibility = View.GONE
        }

        if (selectedMode2 != null) {
            binding.selectedMode2Card.visibility = View.VISIBLE
            binding.tvSelectedMode2.text = selectedMode2!!.libelle
        } else {
            binding.selectedMode2Card.visibility = View.GONE
        }

        // Determine which amount inputs to show
        val hasCash = selectedMode1?.isCash() == true || selectedMode2?.isCash() == true
        val hasNonCash1 = selectedMode1 != null && !selectedMode1!!.isCash()
        val hasNonCash2 = selectedMode2 != null && !selectedMode2!!.isCash()

        // Reset visibility
        binding.mode1AmountLayout.visibility = View.GONE
        binding.mode2AmountLayout.visibility = View.GONE
        binding.amountInputLayout.visibility = View.GONE
        binding.tvChange.visibility = View.GONE
        binding.qrCodeCard.visibility = View.GONE

        // Case 1: Two payment modes selected (CASH + other OR two non-cash)
        if (selectedMode1 != null && selectedMode2 != null) {
            // Show amount input for mode 1
            binding.mode1AmountLayout.visibility = View.VISIBLE
            binding.mode1AmountLayout.hint = "Montant ${selectedMode1!!.libelle}"
            binding.etMode1Amount.isEnabled = true

            // Show amount input for mode 2
            binding.mode2AmountLayout.visibility = View.VISIBLE
            binding.mode2AmountLayout.hint = "Montant ${selectedMode2!!.libelle}"
            binding.etMode2Amount.isEnabled = true

            // Focus on the first editable field
            binding.etMode1Amount.requestFocus()

            // Show change display if cash is involved
            if (hasCash) {
                binding.tvChange.visibility = View.VISIBLE
            }

            // Initialize amounts if empty: mode1 = total, mode2 = 0
            if (binding.etMode1Amount.text.isNullOrEmpty() && binding.etMode2Amount.text.isNullOrEmpty()) {
                isUpdatingAmounts = true
                binding.etMode1Amount.setText(payrollAmount.toString())
                binding.etMode2Amount.setText("0")
                isUpdatingAmounts = false
            }
        }
        // Case 3: Single cash mode (legacy behavior)
        else if (selectedMode1?.isCash() == true && selectedMode2 == null) {
            binding.amountInputLayout.visibility = View.VISIBLE
            binding.tvChange.visibility = View.VISIBLE
            // Focus on cash amount field
            binding.etAmountGiven.requestFocus()
        }
        // Case 4: Single non-cash mode
        else if (selectedMode1 != null && !selectedMode1!!.isCash() && selectedMode2 == null) {
            // No amount input needed, uses full amount

            // Show QR code for mobile money
            if (selectedMode1!!.isMobileMoney() && selectedMode1!!.hasQrCode()) {
                showQrCode(selectedMode1!!)
            }
        }

        // Hide payment modes grid if:
        // 1. Two modes are already selected, OR
        // 2. Cash mode is selected and amount covers the total
        val cashCoversTotal = selectedMode1?.isCash() == true &&
                              selectedMode2 == null &&
                              getCashAmountEntered() >= payrollAmount

        if (selectedMode1 != null && selectedMode2 != null) {
            binding.rvPaymentModes.visibility = View.GONE
        } else if (cashCoversTotal) {
            binding.rvPaymentModes.visibility = View.GONE
        } else {
            binding.rvPaymentModes.visibility = View.VISIBLE
        }

        // Show QR codes if applicable
        if (selectedMode2?.isMobileMoney() == true && selectedMode2!!.hasQrCode()) {
            showQrCode(selectedMode2!!)
        }
    }

    // Flag to prevent recursive updates between amount fields
    private var isUpdatingAmounts = false

    /**
     * Handle mode 1 amount change
     * Automatically adjusts mode 2 amount when two modes are selected
     */
    private fun onMode1AmountChanged() {
        if (isUpdatingAmounts) return

        val amount1Text = binding.etMode1Amount.text.toString()
        val amount1 = amount1Text.toIntOrNull() ?: 0

        // If two modes are selected, calculate remaining for mode 2
        if (selectedMode1 != null && selectedMode2 != null) {
            isUpdatingAmounts = true
            val remaining = payrollAmount - amount1
            binding.etMode2Amount.setText(if (remaining >= 0) remaining.toString() else "0")
            isUpdatingAmounts = false

            // Calculate change if cash is involved
            calculateChange()
        }
    }

    /**
     * Handle mode 2 amount change
     * Automatically adjusts mode 1 amount when two modes are selected
     */
    private fun onMode2AmountChanged() {
        if (isUpdatingAmounts) return

        val amount2Text = binding.etMode2Amount.text.toString()
        val amount2 = amount2Text.toIntOrNull() ?: 0

        // If two modes are selected, calculate remaining for mode 1
        if (selectedMode1 != null && selectedMode2 != null) {
            isUpdatingAmounts = true
            val remaining = payrollAmount - amount2
            binding.etMode1Amount.setText(if (remaining >= 0) remaining.toString() else "0")
            isUpdatingAmounts = false

            // Calculate change if cash is involved
            calculateChange()
        }
    }

    /**
     * Show QR code for mobile money payment
     */
    private fun showQrCode(paymentMode: PaymentMode) {
        binding.qrCodeCard.visibility = View.VISIBLE

        paymentMode.qrCode?.let { qrCodeBytes ->
            try {
                val bitmap = BitmapFactory.decodeByteArray(qrCodeBytes, 0, qrCodeBytes.size)
                binding.ivQrCode.setImageBitmap(bitmap)
                binding.tvQrCodeLabel.text = "Scanner pour payer avec ${paymentMode.libelle}"
            } catch (e: Exception) {
                binding.qrCodeCard.visibility = View.GONE
            }
        }
    }

    /**
     * Update total display
     */
    private fun updateTotalDisplay() {
        binding.tvTotal.text = "Total: ${formatAmount(payrollAmount)} FCFA"
    }

    /**
     * Update VAT display
     */
    private fun updateVatDisplay() {
        val currentSale = saleViewModel.currentSale.value
        val vatAmount = currentSale?.getTotalTax() ?: 0

        if (vatAmount > 0) {
            binding.tvVatAmount.visibility = View.VISIBLE
            binding.tvVatAmount.text = "TVA: ${formatAmount(vatAmount)} FCFA"
        } else {
            binding.tvVatAmount.visibility = View.GONE
        }
    }

    /**
     * Calculate change (legacy for single CASH payment)
     */
    private fun calculateChange() {
        // For CASH + other mode scenario
        val mode1IsCash = selectedMode1?.isCash() == true
        val mode2IsCash = selectedMode2?.isCash() == true

        if (mode1IsCash && selectedMode2 != null) {
            val cashAmount = binding.etMode1Amount.text.toString().toIntOrNull() ?: 0
            val otherAmount = binding.etMode2Amount.text.toString().toIntOrNull() ?: 0
            val totalGiven = cashAmount + otherAmount
            val change = totalGiven - payrollAmount

            binding.tvChange.text = if (change >= 0) {
                "Monnaie: ${formatAmount(change)} FCFA"
            } else {
                "Montant insuffisant"
            }

            binding.tvChange.setTextColor(
                requireContext().getColor(
                    if (change >= 0) R.color.success else R.color.error
                )
            )
        } else if (mode2IsCash && selectedMode1 != null) {
            val cashAmount = binding.etMode2Amount.text.toString().toIntOrNull() ?: 0
            val otherAmount = binding.etMode1Amount.text.toString().toIntOrNull() ?: 0
            val totalGiven = cashAmount + otherAmount
            val change = totalGiven - payrollAmount

            binding.tvChange.text = if (change >= 0) {
                "Monnaie: ${formatAmount(change)} FCFA"
            } else {
                "Montant insuffisant"
            }

            binding.tvChange.setTextColor(
                requireContext().getColor(
                    if (change >= 0) R.color.success else R.color.error
                )
            )
        } else if (mode1IsCash && selectedMode2 == null) {
            // Legacy single cash payment
            val amountGivenText = binding.etAmountGiven.text.toString()
            if (amountGivenText.isEmpty()) {
                binding.tvChange.text = "Monnaie: ${formatAmount(0)} FCFA"
                return
            }

            val amountGiven = amountGivenText.toIntOrNull() ?: 0
            val change = amountGiven - payrollAmount

            binding.tvChange.text = if (change >= 0) {
                "Monnaie: ${formatAmount(change)} FCFA"
            } else {
                "Montant insuffisant"
            }

            binding.tvChange.setTextColor(
                requireContext().getColor(
                    if (change >= 0) R.color.success else R.color.error
                )
            )
        }
    }

    /**
     * Format amount with space as thousand separator
     */
    private fun formatAmount(amount: Int): String {
        return amount.toString().reversed().chunked(3).joinToString(" ").reversed()
    }

    /**
     * Process payment
     */
    private fun processPayment() {
        if (selectedMode1 == null) {
            Toast.makeText(requireContext(), "Sélectionnez au moins un mode de paiement", Toast.LENGTH_SHORT).show()
            return
        }

        val payments = mutableListOf<Payment>()
        var totalMontantVerse = 0
        var totalMontantRendu = 0

        // Case 1: Single payment mode
        if (selectedMode2 == null) {
            if (selectedMode1!!.isCash()) {
                // Legacy cash payment
                val amountGivenText = binding.etAmountGiven.text.toString()
                if (amountGivenText.isEmpty()) {
                    binding.amountInputLayout.error = "Entrez le montant versé"
                    return
                }

                val montantVerse = amountGivenText.toIntOrNull() ?: 0
                if (montantVerse < payrollAmount) {
                    binding.amountInputLayout.error = "Montant insuffisant"
                    return
                }

                totalMontantVerse = montantVerse
                totalMontantRendu = montantVerse - payrollAmount

                payments.add(
                    Payment(
                        paymentModeCode = selectedMode1!!.code,
                        paymentMode = selectedMode1,
                        amount = payrollAmount,
                        netAmount = payrollAmount,
                        paidAmount = montantVerse,
                        montantVerse = montantVerse,
                        montantRendu = totalMontantRendu
                    )
                )
            } else {
                // Single non-cash payment (uses full amount)
                payments.add(
                    Payment(
                        paymentModeCode = selectedMode1!!.code,
                        paymentMode = selectedMode1,
                        amount = payrollAmount,
                        netAmount = payrollAmount,
                        paidAmount = payrollAmount,
                        montantVerse = payrollAmount,
                        montantRendu = 0
                    )
                )
                totalMontantVerse = payrollAmount
            }
        }
        // Case 2: Two payment modes
        else {
            val mode1IsCash = selectedMode1!!.isCash()
            val mode2IsCash = selectedMode2!!.isCash()

            // Case 2a: CASH + other
            if (mode1IsCash || mode2IsCash) {
                val cashMode = if (mode1IsCash) selectedMode1!! else selectedMode2!!
                val otherMode = if (mode1IsCash) selectedMode2!! else selectedMode1!!

                val cashAmountText = if (mode1IsCash) {
                    binding.etMode1Amount.text.toString()
                } else {
                    binding.etMode2Amount.text.toString()
                }

                if (cashAmountText.isEmpty()) {
                    Toast.makeText(requireContext(), "Entrez le montant espèce", Toast.LENGTH_SHORT).show()
                    return
                }

                val cashAmount = cashAmountText.toIntOrNull() ?: 0
                val otherAmount = payrollAmount - cashAmount

                if (otherAmount < 0) {
                    Toast.makeText(requireContext(), "Le montant espèce dépasse le total", Toast.LENGTH_SHORT).show()
                    return
                }

                // Cash payment
                payments.add(
                    Payment(
                        paymentModeCode = cashMode.code,
                        paymentMode = cashMode,
                        amount = cashAmount,
                        netAmount = cashAmount,
                        paidAmount = cashAmount,
                        montantVerse = cashAmount,
                        montantRendu = 0
                    )
                )

                // Other mode payment
                payments.add(
                    Payment(
                        paymentModeCode = otherMode.code,
                        paymentMode = otherMode,
                        amount = otherAmount,
                        netAmount = otherAmount,
                        paidAmount = otherAmount,
                        montantVerse = otherAmount,
                        montantRendu = 0
                    )
                )

                totalMontantVerse = payrollAmount
                totalMontantRendu = 0
            }
            // Case 2b: Two non-cash modes
            else {
                val amount1Text = binding.etMode1Amount.text.toString()
                val amount2Text = binding.etMode2Amount.text.toString()

                if (amount1Text.isEmpty() || amount2Text.isEmpty()) {
                    Toast.makeText(requireContext(), "Entrez les montants pour les deux modes", Toast.LENGTH_SHORT).show()
                    return
                }

                val amount1 = amount1Text.toIntOrNull() ?: 0
                val amount2 = amount2Text.toIntOrNull() ?: 0
                val total = amount1 + amount2

                if (total != payrollAmount) {
                    Toast.makeText(
                        requireContext(),
                        "Le total (${formatAmount(total)}) doit être égal à ${formatAmount(payrollAmount)} FCFA",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                // Mode 1 payment
                payments.add(
                    Payment(
                        paymentModeCode = selectedMode1!!.code,
                        paymentMode = selectedMode1,
                        amount = amount1,
                        netAmount = amount1,
                        paidAmount = amount1,
                        montantVerse = amount1,
                        montantRendu = 0
                    )
                )

                // Mode 2 payment
                payments.add(
                    Payment(
                        paymentModeCode = selectedMode2!!.code,
                        paymentMode = selectedMode2,
                        amount = amount2,
                        netAmount = amount2,
                        paidAmount = amount2,
                        montantVerse = amount2,
                        montantRendu = 0
                    )
                )

                totalMontantVerse = payrollAmount
                totalMontantRendu = 0
            }
        }

        // Finalize sale through ViewModel
        saleViewModel.finalizeSale(payments, totalMontantVerse, totalMontantRendu)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
