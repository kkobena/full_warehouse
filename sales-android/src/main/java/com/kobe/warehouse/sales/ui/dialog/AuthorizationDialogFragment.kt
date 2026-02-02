package com.kobe.warehouse.sales.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.databinding.DialogAuthorizationBinding
import com.kobe.warehouse.sales.data.repository.AuthRepository
import com.kobe.warehouse.sales.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Dialog for requesting authorization from another user with required permission
 * Used for sensitive operations like price modification or line deletion
 */
class AuthorizationDialogFragment(
    private val requiredPermission: String,
    private val operationName: String,
    private val onAuthorized: (userId: Int) -> Unit
) : DialogFragment() {

    private var _binding: DialogAuthorizationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAuthorizationBinding.inflate(LayoutInflater.from(requireContext()))

        binding.tvAuthMessage.text = "Cette opération nécessite la permission : $operationName"

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Autorisation requise")
            .setView(binding.root)
            .setPositiveButton("Valider") { _, _ ->
                val username = binding.etAuthUsername.text.toString().trim()
                val password = binding.etAuthPassword.text.toString()

                if (username.isBlank()) {
                    Toast.makeText(requireContext(), "Veuillez saisir le nom d'utilisateur", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (password.isBlank()) {
                    Toast.makeText(requireContext(), "Veuillez saisir le mot de passe", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                validateCredentials(username, password)
            }
            .setNegativeButton("Annuler", null)
            .create()

        return dialog
    }

    private fun validateCredentials(username: String, password: String) {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.tilAuthUsername.isEnabled = false
        binding.tilAuthPassword.isEnabled = false

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val tokenManager = TokenManager(requireContext())
                val retrofit = com.kobe.warehouse.sales.utils.ApiClient.create(tokenManager = tokenManager)
                val authApiService = retrofit.create(com.kobe.warehouse.sales.data.api.AuthApiService::class.java)
                val authRepository = AuthRepository(authApiService, tokenManager)

                val result = withContext(Dispatchers.IO) {
                    authRepository.validateUserPermission(username, password, requiredPermission)
                }

                result.fold(
                    onSuccess = { userId ->
                        Toast.makeText(requireContext(), "Autorisation accordée", Toast.LENGTH_SHORT).show()
                        onAuthorized(userId)
                        dismiss()
                    },
                    onFailure = { error ->
                        binding.progressBar.visibility = android.view.View.GONE
                        binding.tilAuthUsername.isEnabled = true
                        binding.tilAuthPassword.isEnabled = true
                        Toast.makeText(requireContext(), "Erreur : ${error.message}", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                binding.progressBar.visibility = android.view.View.GONE
                binding.tilAuthUsername.isEnabled = true
                binding.tilAuthPassword.isEnabled = true
                Toast.makeText(requireContext(), "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val PERMISSION_MODIFY_PRICE = "PR_MODIFICATION_PRIX_VENTE"
        const val PERMISSION_DELETE_PRODUCT = "PR_SUPPRIME_PRODUIT_VENTE"
        const val PERMISSION_FORCE_STOCK = "PR_FORCE_STOCK"
    }
}
