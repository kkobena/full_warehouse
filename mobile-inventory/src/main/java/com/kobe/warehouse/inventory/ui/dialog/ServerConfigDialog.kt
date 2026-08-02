package com.kobe.warehouse.inventory.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.kobe.warehouse.inventory.R
import com.kobe.warehouse.inventory.data.model.ServerConfig
import com.kobe.warehouse.inventory.databinding.DialogServerConfigBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Paramétrage de l'adresse du serveur.
 *
 * Aligné sur `sales-android/ui/dialog/ServerConfigDialog` : mêmes champs, même test de
 * connexion, mêmes messages. La taille du rouleau de reçu, propre à la vente, n'est pas
 * reprise.
 */
class ServerConfigDialog : DialogFragment() {

    private var _binding: DialogServerConfigBinding? = null
    private val binding get() = _binding!!

    private var onConfigSaved: ((ServerConfig) -> Unit)? = null
    private var currentConfig: ServerConfig = ServerConfig.default()

    companion object {
        private const val ARG_PROTOCOL = "config_protocol"
        private const val ARG_HOST = "config_host"
        private const val ARG_PORT = "config_port"

        fun newInstance(config: ServerConfig, onSave: (ServerConfig) -> Unit): ServerConfigDialog {
            return ServerConfigDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_PROTOCOL, config.protocol)
                    putString(ARG_HOST, config.host)
                    putString(ARG_PORT, config.port)
                }
                onConfigSaved = onSave
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            currentConfig = ServerConfig.create(
                protocol = args.getString(ARG_PROTOCOL, "http"),
                host = args.getString(ARG_HOST, ServerConfig.DEFAULT_HOST),
                port = args.getString(ARG_PORT, ServerConfig.DEFAULT_PORT)
            )
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
        loadCurrentConfig()
        setupButtons()
    }

    private fun loadCurrentConfig() {
        binding.protocolToggleGroup.check(
            if (currentConfig.protocol == "https") R.id.btnHttps else R.id.btnHttp
        )
        binding.hostInput.setText(currentConfig.host)
        binding.portInput.setText(currentConfig.port)
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener { saveConfiguration() }
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnTestConnection.setOnClickListener { testConnection() }
    }

    private fun selectedProtocol(): String =
        if (binding.protocolToggleGroup.checkedButtonId == R.id.btnHttps) "https" else "http"

    /** Valide les champs et retourne (hôte, port) si tout est correct */
    private fun validatedInputs(): Pair<String, String>? {
        val host = binding.hostInput.text.toString().trim()
        val port = binding.portInput.text.toString().trim()

        if (host.isEmpty()) {
            binding.hostInputLayout.error = getString(R.string.server_host_required)
            return null
        }
        if (port.isEmpty()) {
            binding.portInputLayout.error = getString(R.string.server_port_required)
            return null
        }
        val portNumber = port.toIntOrNull()
        if (portNumber == null || portNumber < 1 || portNumber > 65535) {
            binding.portInputLayout.error = getString(R.string.server_port_invalid)
            return null
        }

        binding.hostInputLayout.error = null
        binding.portInputLayout.error = null
        return host to port
    }

    private fun saveConfiguration() {
        val (host, port) = validatedInputs() ?: return
        onConfigSaved?.invoke(ServerConfig.create(selectedProtocol(), host, port))
        dismiss()
    }

    private fun testConnection() {
        val (host, port) = validatedInputs() ?: return

        showConnectionStatus(loading = true)

        lifecycleScope.launch {
            try {
                val reachable = withContext(Dispatchers.IO) {
                    pingServer("${selectedProtocol()}://$host:$port/api/account")
                }
                if (reachable) {
                    showConnectionStatus(true, getString(R.string.connection_ok))
                } else {
                    showConnectionStatus(false, getString(R.string.connection_unreachable))
                }
            } catch (e: Exception) {
                val msg = when (e) {
                    is SocketTimeoutException -> getString(R.string.connection_timeout)
                    is UnknownHostException -> getString(R.string.connection_unknown_host)
                    else -> getString(R.string.connection_error, e.message ?: "")
                }
                showConnectionStatus(false, msg)
            }
        }
    }

    /**
     * Un 401 vaut succès : le serveur répond, seule l'authentification manque —
     * c'est précisément ce qu'on veut vérifier avant de se connecter.
     */
    private fun pingServer(url: String): Boolean {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder().url(url).head().build()
        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful || response.code == 401
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun showConnectionStatus(
        success: Boolean = false,
        message: String = "",
        loading: Boolean = false
    ) {
        val context = requireContext()
        binding.connectionStatusCard.visibility = View.VISIBLE

        if (loading) {
            binding.btnTestConnection.isEnabled = false
            binding.btnTestConnection.text = getString(R.string.connection_testing)
            binding.statusMessage.text = getString(R.string.connection_testing_message)
            binding.statusMessage.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            binding.connectionStatusCard.setCardBackgroundColor(
                ContextCompat.getColor(context, android.R.color.transparent)
            )
            binding.connectionStatusCard.strokeColor =
                ContextCompat.getColor(context, R.color.text_secondary)
            return
        }

        binding.btnTestConnection.isEnabled = true
        binding.btnTestConnection.text = getString(R.string.test_connection)
        binding.statusMessage.text = message

        val colorRes = if (success) R.color.success else R.color.error
        val bgRes = if (success) R.color.success_light else R.color.error_light
        val iconRes = if (success) R.drawable.ic_check else R.drawable.ic_error

        binding.statusMessage.setTextColor(ContextCompat.getColor(context, colorRes))
        binding.connectionStatusCard.setCardBackgroundColor(ContextCompat.getColor(context, bgRes))
        binding.connectionStatusCard.strokeColor = ContextCompat.getColor(context, colorRes)
        binding.statusIcon.setImageResource(iconRes)
        binding.statusIcon.setColorFilter(ContextCompat.getColor(context, colorRes))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
