package com.kobe.warehouse.sales.printer

import android.content.Context
import android.util.Log
import com.kobe.warehouse.sales.data.api.StoreApiService
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.model.SaleLine
import com.kobe.warehouse.sales.data.repository.AuthRepository
import com.kobe.warehouse.sales.data.repository.StoreRepository
import com.kobe.warehouse.sales.utils.ApiClient
import com.kobe.warehouse.sales.utils.TokenManager
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Receipt printer for cash sales
 * Formats and prints thermal receipts using Sunmi printer (real or mock)
 * Automatically detects device type and uses appropriate printer service
 * Automatically loads store info and account info from cache/API
 *
 * Based on: com.kobe.warehouse.service.receipt.service.CashSaleReceiptService
 */
class ReceiptPrinter(private val context: Context) {

    private val printerService = UnifiedPrinterService(context)
    private val tokenManager = TokenManager(context)

    // Lazy init repositories
    private val storeRepository: StoreRepository by lazy {
        val retrofit = ApiClient.create(tokenManager = tokenManager)
        val storeApiService = retrofit.create(StoreApiService::class.java)
        StoreRepository(storeApiService, context)
    }

    private val authRepository: AuthRepository by lazy {
        val retrofit = ApiClient.create(tokenManager = tokenManager)
        val authApiService = retrofit.create(com.kobe.warehouse.sales.data.api.AuthApiService::class.java)
        AuthRepository(authApiService, tokenManager)
    }
    private val numberFormat = NumberFormat.getNumberInstance(Locale.FRANCE).apply {
        // Ensure space is used as grouping separator (French format)
        val symbols = (this as java.text.DecimalFormat).decimalFormatSymbols
        symbols.groupingSeparator = ' '
        this.decimalFormatSymbols = symbols
    }
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE)

    companion object {
        private const val TAG = "ReceiptPrinter"

        // Labels (French)
        private const val MONTANT_TTC = "MONTANT TTC"
        private const val REMISE = "REMISE"
        private const val TOTAL_A_PAYER = "TOTAL A PAYER"
        private const val MONTANT_RENDU = "MONNAIE RENDUE"
        private const val RESTE_A_PAYER = "RESTE A PAYER"
        private const val REGLEMENT = "REGLEMENT(S)"
    }

    /**
     * Paper roll size enumeration
     */
    enum class PaperRoll(val width: Int, val productNameWidth: Int) {
        MM_58(32, 18),  // 58mm paper: 32 chars per line, 18 chars for product name
        MM_80(48, 24)   // 80mm paper: 48 chars per line, 24 chars for product name
    }

    /**
     * Print cash sale receipt
     * Automatically loads store info and account info from cache/API
     *
     * @param sale         the sale to print
     * @param paperRollSize paper roll size in mm (58 or 80), defaults to user preference
     * @return true if printing succeeded, false otherwise
     */
    suspend fun printReceipt(
        sale: Sale,
        paperRollSize: Int = tokenManager.getReceiptRollSize()
    ): Boolean {
        return try {
            // Determine paper roll type
            val paperRoll = when {
                paperRollSize >= 80 -> PaperRoll.MM_80
                else -> PaperRoll.MM_58
            }

            // Load store info from cache/API
            val storeResult = storeRepository.getStore()
            val store = storeResult.getOrNull()

            // Load account info from cache/API
            val accountResult = authRepository.getAccount()
            val account = accountResult.getOrNull()

            // Get cashier name from account
            val cassierName = account?.let {
                "${it.firstName} ${it.lastName}".trim()
            } ?: "Caissier"

            // Store info
            val storeName = store?.getDisplayName() ?: "Pharmacie"
            val storeAddress = store?.getFormattedAddress()
            val storePhone = store?.getFormattedPhone()
            val welcomeMsg = store?.welcomeMessage
            val footerNote = store?.note ?: "Merci pour votre visite!"

            // Convert sale payments to PaymentDetail
            val payments = sale.payments.map { payment ->
                PaymentDetail(
                    paymentModeLabel = payment.paymentMode?.libelle ?: "Espèces",
                    amount = payment.paidAmount,
                    isCash = payment.paymentMode?.isCash() ?: true
                )
            }

            // Connect to printer (real or mock)
            if (!printerService.connect()) {
                Log.e(TAG, "Failed to connect to printer")
                return false
            }

            // Check printer status (0 = ready)
            val status = printerService.getPrinterStatus()
            if (status != 0) {
                Log.e(TAG, "Printer not ready, status: $status")
                // Continue anyway for mock printer
            }

            // Print receipt
            printHeader(storeName, storeAddress, storePhone, welcomeMsg, paperRoll)
            printSaleInfo(sale, cassierName, paperRoll)
            printSaleLines(sale.salesLines, paperRoll)
            printSummary(sale, payments, sale.montantVerse, paperRoll)
            printFooter(footerNote, System.currentTimeMillis(), paperRoll)

            // Feed and cut paper
            printerService.feedPaper(3)
            printerService.cutPaper()

            Log.d(TAG, "Receipt printed successfully for sale ${sale.numberTransaction}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to print receipt", e)
            false
        } finally {
            printerService.disconnect()
        }
    }

    /**
     * Print receipt header (store info, welcome message)
     */
    private fun printHeader(storeName: String, address: String?, phone: String?, welcomeMsg: String?, paperRoll: PaperRoll) {
        // Store name (centered, large, bold)
        printerService.printLine(
            storeName,
            alignment = UnifiedPrinterService.ALIGN_CENTER,
            fontSize = UnifiedPrinterService.FONT_SIZE_XLARGE,
            isBold = true
        )
        printerService.printEmptyLine(1)

        // Store address and phone (centered)
        address?.let {
            printerService.printLine(it, alignment = UnifiedPrinterService.ALIGN_CENTER)
        }
        phone?.let {
            printerService.printLine("TEL: $it", alignment = UnifiedPrinterService.ALIGN_CENTER)
        }
        printerService.printEmptyLine(1)

        // Welcome message (centered)
        welcomeMsg?.let {
            printerService.printLine(it, alignment = UnifiedPrinterService.ALIGN_CENTER)
            printerService.printEmptyLine(1)
        }

        // Separator
        printerService.printSeparator(paperRoll.width)
    }

    /**
     * Print sale information (transaction number, customer, cashier)
     */
    private fun printSaleInfo(sale: Sale, cassierName: String, paperRoll: PaperRoll) {
        // Transaction number
        printerService.printLine("TICKET: ${sale.numberTransaction}", isBold = false)

        // Cashier
        printerService.printLine("CASSIER(RE): $cassierName")

        // Customer (if exists)
        sale.customer?.let { customer ->
            val customerFullName = buildString {
                append(customer.firstName ?: "")
                if (!customer.lastName.isNullOrEmpty()) {
                    append(" ")
                    append(customer.lastName)
                }
            }.trim()

            if (customerFullName.isNotEmpty()) {
                printerService.printLine("CLIENT: $customerFullName")
            }

            customer.phone?.takeIf { it.isNotEmpty() }?.let { phone ->
                printerService.printLine("TEL: $phone")
            }
        }

        printerService.printEmptyLine(1)
        printerService.printSeparator(paperRoll.width)
    }

    /**
     * Print sale lines (products)
     * Format: QTE  PRODUIT              PU     MONTANT
     */
    private fun printSaleLines(saleLines: List<SaleLine>, paperRoll: PaperRoll) {
        // Column widths based on paper size
        val qtyWidth = 3
        val productWidth = paperRoll.productNameWidth
        val priceWidth = if (paperRoll == PaperRoll.MM_58) 6 else 8
        val totalWidth = if (paperRoll == PaperRoll.MM_58) 6 else 10

        // Table header (bold)
        printerService.printColumns(
            columns = listOf("QTE", "PRODUIT", "PU", "MONTANT"),
            widths = listOf(qtyWidth, productWidth, priceWidth, totalWidth),
            alignments = listOf(
                UnifiedPrinterService.ALIGN_LEFT,
                UnifiedPrinterService.ALIGN_LEFT,
                UnifiedPrinterService.ALIGN_RIGHT,
                UnifiedPrinterService.ALIGN_RIGHT
            )
        )
        printerService.printSeparator(paperRoll.width)

        // Sale lines
        for (line in saleLines) {
            val quantity = line.quantityRequested.toString()
            val productName = (line.produitLibelle ?: "").take(productWidth)
            val unitPrice = formatAmount(line.regularUnitPrice)
            val totalPrice = formatAmount(line.salesAmount)

            printerService.printColumns(
                columns = listOf(quantity, productName, unitPrice, totalPrice),
                widths = listOf(qtyWidth, productWidth, priceWidth, totalWidth),
                alignments = listOf(
                    UnifiedPrinterService.ALIGN_LEFT,
                    UnifiedPrinterService.ALIGN_LEFT,
                    UnifiedPrinterService.ALIGN_RIGHT,
                    UnifiedPrinterService.ALIGN_RIGHT
                )
            )
        }

        printerService.printSeparator(paperRoll.width)
    }

    /**
     * Print summary (totals, discounts, payments, change)
     */
    private fun printSummary(sale: Sale, payments: List<PaymentDetail>, montantVerse: Int, paperRoll: PaperRoll) {
        // Total amount (TTC)
        printerService.printLabelValue(
            label = MONTANT_TTC,
            value = formatAmount(sale.salesAmount),
            paperWidth = paperRoll.width
        )

        // Discount (if any)
        if (sale.discountAmount > 0) {
            printerService.printLabelValue(
                label = REMISE,
                value = formatAmount(sale.discountAmount),
                paperWidth = paperRoll.width
            )
        }

        printerService.printEmptyLine(1)

        // Total to pay (large, bold)
        val totalLabel = if (paperRoll == PaperRoll.MM_58) "TOTAL" else TOTAL_A_PAYER
        printerService.printLine(
            "$totalLabel: ${formatAmount(sale.netAmount)}",
            fontSize = UnifiedPrinterService.FONT_SIZE_LARGE,
            isBold = true
        )
        printerService.printEmptyLine(1)

        // Payment section
        if (payments.isNotEmpty()) {
            printerService.printLine(
                REGLEMENT,
                alignment = UnifiedPrinterService.ALIGN_CENTER,
                isBold = true
            )
            printerService.printEmptyLine(1)

            var monnaie = 0
            for (payment in payments) {
                val amount = if (payment.isCash) {
                    monnaie = montantVerse - sale.netAmount
                    formatAmount(montantVerse)
                } else {
                    formatAmount(payment.amount)
                }

                printerService.printLabelValue(
                    label = payment.paymentModeLabel,
                    value = amount,
                    paperWidth = paperRoll.width
                )
            }

            // Cash change (if any)
            if (monnaie > 0) {
                val changeLabel = if (paperRoll == PaperRoll.MM_58) "MONNAIE" else MONTANT_RENDU
                printerService.printLabelValue(
                    label = changeLabel,
                    value = formatAmount(monnaie),
                    paperWidth = paperRoll.width
                )
            }

            // Remaining to pay (if any)
            if (sale.restToPay > 0) {
                val remainingLabel = if (paperRoll == PaperRoll.MM_58) "RESTE" else RESTE_A_PAYER
                printerService.printLine(
                    "$remainingLabel: ${formatAmount(sale.restToPay)}",
                    isBold = true
                )
            }

            printerService.printEmptyLine(1)
        }
    }

    /**
     * Print footer (timestamp, thank you message)
     */
    private fun printFooter(footerNote: String?, saleTimestamp: Long, paperRoll: PaperRoll) {
        printerService.printSeparator(paperRoll.width)

        // Sale timestamp
        val formattedDate = dateFormat.format(saleTimestamp)
        printerService.printLine(formattedDate)

        printerService.printEmptyLine(1)

        // Thank you message (centered)
        footerNote?.let {
            printerService.printLine(it, alignment = UnifiedPrinterService.ALIGN_CENTER)
        }
    }

    /**
     * Format amount as string with space as thousand separator (French format)
     * Example: 15000 -> "15 000"
     * Example: 1000 -> "1 000"
     * Example: 250 -> "250"
     */
    private fun formatAmount(amount: Int): String {
        return numberFormat.format(amount)
    }

    /**
     * Payment detail data class
     */
    data class PaymentDetail(
      val paymentModeLabel: String,
      val amount: Int,
      val isCash: Boolean = false
    )
}
