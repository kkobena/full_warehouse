package com.kobe.warehouse.sales.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.model.ServerConfig
import com.kobe.warehouse.sales.databinding.DialogServerConfigBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Dialog for configuring server connection settings
 * Similar to Tauri login form settings button
 */
class ServerConfigDialog : DialogFragment() {

    private var _binding: DialogServerConfigBinding? = null
    private val binding get() = _binding!!

    private var onConfigSaved: ((ServerConfig) -> Unit)? = null
    private var currentConfig: ServerConfig = ServerConfig.default()

    companion object {
        private const val ARG_CONFIG = "config"

        /**
         * Create new instance with current configuration
         */
        fun newInstance(config: ServerConfig, onSave: (ServerConfig) -> Unit): ServerConfigDialog {
            return ServerConfigDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_CONFIG + "_url", config.serverUrl)
                    putString(ARG_CONFIG + "_protocol", config.protocol)
                    putString(ARG_CONFIG + "_host", config.host)
                    putString(ARG_CONFIG + "_port", config.port)
                    putInt(ARG_CONFIG + "_rollSize", config.receiptRollSize)
                }
                onConfigSaved = onSave
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            val url = args.getString(ARG_CONFIG + "_url")
            val protocol = args.getString(ARG_CONFIG + "_protocol", "http")
            val host = args.getString(ARG_CONFIG + "_host", "10.0.2.2")  // Emulator host
            val port = args.getString(ARG_CONFIG + "_port", "9080")
            val rollSize = args.getInt(ARG_CONFIG + "_rollSize", 58)
            currentConfig = ServerConfig(url ?: "", protocol, host, port, rollSize)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogServerConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupProtocolSpinner()
        setupReceiptRollSizeSpinner()
        loadCurrentConfig()
        setupButtons()
    }

    /**
     * Setup protocol dropdown with HTTP/HTTPS options
     */
    private fun setupProtocolSpinner() {
        val protocols = arrayOf("http", "https")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            protocols
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.protocolSpinner.adapter = adapter
    }

    /**
     * Setup receipt roll size dropdown with 58mm and 80mm options
     */
    private fun setupReceiptRollSizeSpinner() {
        val rollSizes = arrayOf("58mm", "80mm")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            rollSizes
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.receiptRollSizeSpinner.adapter = adapter
    }

    /**
     * Load current server configuration into form fields
     */
    private fun loadCurrentConfig() {
        // Set protocol
        val protocolPosition = if (currentConfig.protocol == "https") 1 else 0
        binding.protocolSpinner.setSelection(protocolPosition)

        // Set host and port
        binding.hostInput.setText(currentConfig.host)
        binding.portInput.setText(currentConfig.port)

        // Set receipt roll size
        val rollSizePosition = if (currentConfig.receiptRollSize == 80) 1 else 0
        binding.receiptRollSizeSpinner.setSelection(rollSizePosition)

        // Update preview
        updatePreview()
    }

    /**
     * Setup button click listeners
     */
    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            saveConfiguration()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnTestConnection.setOnClickListener {
            testConnection()
        }

        // Update preview when inputs change
        binding.hostInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updatePreview()
            }
        })

        binding.portInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updatePreview()
            }
        })

        binding.protocolSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                updatePreview()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    /**
     * Update URL preview
     */
    private fun updatePreview() {
        val protocol = binding.protocolSpinner.selectedItem.toString()
        val host = binding.hostInput.text.toString().trim()
        val port = binding.portInput.text.toString().trim()

        val previewUrl = if (host.isNotEmpty() && port.isNotEmpty()) {
            "$protocol://$host:$port/"
        } else {
            "Please enter host and port"
        }

        binding.urlPreview.text = previewUrl
    }

    /**
     * Validate and save configuration
     */
    private fun saveConfiguration() {
        val protocol = binding.protocolSpinner.selectedItem.toString()
        val host = binding.hostInput.text.toString().trim()
        val port = binding.portInput.text.toString().trim()
        val rollSizeText = binding.receiptRollSizeSpinner.selectedItem.toString()
        val rollSize = if (rollSizeText.contains("80")) 80 else 58

        // Validation
        if (host.isEmpty()) {
            binding.hostInputLayout.error = "Host is required"
            return
        }

        if (port.isEmpty()) {
            binding.portInputLayout.error = "Port is required"
            return
        }

        val portNumber = port.toIntOrNull()
        if (portNumber == null || portNumber < 1 || portNumber > 65535) {
            binding.portInputLayout.error = "Invalid port (1-65535)"
            return
        }

        // Clear errors
        binding.hostInputLayout.error = null
        binding.portInputLayout.error = null

        // Create config and notify listener
        val config = ServerConfig.create(protocol, host, port, rollSize)
        onConfigSaved?.invoke(config)
        dismiss()
    }

    /**
     * Test connection to server
     * Pings the /api/account endpoint to verify server is accessible
     */
    private fun testConnection() {
        val protocol = binding.protocolSpinner.selectedItem.toString()
        val host = binding.hostInput.text.toString().trim()
        val port = binding.portInput.text.toString().trim()

        // Validate inputs first
        if (host.isEmpty()) {
            binding.hostInputLayout.error = "Host est requis"
            return
        }

        if (port.isEmpty()) {
            binding.portInputLayout.error = "Port est requis"
            return
        }

        val portNumber = port.toIntOrNull()
        if (portNumber == null || portNumber < 1 || portNumber > 65535) {
            binding.portInputLayout.error = "Port invalide (1-65535)"
            return
        }

        // Clear errors
        binding.hostInputLayout.error = null
        binding.portInputLayout.error = null

        // Build URL
        val testUrl = "$protocol://$host:$port/api/account"

        // Show loading state
        showConnectionStatus(loading = true)

        // Test connection using coroutines
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    pingServer(testUrl)
                }

                // Show success
                if (result) {
                    showConnectionStatus(
                        success = true,
                        message = "✓ Connexion réussie"
                    )
                } else {
                    showConnectionStatus(
                        success = false,
                        message = "✗ Serveur inaccessible"
                    )
                }
            } catch (e: Exception) {
                // Show error with specific message
                val errorMessage = when (e) {
                    is SocketTimeoutException -> "✗ Délai d'attente dépassé"
                    is UnknownHostException -> "✗ Hôte introuvable"
                    else -> "✗ Erreur: ${e.message ?: "Connexion échouée"}"
                }
                showConnectionStatus(
                    success = false,
                    message = errorMessage
                )
            }
        }
    }

    /**
     * Ping server to check if it's accessible
     * Makes a HEAD request to /api/account endpoint
     */
    private fun pingServer(url: String): Boolean {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(url)
            .head()  // Use HEAD to avoid downloading response body
            .build()

        return try {
            val response = client.newCall(request).execute()
            // Accept both successful responses and 401 (means server is there but need auth)
            response.isSuccessful || response.code == 401
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Show connection status card with result
     */
    private fun showConnectionStatus(
        success: Boolean = false,
        message: String = "",
        loading: Boolean = false
    ) {
        binding.connectionStatusCard.visibility = View.VISIBLE

        if (loading) {
            // Show loading state
            binding.btnTestConnection.isEnabled = false
            binding.btnTestConnection.text = "Test en cours..."
            binding.statusMessage.text = "Test de connexion en cours..."
            binding.statusMessage.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_secondary)
            )
            binding.connectionStatusCard.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), android.R.color.transparent)
            )
            binding.connectionStatusCard.strokeColor =
                ContextCompat.getColor(requireContext(), R.color.text_secondary)
        } else {
            // Show result
            binding.btnTestConnection.isEnabled = true
            binding.btnTestConnection.text = "Tester la connexion"
            binding.statusMessage.text = message

            if (success) {
                // Success state - green
                binding.statusMessage.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.success)
                )
                binding.connectionStatusCard.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.success_light)
                )
                binding.connectionStatusCard.strokeColor =
                    ContextCompat.getColor(requireContext(), R.color.success)
                binding.statusIcon.setImageResource(R.drawable.ic_check)
                binding.statusIcon.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.success)
                )
            } else {
                // Error state - red
                binding.statusMessage.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.error)
                )
                binding.connectionStatusCard.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.error_light)
                )
                binding.connectionStatusCard.strokeColor =
                    ContextCompat.getColor(requireContext(), R.color.error)
                binding.statusIcon.setImageResource(R.drawable.ic_error)
                binding.statusIcon.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.error)
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
