package com.kobe.warehouse.sales.ui.dialog

import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.api.*
import com.kobe.warehouse.sales.data.model.PaymentMode
import com.kobe.warehouse.sales.data.repository.PaymentRepository
import com.kobe.warehouse.sales.databinding.DialogPaymentBinding
import com.kobe.warehouse.sales.ui.adapter.PaymentModeAdapter
import com.kobe.warehouse.sales.ui.viewmodel.ComptantSaleViewModel
import com.kobe.warehouse.sales.utils.ApiClient
import com.kobe.warehouse.sales.utils.TokenManager

/**
 * Payment Dialog Fragment
 * Handles payment selection and processing
 *
 * Features:
 * - Multiple payment methods
 * - QR code display for mobile money
 * - Amount input validation
 * - Change calculation
 */
class PaymentDialogFragment : DialogFragment() {

    private var _binding: DialogPaymentBinding? = null
    private val binding get() = _binding!!

    private lateinit var paymentModeAdapter: PaymentModeAdapter
    private lateinit var paymentRepository: PaymentRepository
    private lateinit var saleViewModel: ComptantSaleViewModel

    private var totalAmount: Int = 0
    private var selectedPaymentMode: PaymentMode? = null

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

        // Using coroutine (simplified for now)
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
        // Amount given text change
        binding.etAmountGiven.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                calculateChange()
            }
        })

        // Validate button
        binding.btnValidate.setOnClickListener {
            processPayment()
        }

        // Cancel button
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    /**
     * Select payment mode
     */
    private fun selectPaymentMode(paymentMode: PaymentMode) {
        selectedPaymentMode = paymentMode
        paymentModeAdapter.setSelected(paymentMode.code)

        // Show/hide amount input based on payment method
        if (paymentMode.isCash()) {
            binding.amountInputLayout.visibility = View.VISIBLE
            binding.tvChange.visibility = View.VISIBLE
        } else {
            binding.amountInputLayout.visibility = View.GONE
            binding.tvChange.visibility = View.GONE
        }

        // Show QR code for mobile money
        if (paymentMode.isMobileMoney() && paymentMode.hasQrCode()) {
            showQrCode(paymentMode)
        } else {
            binding.qrCodeCard.visibility = View.GONE
        }
    }

    /**
     * Show QR code for mobile money payment
     */
    private fun showQrCode(paymentMode: PaymentMode) {
        binding.qrCodeCard.visibility = View.VISIBLE

        paymentMode.qrCode?.let { qrCodeBase64 ->
            try {
                // Decode base64 QR code
                val decodedBytes = Base64.decode(qrCodeBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
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
        binding.tvTotal.text = "$totalAmount FCFA"
    }

    /**
     * Calculate change
     */
    private fun calculateChange() {
        val amountGivenText = binding.etAmountGiven.text.toString()
        if (amountGivenText.isEmpty()) {
            binding.tvChange.text = "Monnaie: 0 FCFA"
            return
        }

        val amountGiven = amountGivenText.toIntOrNull() ?: 0
        val change = amountGiven - totalAmount

        binding.tvChange.text = if (change >= 0) {
            "Monnaie: $change FCFA"
        } else {
            "Montant insuffisant"
        }

        binding.tvChange.setTextColor(
            requireContext().getColor(
                if (change >= 0) R.color.success else R.color.error
            )
        )
    }

    /**
     * Process payment
     */
    private fun processPayment() {
        if (selectedPaymentMode == null) {
            Toast.makeText(requireContext(), "Sélectionnez un mode de paiement", Toast.LENGTH_SHORT).show()
            return
        }

        var montantVerse = totalAmount
        var montantRendu = 0

        // Validate amount for cash payment
        if (selectedPaymentMode!!.isCash()) {
            val amountGivenText = binding.etAmountGiven.text.toString()
            if (amountGivenText.isEmpty()) {
                binding.amountInputLayout.error = "Entrez le montant versé"
                return
            }

            montantVerse = amountGivenText.toIntOrNull() ?: 0
            if (montantVerse < totalAmount) {
                binding.amountInputLayout.error = "Montant insuffisant"
                return
            }

            montantRendu = montantVerse - totalAmount
        }

        // Create payment request
        val payments = listOf(
            PaymentRequest(
                paymentModeCode = selectedPaymentMode!!.code,
                amount = totalAmount
            )
        )

        // Finalize sale through ViewModel
        saleViewModel.finalizeSale(payments, montantVerse, montantRendu)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
