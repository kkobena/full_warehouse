package com.kobe.warehouse.sales.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.kobe.warehouse.sales.databinding.ViewLoadingStateBinding

/**
 * LoadingStateView
 * Reusable component for displaying loading states
 *
 * Features:
 * - Animated progress indicator
 * - Optional loading message
 * - Clean, consistent design across the app
 *
 * Usage:
 * ```xml
 * <com.kobe.warehouse.sales.ui.components.LoadingStateView
 *     android:id="@+id/loadingState"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     android:visibility="gone" />
 * ```
 *
 * ```kotlin
 * loadingState.setMessage("Chargement...")
 * loadingState.visibility = View.VISIBLE
 * ```
 */
class LoadingStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewLoadingStateBinding

    init {
        binding = ViewLoadingStateBinding.inflate(LayoutInflater.from(context), this, true)
        orientation = VERTICAL
    }

    /**
     * Set loading message
     */
    fun setMessage(message: String) {
        binding.tvMessage.text = message
    }

    /**
     * Show loading state
     */
    fun show(message: String = "Chargement...") {
        setMessage(message)
        visibility = VISIBLE
    }

    /**
     * Hide loading state
     */
    fun hide() {
        visibility = GONE
    }
}
