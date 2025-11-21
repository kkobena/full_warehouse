package com.kobe.warehouse.sales.printer

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mock implementation of Sunmi Printer Service for testing on non-Sunmi devices
 * Logs all print commands instead of actually printing
 */
class MockSunmiPrinterService(private val context: Context) {

    private var isConnected = false
    private val printLog = StringBuilder()

    companion object {
        private const val TAG = "MockSunmiPrinter"

        // Font sizes
        const val FONT_SIZE_NORMAL = 24f
        const val FONT_SIZE_LARGE = 28f
        const val FONT_SIZE_XLARGE = 32f

        // Alignment
        const val ALIGN_LEFT = 0
        const val ALIGN_CENTER = 1
        const val ALIGN_RIGHT = 2

        // Paper width
        const val PAPER_WIDTH_80MM = 48
        const val PAPER_WIDTH_58MM = 32
    }

    /**
     * Mock connect - always succeeds
     */
    suspend fun connect(): Boolean {
        isConnected = true
        printLog.clear()
        printLog.appendLine("========== MOCK RECEIPT START ==========")
        Log.d(TAG, "Mock printer connected")

        // Show toast on main thread
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Mock Printer: Connected", Toast.LENGTH_SHORT).show()
        }

        return true
    }

    /**
     * Mock disconnect
     */
    fun disconnect() {
        isConnected = false
        Log.d(TAG, "Mock printer disconnected")
        Log.d(TAG, "Final receipt:\n$printLog")
    }

    /**
     * Check if ready
     */
    fun isReady(): Boolean = isConnected

    /**
     * Mock get printer status
     */
    fun getPrinterStatus(): Int {
        Log.d(TAG, "getPrinterStatus() -> NORMAL")
        return 0 // NORMAL
    }

    /**
     * Mock print text
     */
    fun printText(text: String, fontSize: Float = FONT_SIZE_NORMAL, isBold: Boolean = false) {
        val boldPrefix = if (isBold) "[BOLD] " else ""
        val sizePrefix = when (fontSize) {
            FONT_SIZE_LARGE -> "[LARGE] "
            FONT_SIZE_XLARGE -> "[XLARGE] "
            else -> ""
        }
        val line = "$boldPrefix$sizePrefix$text"
        printLog.appendLine(line)
        Log.d(TAG, "printText: $line")
    }

    /**
     * Mock print text with alignment
     */
    fun printTextWithAlignment(
        text: String,
        alignment: Int = ALIGN_LEFT,
        fontSize: Float = FONT_SIZE_NORMAL,
        isBold: Boolean = false
    ) {
        val alignPrefix = when (alignment) {
            ALIGN_CENTER -> "[CENTER] "
            ALIGN_RIGHT -> "[RIGHT] "
            else -> "[LEFT] "
        }
        val boldPrefix = if (isBold) "[BOLD] " else ""
        val sizePrefix = when (fontSize) {
            FONT_SIZE_LARGE -> "[LARGE] "
            FONT_SIZE_XLARGE -> "[XLARGE] "
            else -> ""
        }
        val line = "$alignPrefix$boldPrefix$sizePrefix$text"
        printLog.appendLine(line)
        Log.d(TAG, "printTextWithAlignment: $line")
    }

    /**
     * Mock print line (alias for printTextWithAlignment)
     */
    fun printLine(
        text: String,
        alignment: Int = ALIGN_LEFT,
        fontSize: Float = FONT_SIZE_NORMAL,
        isBold: Boolean = false
    ) {
        printTextWithAlignment(text, alignment, fontSize, isBold)
    }

    /**
     * Mock print empty line(s)
     */
    fun printEmptyLine(count: Int = 1) {
        repeat(count) {
            printLog.appendLine("")
        }
        Log.d(TAG, "printEmptyLine: $count lines")
    }

    /**
     * Mock print separator line
     */
    fun printSeparator(char: String = "-", paperWidth: Int = PAPER_WIDTH_58MM) {
        val separator = char.repeat(paperWidth)
        printLog.appendLine(separator)
        Log.d(TAG, "printSeparator: $separator")
    }

    /**
     * Mock print two columns (label: value)
     */
    fun printLabelValue(
        label: String,
        value: String,
        fontSize: Float = FONT_SIZE_NORMAL,
        paperWidth: Int = PAPER_WIDTH_58MM
    ) {
        val line = String.format("%-${paperWidth / 2}s %${paperWidth / 2}s", label, value)
        printLog.appendLine(line)
        Log.d(TAG, "printLabelValue: $line")
    }

    /**
     * Mock print table row
     */
    fun printTableRow(
        col1: String,
        col2: String,
        col3: String,
        fontSize: Float = FONT_SIZE_NORMAL,
        paperWidth: Int = PAPER_WIDTH_58MM
    ) {
        val width1 = (paperWidth * 0.5).toInt()
        val width2 = (paperWidth * 0.2).toInt()
        val width3 = (paperWidth * 0.3).toInt()

        val line = String.format(
            "%-${width1}s %${width2}s %${width3}s",
            col1.take(width1),
            col2.take(width2),
            col3.take(width3)
        )
        printLog.appendLine(line)
        Log.d(TAG, "printTableRow: $line")
    }

    /**
     * Mock print QR code
     */
    fun printQrCode(data: String, alignment: Int = ALIGN_CENTER, size: Int = 8) {
        val alignPrefix = when (alignment) {
            ALIGN_CENTER -> "[CENTER] "
            ALIGN_RIGHT -> "[RIGHT] "
            else -> "[LEFT] "
        }
        val line = "$alignPrefix[QR CODE: size=$size] Data: ${data.take(50)}..."
        printLog.appendLine(line)
        Log.d(TAG, "printQrCode: $line")
    }

    /**
     * Mock print bitmap/image
     */
    fun printBitmap(bitmap: Bitmap, alignment: Int = ALIGN_CENTER) {
        val alignPrefix = when (alignment) {
            ALIGN_CENTER -> "[CENTER] "
            ALIGN_RIGHT -> "[RIGHT] "
            else -> "[LEFT] "
        }
        val line = "$alignPrefix[IMAGE: ${bitmap.width}x${bitmap.height}]"
        printLog.appendLine(line)
        Log.d(TAG, "printBitmap: $line")
    }

    /**
     * Mock print columns (multi-column layout)
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

        val sizePrefix = when (fontSize) {
            FONT_SIZE_LARGE -> "[LARGE] "
            FONT_SIZE_XLARGE -> "[XLARGE] "
            else -> ""
        }

        printLog.appendLine("$sizePrefix$line")
        Log.d(TAG, "printColumns: $sizePrefix$line")
    }

    /**
     * Mock feed paper (blank lines)
     */
    fun feedPaper(lines: Int = 3) {
        repeat(lines) {
            printLog.appendLine("")
        }
        Log.d(TAG, "feedPaper: $lines lines")
    }

    /**
     * Mock cut paper
     */
    suspend fun cutPaper() {
        printLog.appendLine("========== MOCK RECEIPT END ==========")
        printLog.appendLine("[CUT PAPER]")
        Log.d(TAG, "cutPaper: Receipt cut")

        // Show the final receipt in log
        Log.i(TAG, "\n$printLog")

        // Show toast on main thread
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                "Mock Print: Receipt logged to console",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Get the logged receipt content
     */
    fun getReceiptLog(): String = printLog.toString()
}
