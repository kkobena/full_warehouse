package com.kobe.warehouse.sales.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

/**
 * TextWatcher that auto-formats date input as dd/mm/yyyy.
 * Inserts '/' separators automatically as the user types.
 * Converts dd/mm/yyyy to yyyy-mm-dd (ISO) for backend.
 */
class DateInputFormatter : TextWatcher {

    private var isFormatting = false
    private var deletingSlash = false
    private var prevLength = 0

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        prevLength = s?.length ?: 0
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        deletingSlash = before == 1 && count == 0 && s != null && start < s.length &&
            (start == 2 || start == 5)
    }

    override fun afterTextChanged(s: Editable?) {
        if (isFormatting || s == null) return
        isFormatting = true

        // Remove non-digit characters
        val digits = s.toString().replace("/", "")

        val formatted = StringBuilder()
        for (i in digits.indices) {
            if (i == 2 || i == 4) formatted.append('/')
            if (i >= 8) break // max 8 digits (ddmmyyyy)
            formatted.append(digits[i])
        }

        s.replace(0, s.length, formatted.toString())

        isFormatting = false
    }

    companion object {
        /**
         * Apply date formatting to an EditText
         */
        fun attach(editText: EditText) {
            editText.addTextChangedListener(DateInputFormatter())
        }

        /**
         * Convert dd/mm/yyyy display format to yyyy-mm-dd (ISO) for backend
         * Returns null if input is empty or invalid
         */
        fun toIsoDate(displayDate: String?): String? {
            if (displayDate.isNullOrBlank()) return null
            val parts = displayDate.trim().split("/")
            if (parts.size != 3 || parts[2].length != 4) return null
            return "${parts[2]}-${parts[1]}-${parts[0]}"
        }
    }
}
