package com.kobe.warehouse.sales.printer

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterException
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService as SunmiService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Unified Printer Service
 * Automatically uses real Sunmi printer on Sunmi devices
 * or mock printer on other devices for development
 */
class UnifiedPrinterService(private val context: Context) {

    private var realPrinter: SunmiService? = null
    private var mockPrinter: MockSunmiPrinterService? = null
    private var isConnected = false
    private val useMock = !isSunmiDevice()

    companion object {
        private const val TAG = "UnifiedPrinter"

        const val FONT_SIZE_NORMAL = 24f
        const val FONT_SIZE_LARGE = 28f
        const val FONT_SIZE_XLARGE = 32f

        const val ALIGN_LEFT = 0
        const val ALIGN_CENTER = 1
        const val ALIGN_RIGHT = 2

        const val PAPER_WIDTH_80MM = 48
        const val PAPER_WIDTH_58MM = 32

        /**
         * Check if running on Sunmi device
         */
        fun isSunmiDevice(): Boolean {
            val manufacturer = Build.MANUFACTURER.lowercase()
            val model = Build.MODEL.lowercase()
            return manufacturer.contains("sunmi") ||
                   model.contains("sunmi") ||
                   model.contains("v2")
        }
    }

    /**
     * Connect to printer (real or mock)
     */
    suspend fun connect(): Boolean {
        return if (useMock) {
            mockPrinter = MockSunmiPrinterService(context)
            mockPrinter!!.connect()
        } else {
            connectRealPrinter()
        }
    }

    /**
     * Connect to real Sunmi printer
     */
    private suspend fun connectRealPrinter(): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            InnerPrinterManager.getInstance().bindService(
                context,
                object : InnerPrinterCallback() {
                    override fun onConnected(service: SunmiService?) {
                        realPrinter = service
                        isConnected = true
                        Log.d(TAG, "Real Sunmi printer connected")
                        continuation.resume(true)
                    }

                    override fun onDisconnected() {
                        isConnected = false
                        realPrinter = null
                        Log.d(TAG, "Real Sunmi printer disconnected")
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                }
            )
        } catch (e: InnerPrinterException) {
            Log.e(TAG, "Failed to connect to real printer", e)
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }

    /**
     * Disconnect from printer
     */
    fun disconnect() {
        if (useMock) {
            mockPrinter?.disconnect()
        } else {
            try {
                InnerPrinterManager.getInstance().unBindService(context, object : InnerPrinterCallback() {
                    override fun onConnected(service: SunmiService?) {}
                    override fun onDisconnected() {
                        isConnected = false
                        realPrinter = null
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting printer", e)
            }
        }
    }

    /**
     * Check if ready
     */
    fun isReady(): Boolean {
        return if (useMock) {
            mockPrinter?.isReady() ?: false
        } else {
            isConnected && realPrinter != null
        }
    }

    /**
     * Get printer status
     */
    fun getPrinterStatus(): Int {
        return if (useMock) {
            mockPrinter?.getPrinterStatus() ?: 0
        } else {
            // TODO: Get real printer status
            0 // NORMAL
        }
    }

    /**
     * Print text with formatting
     */
    fun printLine(
        text: String,
        alignment: Int = ALIGN_LEFT,
        fontSize: Float = FONT_SIZE_NORMAL,
        isBold: Boolean = false
    ) {
        if (useMock) {
            mockPrinter?.printTextWithAlignment(text, alignment, fontSize, isBold)
        } else {
            try {
                realPrinter?.setAlignment(alignment, null)
                realPrinter?.setFontSize(fontSize, null)
                if (isBold) {
                    realPrinter?.sendRAWData(byteArrayOf(0x1B, 0x45, 0x01), null) // Bold ON
                }
                realPrinter?.printText(text, null)
                realPrinter?.lineWrap(1, null)
                if (isBold) {
                    realPrinter?.sendRAWData(byteArrayOf(0x1B, 0x45, 0x00), null) // Bold OFF
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error printing line", e)
            }
        }
    }

    /**
     * Print empty line(s)
     */
    fun printEmptyLine(count: Int = 1) {
        if (useMock) {
            mockPrinter?.feedPaper(count)
        } else {
            try {
                realPrinter?.lineWrap(count, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error printing empty line", e)
            }
        }
    }

    /**
     * Print separator line
     */
    fun printSeparator(width: Int = PAPER_WIDTH_58MM, char: String = "-") {
        if (useMock) {
            mockPrinter?.printSeparator(char, width)
        } else {
            try {
                val separator = char.repeat(width)
                realPrinter?.printText(separator, null)
                realPrinter?.lineWrap(1, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error printing separator", e)
            }
        }
    }

    /**
     * Print label-value pair
     */
    fun printLabelValue(
        label: String,
        value: String,
        fontSize: Float = FONT_SIZE_NORMAL,
        paperWidth: Int = PAPER_WIDTH_58MM
    ) {
        if (useMock) {
            mockPrinter?.printLabelValue(label, value, fontSize, paperWidth)
        } else {
            try {
                val line = String.format("%-${paperWidth / 2}s %${paperWidth / 2}s", label, value)
                realPrinter?.setFontSize(fontSize, null)
                realPrinter?.printText(line, null)
                realPrinter?.lineWrap(1, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error printing label-value", e)
            }
        }
    }

    /**
     * Print table row (3 columns)
     */
    fun printTableRow(
        col1: String,
        col2: String,
        col3: String,
        fontSize: Float = FONT_SIZE_NORMAL,
        paperWidth: Int = PAPER_WIDTH_58MM
    ) {
        if (useMock) {
            mockPrinter?.printTableRow(col1, col2, col3, fontSize, paperWidth)
        } else {
            try {
                val width1 = (paperWidth * 0.5).toInt()
                val width2 = (paperWidth * 0.2).toInt()
                val width3 = (paperWidth * 0.3).toInt()

                val line = String.format(
                    "%-${width1}s %${width2}s %${width3}s",
                    col1.take(width1),
                    col2.take(width2),
                    col3.take(width3)
                )

                realPrinter?.setFontSize(fontSize, null)
                realPrinter?.printText(line, null)
                realPrinter?.lineWrap(1, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error printing table row", e)
            }
        }
    }

    /**
     * Print QR code
     */
    fun printQrCode(data: String, alignment: Int = ALIGN_CENTER, size: Int = 8) {
        if (useMock) {
            mockPrinter?.printQrCode(data, alignment, size)
        } else {
            try {
                realPrinter?.setAlignment(alignment, null)
                realPrinter?.printQRCode(data, size, 0, null)
                realPrinter?.lineWrap(1, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error printing QR code", e)
            }
        }
    }

    /**
     * Print bitmap image
     */
    fun printBitmap(bitmap: Bitmap, alignment: Int = ALIGN_CENTER) {
        if (useMock) {
            mockPrinter?.printBitmap(bitmap, alignment)
        } else {
            try {
                realPrinter?.setAlignment(alignment, null)
                realPrinter?.printBitmap(bitmap, null)
                realPrinter?.lineWrap(1, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error printing bitmap", e)
            }
        }
    }

    /**
     * Feed paper (blank lines)
     */
    fun feedPaper(lines: Int = 3) {
        if (useMock) {
            mockPrinter?.feedPaper(lines)
        } else {
            try {
                realPrinter?.lineWrap(lines, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error feeding paper", e)
            }
        }
    }

    /**
     * Cut paper
     */
    suspend fun cutPaper() {
        if (useMock) {
            mockPrinter?.cutPaper()
        } else {
            try {
                realPrinter?.cutPaper(null)
            } catch (e: Exception) {
                Log.e(TAG, "Error cutting paper", e)
            }
        }
    }

    /**
     * Print columns (multi-column layout)
     */
    fun printColumns(
        columns: List<String>,
        widths: List<Int>,
        alignments: List<Int>,
        fontSize: Float = FONT_SIZE_NORMAL
    ) {
        if (columns.size != widths.size || columns.size != alignments.size) {
            Log.e(TAG, "Column count mismatch")
            return
        }

        val line = buildString {
            columns.forEachIndexed { index, column ->
                val width = widths[index]
                val alignment = alignments[index]
                val text = column.take(width)

                when (alignment) {
                    ALIGN_LEFT -> append(String.format("%-${width}s", text))
                    ALIGN_RIGHT -> append(String.format("%${width}s", text))
                    ALIGN_CENTER -> {
                        val padding = (width - text.length) / 2
                        append(" ".repeat(padding.coerceAtLeast(0)))
                        append(text)
                        append(" ".repeat((width - text.length - padding).coerceAtLeast(0)))
                    }
                }
            }
        }

        printLine(line, ALIGN_LEFT, fontSize)
    }

    /**
     * Get device info
     */
    fun getDeviceInfo(): String {
        return """
            Manufacturer: ${Build.MANUFACTURER}
            Model: ${Build.MODEL}
            Device: ${Build.DEVICE}
            Is Sunmi: ${!useMock}
            Using: ${if (useMock) "Mock Printer" else "Real Sunmi Printer"}
        """.trimIndent()
    }
}
