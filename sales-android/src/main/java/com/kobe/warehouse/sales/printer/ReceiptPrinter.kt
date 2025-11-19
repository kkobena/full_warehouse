package com.kobe.warehouse.sales.printer

import android.content.Context
import android.util.Log
import com.kobe.warehouse.sales.data.model.PaymentMode
import com.kobe.warehouse.sales.data.model.Sale
import com.kobe.warehouse.sales.data.model.SaleLine
import com.sunmi.peripheral.printer.InnerPrinterException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Receipt printer for cash sales
 * Formats and prints thermal receipts using Sunmi printer
 *
 * Based on: com.kobe.warehouse.service.receipt.service.CashSaleReceiptService
 */
class ReceiptPrinter(context: Context) {

    private val printerService = SunmiPrinterService(context)
    private val numberFormat = NumberFormat.getNumberInstance(Locale.FRANCE).apply {
        // Ensure space is used as grouping separator (French format)
        val symbols = (this as java.text.DecimalFormat).decimalFormatSymbols
        symbols.groupingSeparator = ' '
        (this as java.text.DecimalFormat).decimalFormatSymbols = symbols
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
     *
     * @param sale         the sale to print
     * @param storeName    the store name (e.g., "Pharma Smart")
     * @param storeAddress the store address
     * @param storePhone   the store phone number
     * @param welcomeMsg   optional welcome message
     * @param footerNote   optional footer note (e.g., "Merci pour votre visite")
     * @param cassierName  cashier name
     * @param sellerName   seller name (if different from cashier)
     * @param payments     list of payment details (payment mode, amount)
     * @param montantVerse amount given by customer (for cash payments)
     * @param paperRoll    paper roll size (58mm or 80mm), defaults to 58mm
     * @return true if printing succeeded, false otherwise
     */
    suspend fun printReceipt(
        sale: Sale,
        storeName: String,
        storeAddress: String? = null,
        storePhone: String? = null,
        welcomeMsg: String? = null,
        footerNote: String? = "Merci pour votre visite!",
        cassierName: String,
        sellerName: String? = null,
        payments: List<PaymentDetail>,
        montantVerse: Int = 0,
        paperRoll: PaperRoll = PaperRoll.MM_58
    ): Boolean {
        return try {
            // Connect to printer
            if (!printerService.connect()) {
                Log.e(TAG, "Failed to connect to printer")
                return false
            }

            // Check printer status
            val status = printerService.getPrinterStatus()
            if (status != SunmiPrinterService.PrinterStatus.READY) {
                Log.e(TAG, "Printer not ready: ${status.message}")
                return false
            }

            // Print receipt
            printHeader(storeName, storeAddress, storePhone, welcomeMsg, paperRoll)
            printSaleInfo(sale, cassierName, sellerName, paperRoll)
            printSaleLines(sale.salesLines, paperRoll)
            printSummary(sale, payments, montantVerse, paperRoll)
            printFooter(footerNote, System.currentTimeMillis(), paperRoll)

            // Feed and cut paper
            printerService.feedPaper(3)
            printerService.cutPaper()

            Log.d(TAG, "Receipt printed successfully for sale ${sale.numberTransaction}")
            true
        } catch (e: InnerPrinterException) {
            Log.e(TAG, "Printer error", e)
            false
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
            alignment = SunmiPrinterService.ALIGN_CENTER,
            fontSize = SunmiPrinterService.FONT_SIZE_XLARGE,
            isBold = true
        )
        printerService.printEmptyLine(1)

        // Store address and phone (centered)
        address?.let {
            printerService.printLine(it, alignment = SunmiPrinterService.ALIGN_CENTER)
        }
        phone?.let {
            printerService.printLine("Tél: $it", alignment = SunmiPrinterService.ALIGN_CENTER)
        }
        printerService.printEmptyLine(1)

        // Welcome message (centered)
        welcomeMsg?.let {
            printerService.printLine(it, alignment = SunmiPrinterService.ALIGN_CENTER)
            printerService.printEmptyLine(1)
        }

        // Separator
        printerService.printSeparator(paperRoll.width)
    }

    /**
     * Print sale information (transaction number, customer, cashier, seller)
     */
    private fun printSaleInfo(sale: Sale, cassierName: String, sellerName: String?, paperRoll: PaperRoll) {
        // Transaction number
        printerService.printLine("TICKET: ${sale.numberTransaction}", isBold = false)

        // Cashier
        printerService.printLine("CASSIER(RE): $cassierName")

        // Seller (if different from cashier)
        sellerName?.let {
            if (it != cassierName) {
                printerService.printLine("VENDEUR(SE): $it")
            }
        }

        // Customer (if exists)
        sale.customer?.let { customer ->
            printerService.printLine("Client: ${customer.firstName} ${customer.lastName}")
            customer.phone?.takeIf { it.isNotEmpty() }?.let { phone ->
                printerService.printLine("Tél: $phone")
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
                SunmiPrinterService.ALIGN_LEFT,
                SunmiPrinterService.ALIGN_LEFT,
                SunmiPrinterService.ALIGN_RIGHT,
                SunmiPrinterService.ALIGN_RIGHT
            )
        )
        printerService.printSeparator(paperRoll.width)

        // Sale lines
        for (line in saleLines) {
            val quantity = line.quantityRequested.toString()
            val productName = line.produitLibelle.take(productWidth)
            val unitPrice = formatAmount(line.regularUnitPrice)
            val totalPrice = formatAmount(line.salesAmount)

            printerService.printColumns(
                columns = listOf(quantity, productName, unitPrice, totalPrice),
                widths = listOf(qtyWidth, productWidth, priceWidth, totalWidth),
                alignments = listOf(
                    SunmiPrinterService.ALIGN_LEFT,
                    SunmiPrinterService.ALIGN_LEFT,
                    SunmiPrinterService.ALIGN_RIGHT,
                    SunmiPrinterService.ALIGN_RIGHT
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
            MONTANT_TTC,
            formatAmount(sale.salesAmount),
            paperRoll.width
        )

        // Discount (if any)
        if (sale.discountAmount > 0) {
            printerService.printLabelValue(
                REMISE,
                formatAmount(sale.discountAmount),
                paperRoll.width
            )
        }

        printerService.printEmptyLine(1)

        // Total to pay (large, bold)
        val totalLabel = if (paperRoll == PaperRoll.MM_58) "TOTAL" else TOTAL_A_PAYER
        printerService.printLine(
            "$totalLabel: ${formatAmount(sale.netAmount)}",
            fontSize = SunmiPrinterService.FONT_SIZE_LARGE,
            isBold = true
        )
        printerService.printEmptyLine(1)

        // Payment section
        if (payments.isNotEmpty()) {
            printerService.printLine(
                REGLEMENT,
                alignment = SunmiPrinterService.ALIGN_CENTER,
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
                    payment.paymentModeLabel,
                    amount,
                    paperRoll.width
                )
            }

            // Cash change (if any)
            if (monnaie > 0) {
                val changeLabel = if (paperRoll == PaperRoll.MM_58) "MONNAIE" else MONTANT_RENDU
                printerService.printLabelValue(
                    changeLabel,
                    formatAmount(monnaie),
                    paperRoll.width
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
            printerService.printLine(it, alignment = SunmiPrinterService.ALIGN_CENTER)
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
