package com.kobe.warehouse.sales.ui.dialog

import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.api.PaymentApiService
import com.kobe.warehouse.sales.data.model.Payment
import com.kobe.warehouse.sales.data.model.PaymentMode
import com.kobe.warehouse.sales.data.repository.PaymentRepository
import com.kobe.warehouse.sales.databinding.DialogPaymentBinding
import com.kobe.warehouse.sales.ui.adapter.PaymentModeAdapter
import com.kobe.warehouse.sales.ui.viewmodel.ComptantSaleViewModel
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
    private lateinit var saleViewModel: ComptantSaleViewModel

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

        // Get ViewModel from activity
        saleViewModel = ViewModelProvider(requireActivity())[ComptantSaleViewModel::class.java]

        // Calculate payrollAmount = salesAmount - discount
        val currentSale = saleViewModel.currentSale.value
        payrollAmount = (currentSale?.salesAmount ?: 0) - (currentSale?.discountAmount ?: 0)

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
            }
        })
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

        // Add to first available slot
        if (selectedMode1 == null) {
            selectedMode1 = paymentMode
        } else {
            selectedMode2 = paymentMode
        }

        updateUIState()
    }

    /**
     * Remove mode 1
     */
    private fun removeMode1() {
        selectedMode1 = null
        binding.etMode1Amount.setText("")
        updateUIState()
    }

    /**
     * Remove mode 2
     */
    private fun removeMode2() {
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

        // Case 1: CASH + other mode
        if (hasCash && (hasNonCash1 || hasNonCash2)) {
            // Show amount input for CASH
            if (selectedMode1?.isCash() == true) {
                binding.mode1AmountLayout.visibility = View.VISIBLE
                binding.mode1AmountLayout.hint = "Montant ${selectedMode1!!.libelle}"
                binding.etMode1Amount.isEnabled = true

                // Mode 2 takes remaining amount automatically
                binding.mode2AmountLayout.visibility = View.VISIBLE
                binding.mode2AmountLayout.hint = "${selectedMode2!!.libelle} (automatique)"
                binding.etMode2Amount.isEnabled = false
            } else {
                binding.mode2AmountLayout.visibility = View.VISIBLE
                binding.mode2AmountLayout.hint = "Montant ${selectedMode2!!.libelle}"
                binding.etMode2Amount.isEnabled = true

                // Mode 1 takes remaining amount automatically
                binding.mode1AmountLayout.visibility = View.VISIBLE
                binding.mode1AmountLayout.hint = "${selectedMode1!!.libelle} (automatique)"
                binding.etMode1Amount.isEnabled = false
            }

            // Show change for cash
            binding.tvChange.visibility = View.VISIBLE
        }
        // Case 2: Two non-cash modes
        else if (selectedMode1 != null && selectedMode2 != null && !hasCash) {
            // Show amount input for both
            binding.mode1AmountLayout.visibility = View.VISIBLE
            binding.mode1AmountLayout.hint = "Montant ${selectedMode1!!.libelle}"
            binding.etMode1Amount.isEnabled = true

            binding.mode2AmountLayout.visibility = View.VISIBLE
            binding.mode2AmountLayout.hint = "Montant ${selectedMode2!!.libelle}"
            binding.etMode2Amount.isEnabled = true
        }
        // Case 3: Single cash mode (legacy behavior)
        else if (selectedMode1?.isCash() == true && selectedMode2 == null) {
            binding.amountInputLayout.visibility = View.VISIBLE
            binding.tvChange.visibility = View.VISIBLE
        }
        // Case 4: Single non-cash mode
        else if (selectedMode1 != null && !selectedMode1!!.isCash() && selectedMode2 == null) {
            // No amount input needed, uses full amount

            // Show QR code for mobile money
            if (selectedMode1!!.isMobileMoney() && selectedMode1!!.hasQrCode()) {
                showQrCode(selectedMode1!!)
            }
        }

        // Hide payment modes grid if 2 selected
        if (selectedMode1 != null && selectedMode2 != null) {
            binding.rvPaymentModes.visibility = View.GONE
        } else {
            binding.rvPaymentModes.visibility = View.VISIBLE
        }

        // Show QR codes if applicable
        if (selectedMode2?.isMobileMoney() == true && selectedMode2!!.hasQrCode()) {
            showQrCode(selectedMode2!!)
        }
    }

    /**
     * Handle mode 1 amount change
     */
    private fun onMode1AmountChanged() {
        val amount1Text = binding.etMode1Amount.text.toString()
        if (amount1Text.isEmpty()) return

        val amount1 = amount1Text.toIntOrNull() ?: 0

        // If CASH + other, calculate remaining for other mode
        if (selectedMode1?.isCash() == true && selectedMode2 != null) {
            val remaining = payrollAmount - amount1
            binding.etMode2Amount.setText(if (remaining > 0) remaining.toString() else "0")

            // Calculate change
            calculateChange()
        }
    }

    /**
     * Handle mode 2 amount change
     */
    private fun onMode2AmountChanged() {
        val amount2Text = binding.etMode2Amount.text.toString()
        if (amount2Text.isEmpty()) return

        val amount2 = amount2Text.toIntOrNull() ?: 0

        // If CASH + other, calculate remaining for cash mode
        if (selectedMode2?.isCash() == true && selectedMode1 != null) {
            val remaining = payrollAmount - amount2
            binding.etMode1Amount.setText(if (remaining > 0) remaining.toString() else "0")

            // Calculate change
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
