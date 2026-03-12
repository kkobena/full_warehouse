package com.kobe.warehouse.sales.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
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

class ServerConfigDialog : DialogFragment() {

    private var _binding: DialogServerConfigBinding? = null
    private val binding get() = _binding!!

    private var onConfigSaved: ((ServerConfig) -> Unit)? = null
    private var currentConfig: ServerConfig = ServerConfig.default()

    companion object {
        private const val ARG_CONFIG = "config"

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
            val host = args.getString(ARG_CONFIG + "_host", "10.0.2.2")
            val port = args.getString(ARG_CONFIG + "_port", "9080")
            val rollSize = args.getInt(ARG_CONFIG + "_rollSize", 58)
            currentConfig = ServerConfig(url ?: "", protocol, host, port, rollSize)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogServerConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCurrentConfig()
        setupButtons()
    }

    private fun loadCurrentConfig() {
        binding.protocolToggleGroup.check(
            if (currentConfig.protocol == "https") R.id.btnHttps else R.id.btnHttp
        )
        binding.hostInput.setText(currentConfig.host)
        binding.portInput.setText(currentConfig.port)
        binding.receiptRollSizeToggleGroup.check(
            if (currentConfig.receiptRollSize == 80) R.id.btn80mm else R.id.btn58mm
        )
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener { saveConfiguration() }
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnTestConnection.setOnClickListener { testConnection() }
    }

    private fun selectedProtocol(): String =
        if (binding.protocolToggleGroup.checkedButtonId == R.id.btnHttps) "https" else "http"

    private fun saveConfiguration() {
        val host = binding.hostInput.text.toString().trim()
        val port = binding.portInput.text.toString().trim()
        val rollSize = if (binding.receiptRollSizeToggleGroup.checkedButtonId == R.id.btn80mm) 80 else 58

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

        binding.hostInputLayout.error = null
        binding.portInputLayout.error = null

        onConfigSaved?.invoke(ServerConfig.create(selectedProtocol(), host, port, rollSize))
        dismiss()
    }

    private fun testConnection() {
        val host = binding.hostInput.text.toString().trim()
        val port = binding.portInput.text.toString().trim()

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

        binding.hostInputLayout.error = null
        binding.portInputLayout.error = null

        showConnectionStatus(loading = true)

        lifecycleScope.launch {
            try {
                val reachable = withContext(Dispatchers.IO) {
                    pingServer("${selectedProtocol()}://$host:$port/api/account")
                }
                if (reachable) {
                    showConnectionStatus(success = true, message = "✓ Connexion réussie")
                } else {
                    showConnectionStatus(success = false, message = "✗ Serveur inaccessible")
                }
            } catch (e: Exception) {
                val msg = when (e) {
                    is SocketTimeoutException -> "✗ Délai d'attente dépassé"
                    is UnknownHostException -> "✗ Hôte introuvable"
                    else -> "✗ Erreur: ${e.message ?: "Connexion échouée"}"
                }
                showConnectionStatus(success = false, message = msg)
            }
        }
    }

    private fun pingServer(url: String): Boolean {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder().url(url).head().build()
        return try {
            val response = client.newCall(request).execute()
            response.isSuccessful || response.code == 401
        } catch (e: Exception) {
            false
        }
    }

    private fun showConnectionStatus(success: Boolean = false, message: String = "", loading: Boolean = false) {
        binding.connectionStatusCard.visibility = View.VISIBLE
        if (loading) {
            binding.btnTestConnection.isEnabled = false
            binding.btnTestConnection.text = "Test en cours..."
            binding.statusMessage.text = "Test de connexion en cours..."
            binding.statusMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            binding.connectionStatusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
            binding.connectionStatusCard.strokeColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        } else {
            binding.btnTestConnection.isEnabled = true
            binding.btnTestConnection.text = "Tester la connexion"
            binding.statusMessage.text = message
            if (success) {
                binding.statusMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
                binding.connectionStatusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success_light))
                binding.connectionStatusCard.strokeColor = ContextCompat.getColor(requireContext(), R.color.success)
                binding.statusIcon.setImageResource(R.drawable.ic_check)
                binding.statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success))
            } else {
                binding.statusMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
                binding.connectionStatusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.error_light))
                binding.connectionStatusCard.strokeColor = ContextCompat.getColor(requireContext(), R.color.error)
                binding.statusIcon.setImageResource(R.drawable.ic_error)
                binding.statusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
