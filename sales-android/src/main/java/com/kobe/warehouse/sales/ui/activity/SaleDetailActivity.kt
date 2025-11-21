package com.kobe.warehouse.sales.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.data.api.SalesApiService
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.repository.SalesRepository
import com.kobe.warehouse.sales.databinding.ActivitySaleDetailBinding
import com.kobe.warehouse.sales.printer.ReceiptPrinter
import com.kobe.warehouse.sales.ui.adapter.PaymentDetailAdapter
import com.kobe.warehouse.sales.ui.adapter.SaleLineDetailAdapter
import com.kobe.warehouse.sales.ui.viewmodel.SalesHomeViewModel
import com.kobe.warehouse.sales.ui.viewmodel.SalesHomeViewModelFactory
import com.kobe.warehouse.sales.utils.ApiClient
import com.kobe.warehouse.sales.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class SaleDetailActivity : BaseActivity() {

  private lateinit var binding: ActivitySaleDetailBinding
  private lateinit var viewModel: SalesHomeViewModel
  private lateinit var saleLineAdapter: SaleLineDetailAdapter
  private lateinit var paymentAdapter: PaymentDetailAdapter


  private var currentSale: Sale? = null

  companion object {
    const val EXTRA_SALE_ID = "sale_id"
    const val EXTRA_SALE_DATE = "sale_date"
  }

  private val numberFormat = NumberFormat.getNumberInstance(Locale.FRANCE)
  private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRANCE)


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivitySaleDetailBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setupToolbar()
    setupRecyclerViews()
    setupViewModel()
    setupClickListeners()



    // Load sale details
    val saleId = intent.getLongExtra(EXTRA_SALE_ID, -1)
    val saleDate = intent.getStringExtra(EXTRA_SALE_DATE)

    if (saleId != -1L && saleDate != null) {
      viewModel.loadSaleById(saleId, saleDate)
    } else {
      Toast.makeText(this, "Erreur: Vente introuvable", Toast.LENGTH_SHORT).show()
      finish()
    }
  }

  private fun setupToolbar() {
    setSupportActionBar(binding.toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    binding.toolbar.setNavigationOnClickListener {
      finish()
    }
  }

  private fun setupRecyclerViews() {
    // Sale lines
    saleLineAdapter = SaleLineDetailAdapter()
    binding.rvItems.apply {
      layoutManager = LinearLayoutManager(this@SaleDetailActivity)
      adapter = saleLineAdapter
    }

    // Payments
    paymentAdapter = PaymentDetailAdapter()
    binding.rvPayments.apply {
      layoutManager = LinearLayoutManager(this@SaleDetailActivity)
      adapter = paymentAdapter
    }
  }

  private fun setupViewModel() {
    val tokenManager = TokenManager(this)
    val retrofit = ApiClient.create(tokenManager = tokenManager)
    val salesApiService = retrofit.create(SalesApiService::class.java)
    val salesRepository = SalesRepository(salesApiService)

    val factory = SalesHomeViewModelFactory(salesRepository)
    viewModel = ViewModelProvider(this, factory)[SalesHomeViewModel::class.java]

    // Observe sale details
    viewModel.selectedSale.observe(this) { sale ->
      sale?.let {
        currentSale = it
        displaySaleDetails(it)
      }
    }

    // Observe errors
    viewModel.errorMessage.observe(this) { error ->
      error?.let {
        Toast.makeText(this, it, Toast.LENGTH_LONG).show()
      }
    }
  }

  private fun setupClickListeners() {
    binding.btnPrint.setOnClickListener {
      currentSale?.let { sale ->
        printReceipt(sale)
      }
    }
  }

  private fun printReceipt(sale: Sale) {
    MaterialAlertDialogBuilder(this)
      .setTitle("Impression")
      .setMessage("Voulez-vous imprimer le reçu ?")
      .setPositiveButton("Imprimer") { _, _ ->
        lifecycleScope.launch {
          try {
            Toast.makeText(this@SaleDetailActivity, "Impression du reçu en cours...", Toast.LENGTH_SHORT).show()

            val success = withContext(Dispatchers.IO) {
              try {
                val receiptPrinter = ReceiptPrinter(this@SaleDetailActivity)
                // Print receipt (ReceiptPrinter automatically loads store and account info)
                receiptPrinter.printReceipt(sale = sale)
              } catch (e: Exception) {
                android.util.Log.e("SaleDetailActivity", "Print error", e)
                false
              }
            }

            if (success) {
              Toast.makeText(
                this@SaleDetailActivity,
                "Reçu imprimé avec succès",
                Toast.LENGTH_SHORT
              ).show()
            } else {
              Toast.makeText(
                this@SaleDetailActivity,
                "Erreur lors de l'impression",
                Toast.LENGTH_LONG
              ).show()
            }
          } catch (e: Exception) {
            android.util.Log.e("SaleDetailActivity", "Print error", e)
            Toast.makeText(
              this@SaleDetailActivity,
              "Erreur d'impression: ${e.message}",
              Toast.LENGTH_LONG
            ).show()
          }
        }
      }
      .setNegativeButton("Annuler", null)
      .show()
  }

  private fun displaySaleDetails(sale: Sale) {
    // Transaction number
    binding.tvTransactionNumber.text = sale.numberTransaction

    // Date
    val date = sale.updatedAt?.let {
      java.time.LocalDateTime.parse(it)
    } ?: java.time.LocalDateTime.now()
    binding.tvDate.text = date.format(dateFormat)

    // Customer
    val customerName = buildString {
      append(sale.customer?.firstName ?: "")
      if (!sale.customer?.lastName.isNullOrEmpty()) {
        append(" ")
        append(sale.customer.lastName)
      }
    }.takeIf { it.isNotBlank() } ?: "Comptant"
    binding.tvCustomer.text = customerName

    // Cashier (would need to fetch from backend or add to Sale model)
    binding.tvCashier.text = "N/A" // TODO: Add cashier name to Sale model

    // Sale lines
    saleLineAdapter.submitList(sale.salesLines)

    // Payments
    paymentAdapter.submitList(sale.payments)

    // Discount
    if (sale.discountAmount > 0) {
      binding.discountRow.visibility = View.VISIBLE
      binding.tvDiscount.text = "- ${formatAmount(sale.discountAmount)}"
    } else {
      binding.discountRow.visibility = View.GONE
    }

    // Net amount
    val netAmount = sale.salesAmount - sale.discountAmount
    binding.tvNetAmount.text = formatAmount(netAmount)

    // Amount given and change (only for cash payments)
    if (sale.montantVerse > 0) {
      binding.amountGivenRow.visibility = View.VISIBLE
      binding.tvAmountGiven.text = formatAmount(sale.montantVerse)

      val change = sale.montantVerse - netAmount
      if (change > 0) {
        binding.changeRow.visibility = View.VISIBLE
        binding.tvChange.text = formatAmount(change)
      } else {
        binding.changeRow.visibility = View.GONE
      }
    } else {
      binding.amountGivenRow.visibility = View.GONE
      binding.changeRow.visibility = View.GONE
    }
  }

  private fun formatAmount(amount: Int): String {
    return "${numberFormat.format(amount)} FCFA"
  }


}
