package com.kobe.warehouse.reports.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.kobe.warehouse.reports.PharmaReportApplication
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.databinding.ActivityServerConfigBinding
import kotlinx.coroutines.launch

/**
 * Server configuration activity - allows setting the backend server URL.
 */
class ServerConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServerConfigBinding

    companion object {
        private const val DEFAULT_URL = "http://192.168.1.100:9080/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServerConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadSavedUrl()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadSavedUrl() {
        val tokenManager = PharmaReportApplication.getTokenManager()
        val savedUrl = tokenManager.getServerUrl()
        if (savedUrl.isNotEmpty()) {
            binding.etServerUrl.setText(savedUrl)
        }
    }

    private fun setupListeners() {
        binding.btnTestConnection.setOnClickListener {
            testConnection()
        }

        binding.btnSave.setOnClickListener {
            saveServerUrl()
        }

        binding.btnReset.setOnClickListener {
            resetToDefault()
        }
    }

    private fun testConnection() {
        val url = binding.etServerUrl.text?.toString()?.trim() ?: ""
        if (!validateUrl(url)) {
            return
        }

        binding.loadingOverlay.isVisible = true

        lifecycleScope.launch {
            val result = PharmaReportApplication.getRepository().testConnection(url)

            binding.loadingOverlay.isVisible = false

            result.fold(
                onSuccess = {
                    showStatus(true, getString(R.string.server_connection_success))
                },
                onFailure = { exception ->
                    showStatus(false, exception.message ?: getString(R.string.server_connection_failed))
                }
            )
        }
    }

    private fun saveServerUrl() {
        val url = binding.etServerUrl.text?.toString()?.trim() ?: ""
        if (!validateUrl(url)) {
            return
        }

        val tokenManager = PharmaReportApplication.getTokenManager()
        tokenManager.saveServerUrl(url)

        Snackbar.make(binding.root, R.string.save, Snackbar.LENGTH_SHORT).show()
        finish()
    }

    private fun resetToDefault() {
        binding.etServerUrl.setText(DEFAULT_URL)
        hideStatus()
    }

    private fun validateUrl(url: String): Boolean {
        if (url.isBlank()) {
            binding.tilServerUrl.error = "L'URL est requise"
            return false
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            binding.tilServerUrl.error = "L'URL doit commencer par http:// ou https://"
            return false
        }

        binding.tilServerUrl.error = null
        return true
    }

    private fun showStatus(isSuccess: Boolean, message: String) {
        binding.llStatus.isVisible = true
        binding.tvStatus.text = message

        val statusColor = if (isSuccess) R.color.success else R.color.error
        binding.viewStatusIndicator.backgroundTintList =
            ContextCompat.getColorStateList(this, statusColor)
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, statusColor))
    }

    private fun hideStatus() {
        binding.llStatus.isVisible = false
    }
}
