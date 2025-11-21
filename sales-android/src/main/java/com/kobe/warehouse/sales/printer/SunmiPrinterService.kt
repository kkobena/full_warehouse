package com.kobe.warehouse.sales.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterException
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.InnerResultCallback
import com.sunmi.peripheral.printer.SunmiPrinterService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Service wrapper for Sunmi printer library
 * Provides a Kotlin-friendly coroutine-based API for thermal receipt printing
 */
class SunmiPrinterService(private val context: Context) {

    private var printerService: SunmiPrinterService? = null
    private var isConnected = false

    companion object {
        private const val TAG = "SunmiPrinterService"

        // Font sizes
        const val FONT_SIZE_NORMAL = 24f
        const val FONT_SIZE_LARGE = 28f
        const val FONT_SIZE_XLARGE = 32f

        // Alignment
        const val ALIGN_LEFT = 0
        const val ALIGN_CENTER = 1
        const val ALIGN_RIGHT = 2

        // Paper width (characters per line for 80mm paper)
        const val PAPER_WIDTH_80MM = 48
        const val PAPER_WIDTH_58MM = 32
    }

    /**
     * Initialize and connect to Sunmi printer
     * Must be called before any printing operations
     */
    suspend fun connect(): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            InnerPrinterManager.getInstance().bindService(
                context,
                object : InnerPrinterCallback() {
                    override fun onConnected(service: SunmiPrinterService?) {
                        printerService = service
                        isConnected = true
                        Log.d(TAG, "Sunmi printer connected successfully")
                        continuation.resume(true)
                    }

                    override fun onDisconnected() {
                        isConnected = false
                        printerService = null
                        Log.d(TAG, "Sunmi printer disconnected")
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                }
            )
        } catch (e: InnerPrinterException) {
            Log.e(TAG, "Failed to connect to Sunmi printer", e)
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }

    /**
     * Disconnect from printer service
     */
    fun disconnect() {
        try {
            InnerPrinterManager.getInstance().unBindService(context, object : InnerPrinterCallback() {
                override fun onConnected(service: SunmiPrinterService?) {}
                override fun onDisconnected() {
                    isConnected = false
                    printerService = null
                    Log.d(TAG, "Sunmi printer unbound")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting printer", e)
        }
    }

    /**
     * Check if printer is connected and ready
     */
    fun isReady(): Boolean = isConnected && printerService != null

    /**
     * Print text with specified alignment and font size
     */
    @Throws(InnerPrinterException::class)
    fun printText(text: String, alignment: Int = ALIGN_LEFT, fontSize: Float = FONT_SIZE_NORMAL, isBold: Boolean = false) {
        checkPrinterReady()
        printerService?.apply {
            setAlignment(alignment, null)
            setFontSize(fontSize, null)
            if (isBold) {
                sendRAWData(byteArrayOf(0x1B, 0x45, 0x01), null) // ESC E 1 - Enable bold
            }
            printText(text, null)
            if (isBold) {
                sendRAWData(byteArrayOf(0x1B, 0x45, 0x00), null) // ESC E 0 - Disable bold
            }
        }
    }

    /**
     * Print line of text (with line feed)
     */
    @Throws(InnerPrinterException::class)
    fun printLine(text: String, alignment: Int = ALIGN_LEFT, fontSize: Float = FONT_SIZE_NORMAL, isBold: Boolean = false) {
        printText(text + "\n", alignment, fontSize, isBold)
    }

    /**
     * Print separator line
     */
    @Throws(InnerPrinterException::class)
    fun printSeparator(length: Int = PAPER_WIDTH_58MM) {
        printLine("-".repeat(length), ALIGN_LEFT)
    }

    /**
     * Print empty line (line feed)
     */
    @Throws(InnerPrinterException::class)
    fun printEmptyLine(lines: Int = 1) {
        checkPrinterReady()
        printerService?.lineWrap(lines, null)
    }

    /**
     * Print formatted columns (e.g., "Product    Price")
     */
    @Throws(InnerPrinterException::class)
    fun printColumns(
        columns: List<String>,
        widths: List<Int>,
        alignments: List<Int> = List(columns.size) { ALIGN_LEFT }
    ) {
        checkPrinterReady()
        val formattedColumns = columns.mapIndexed { index, text ->
            val width = widths.getOrElse(index) { 10 }
            val alignment = alignments.getOrElse(index) { ALIGN_LEFT }
            when (alignment) {
                ALIGN_LEFT -> text.take(width).padEnd(width)
                ALIGN_RIGHT -> text.take(width).padStart(width)
                ALIGN_CENTER -> {
                    val padding = (width - text.length) / 2
                    text.take(width).padStart(text.length + padding).padEnd(width)
                }
                else -> text.take(width).padEnd(width)
            }
        }.joinToString("")

        printerService?.printText(formattedColumns + "\n", null)
    }

    /**
     * Print two-column layout (label and value)
     * Common pattern: "Label:               Value"
     */
    @Throws(InnerPrinterException::class)
    fun printLabelValue(label: String, value: String, totalWidth: Int = PAPER_WIDTH_58MM) {
        val valueLength = value.length
        val labelLength = totalWidth - valueLength
        val paddedLabel = label.take(labelLength).padEnd(labelLength)
        printLine(paddedLabel + value)
    }

    /**
     * Print bitmap image (e.g., QR code, logo)
     */
    @Throws(InnerPrinterException::class)
    fun printBitmap(bitmap: Bitmap, alignment: Int = ALIGN_CENTER) {
        checkPrinterReady()
        printerService?.apply {
            setAlignment(alignment, null)
            printBitmap(bitmap, null)
            setAlignment(ALIGN_LEFT, null) // Reset to left
        }
    }

    /**
     * Print QR code from Base64 string
     */
    @Throws(InnerPrinterException::class)
    fun printQrCode(base64QrCode: String, alignment: Int = ALIGN_CENTER) {
        val decodedBytes = Base64.decode(base64QrCode, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        printBitmap(bitmap, alignment)
    }

    /**
     * Set text alignment
     */
    @Throws(InnerPrinterException::class)
    fun setAlignment(alignment: Int) {
        checkPrinterReady()
        printerService?.setAlignment(alignment, null)
    }

    /**
     * Set font size
     */
    @Throws(InnerPrinterException::class)
    fun setFontSize(size: Float) {
        checkPrinterReady()
        printerService?.setFontSize(size, null)
    }

    /**
     * Enable/disable bold text
     */
    @Throws(InnerPrinterException::class)
    fun setBold(enable: Boolean) {
        checkPrinterReady()
        val command = if (enable) byteArrayOf(0x1B, 0x45, 0x01) else byteArrayOf(0x1B, 0x45, 0x00)
        printerService?.sendRAWData(command, null)
    }

    /**
     * Feed paper (advance by n lines)
     */
    @Throws(InnerPrinterException::class)
    fun feedPaper(lines: Int = 3) {
        checkPrinterReady()
        printerService?.lineWrap(lines, null)
    }

    /**
     * Cut paper (if printer supports it)
     */
    @Throws(InnerPrinterException::class)
    fun cutPaper() {
        checkPrinterReady()
        printerService?.cutPaper(null)
    }

    /**
     * Get printer status asynchronously
     */
    suspend fun getPrinterStatus(): PrinterStatus = suspendCancellableCoroutine { continuation ->
        try {
            checkPrinterReady()
            printerService?.updatePrinterState()

            // Query printer status codes
            printerService?.let { service ->
                // Sunmi printer status codes:
                // 1: Normal, 2: Preparing, 3: Abnormal communication, 4: Out of paper
                // 5: Overheated, 8: Cover open, 9: Cutter error, 505: Firmware upgrade

                val callback = object : InnerResultCallback() {
                    override fun onRunResult(isSuccess: Boolean) {
                        if (continuation.isActive) {
                            continuation.resume(if (isSuccess) PrinterStatus.READY else PrinterStatus.ERROR)
                        }
                    }

                    override fun onReturnString(result: String?) {
                        // Not used for status check
                    }

                    override fun onRaiseException(code: Int, msg: String?) {
                        Log.e(TAG, "Printer error: code=$code, msg=$msg")
                        if (continuation.isActive) {
                            continuation.resume(PrinterStatus.fromCode(code))
                        }
                    }

                    override fun onPrintResult(code: Int, msg: String?) {
                        // Not used for status check
                    }
                }

                service.sendRAWData(byteArrayOf(0x10, 0x04, 0x01), callback) // Query status
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get printer status", e)
            if (continuation.isActive) {
                continuation.resume(PrinterStatus.ERROR)
            }
        }
    }

    /**
     * Check if printer is ready, throw exception if not
     */
    private fun checkPrinterReady() {
        if (!isReady()) {
            throw InnerPrinterException("Printer not connected or not ready")
        }
    }

    /**
     * Printer status enumeration
     */
    enum class PrinterStatus(val code: Int, val message: String) {
        READY(1, "Prêt"),
        PREPARING(2, "En préparation"),
        ABNORMAL_COMMUNICATION(3, "Erreur de communication"),
        OUT_OF_PAPER(4, "Papier épuisé"),
        OVERHEATED(5, "Surchauffe"),
        COVER_OPEN(8, "Couvercle ouvert"),
        CUTTER_ERROR(9, "Erreur de coupe"),
        FIRMWARE_UPGRADE(505, "Mise à jour firmware"),
        ERROR(-1, "Erreur");

        companion object {
            fun fromCode(code: Int): PrinterStatus {
                return entries.find { it.code == code } ?: ERROR
            }
        }
    }
}
