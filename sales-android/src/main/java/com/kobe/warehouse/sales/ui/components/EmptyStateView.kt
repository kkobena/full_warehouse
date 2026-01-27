package com.kobe.warehouse.sales.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import com.kobe.warehouse.sales.databinding.ViewEmptyStateBinding

/**
 * EmptyStateView
 * Reusable component for displaying empty states
 *
 * Features:
 * - Customizable icon
 * - Customizable title and message
 * - Clean, consistent design across the app
 *
 * Usage:
 * ```xml
 * <com.kobe.warehouse.sales.ui.components.EmptyStateView
 *     android:id="@+id/emptyState"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     android:visibility="gone" />
 * ```
 *
 * ```kotlin
 * emptyState.setIcon(R.drawable.ic_empty_cart)
 * emptyState.setTitle("Aucune vente")
 * emptyState.setMessage("Créez une nouvelle vente pour commencer")
 * emptyState.visibility = View.VISIBLE
 * ```
 */
class EmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewEmptyStateBinding

    init {
        binding = ViewEmptyStateBinding.inflate(LayoutInflater.from(context), this, true)
        orientation = VERTICAL
    }

    /**
     * Set icon
     */
    fun setIcon(@DrawableRes iconRes: Int) {
        binding.ivIcon.setImageResource(iconRes)
    }

    /**
     * Set title
     */
    fun setTitle(title: String) {
        binding.tvTitle.text = title
    }

    /**
     * Set message
     */
    fun setMessage(message: String) {
        binding.tvMessage.text = message
    }

    /**
     * Configure all at once
     */
    fun configure(
        @DrawableRes iconRes: Int,
        title: String,
        message: String
    ) {
        setIcon(iconRes)
        setTitle(title)
        setMessage(message)
    }
}
