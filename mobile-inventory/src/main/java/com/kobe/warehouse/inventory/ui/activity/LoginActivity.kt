package com.kobe.warehouse.inventory.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kobe.warehouse.inventory.R
import com.kobe.warehouse.inventory.data.repository.AuthRepository
import com.kobe.warehouse.inventory.databinding.ActivityLoginBinding
import com.kobe.warehouse.inventory.ui.dialog.ServerConfigDialog
import com.kobe.warehouse.inventory.ui.viewmodel.LoginState
import com.kobe.warehouse.inventory.ui.viewmodel.LoginViewModel
import com.kobe.warehouse.inventory.utils.TokenManager

/**
 * Écran de connexion.
 *
 * Aligné sur `sales-android/ui/activity/LoginActivity` : même parcours (paramétrage du
 * serveur accessible avant connexion, mémorisation des identifiants, connexion
 * automatique) et même charte graphique.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupUI()
        setupObservers()

        viewModel.autoLogin()
    }

    /**
     * La ViewModel est reconstruite explicitement (et non par `by viewModels`) afin de
     * pouvoir la recréer après un changement d'adresse serveur.
     */
    private fun setupViewModel() {
        val repository = AuthRepository(TokenManager(this))
        viewModel = ViewModelProvider(this, LoginViewModelFactory(repository))[LoginViewModel::class.java]
    }

    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            viewModel.login(
                binding.etUsername.text.toString(),
                binding.etPassword.text.toString(),
                binding.cbRememberMe.isChecked
            )
        }

        binding.btnSettings.setOnClickListener { showServerConfigDialog() }

        binding.etUsername.requestFocus()
    }

    /**
     * Paramétrage de l'adresse du serveur. Après enregistrement, l'activité est recréée
     * pour que le client HTTP soit reconstruit avec la nouvelle URL.
     */
    private fun showServerConfigDialog() {
        val tokenManager = TokenManager(this)
        val dialog = ServerConfigDialog.newInstance(tokenManager.getServerConfig()) { newConfig ->
            tokenManager.saveServerConfig(newConfig)
            viewModelStore.clear()
            recreate()
        }
        dialog.show(supportFragmentManager, "ServerConfigDialog")
    }

    private fun setupObservers() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Idle -> {
                    showLoading(false)
                    binding.cardErrorMessage.visibility = View.GONE
                }

                is LoginState.Loading -> {
                    showLoading(true)
                    binding.cardErrorMessage.visibility = View.GONE
                }

                is LoginState.Success -> {
                    showLoading(false)
                    binding.cardErrorMessage.visibility = View.GONE
                    // Directement sur les inventaires : pas d'écran d'accueil
                    // intermédiaire, comme dans le module de vente
                    startActivity(
                        Intent(this, InventoryListActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    finish()
                }

                is LoginState.Error -> {
                    showLoading(false)
                    binding.cardErrorMessage.visibility = View.VISIBLE
                    binding.tvErrorMessage.text = state.message
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Pré-remplissage si « se souvenir de moi » était coché
        viewModel.savedCredentials.observe(this) { credentials ->
            credentials?.let {
                binding.etUsername.setText(it.first)
                binding.etPassword.setText(it.second)
            }
        }

        viewModel.rememberMe.observe(this) { remember ->
            binding.cbRememberMe.isChecked = remember
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text =
            getString(if (loading) R.string.connecting else R.string.login)
    }

    class LoginViewModelFactory(
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
