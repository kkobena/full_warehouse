package com.kobe.warehouse.reports.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.kobe.warehouse.reports.databinding.ComponentOfflineBannerBinding

/**
 * Banner component to display offline mode status.
 */
class OfflineBanner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ComponentOfflineBannerBinding

    private var onRetryClickListener: (() -> Unit)? = null
    private var pendingActionsCount: Int = 0

    init {
        binding = ComponentOfflineBannerBinding.inflate(LayoutInflater.from(context), this, true)
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnRetry.setOnClickListener {
            onRetryClickListener?.invoke()
        }
    }

    /**
     * Show offline banner.
     */
    fun showOffline(pendingActions: Int = 0) {
        this.pendingActionsCount = pendingActions
        isVisible = true

        val message = if (pendingActions > 0) {
            "Vous êtes hors ligne. $pendingActions action(s) en attente."
        } else {
            "Vous êtes hors ligne."
        }

        binding.tvOfflineMessage.text = message
    }

    /**
     * Show syncing state.
     */
    fun showSyncing() {
        isVisible = true
        binding.tvOfflineMessage.text = "Synchronisation en cours..."
        binding.progressSync.isVisible = true
        binding.btnRetry.isVisible = false
    }

    /**
     * Hide banner.
     */
    fun hide() {
        isVisible = false
        binding.progressSync.isVisible = false
        binding.btnRetry.isVisible = true
    }

    /**
     * Set retry click listener.
     */
    fun setOnRetryClickListener(listener: () -> Unit) {
        this.onRetryClickListener = listener
    }
}
