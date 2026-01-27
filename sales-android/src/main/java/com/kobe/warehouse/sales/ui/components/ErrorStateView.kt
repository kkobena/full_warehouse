package com.kobe.warehouse.sales.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.kobe.warehouse.sales.databinding.ViewErrorStateBinding

/**
 * ErrorStateView
 * Reusable component for displaying error states with retry option
 *
 * Features:
 * - Error icon
 * - Error message
 * - Retry button
 * - Clean, consistent design across the app
 *
 * Usage:
 * ```xml
 * <com.kobe.warehouse.sales.ui.components.ErrorStateView
 *     android:id="@+id/errorState"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     android:visibility="gone" />
 * ```
 *
 * ```kotlin
 * errorState.setMessage("Erreur de chargement")
 * errorState.setOnRetryClickListener {
 *     // Retry action
 * }
 * errorState.visibility = View.VISIBLE
 * ```
 */
class ErrorStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewErrorStateBinding

    init {
        binding = ViewErrorStateBinding.inflate(LayoutInflater.from(context), this, true)
        orientation = VERTICAL
    }

    /**
     * Set error message
     */
    fun setMessage(message: String) {
        binding.tvMessage.text = message
    }

    /**
     * Set retry button click listener
     */
    fun setOnRetryClickListener(listener: OnClickListener) {
        binding.btnRetry.setOnClickListener(listener)
    }

    /**
     * Show error state with message
     */
    fun show(message: String, onRetry: () -> Unit) {
        setMessage(message)
        setOnRetryClickListener { onRetry() }
        visibility = VISIBLE
    }

    /**
     * Hide error state
     */
    fun hide() {
        visibility = GONE
    }
}
