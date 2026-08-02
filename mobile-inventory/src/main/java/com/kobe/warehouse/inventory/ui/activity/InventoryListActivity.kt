package com.kobe.warehouse.inventory.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.kobe.warehouse.inventory.R
import com.kobe.warehouse.inventory.data.repository.InventoryRepository
import com.kobe.warehouse.inventory.databinding.ActivityInventoryListBinding
import com.kobe.warehouse.inventory.sync.SyncManager
import com.kobe.warehouse.inventory.ui.adapter.InventoryAdapter
import com.kobe.warehouse.inventory.ui.viewmodel.InventoryListState
import com.kobe.warehouse.inventory.ui.viewmodel.InventoryListViewModel

/**
 * Liste des inventaires en cours — **écran racine** de l'application après connexion.
 *
 * Il n'y a pas d'écran d'accueil intermédiaire : l'opérateur ouvre l'application pour
 * compter, il arrive donc directement sur les inventaires. Même parcours que le module
 * de vente, où la connexion mène directement à la liste des ventes.
 */
class InventoryListActivity : BaseActivity() {

    private lateinit var binding: ActivityInventoryListBinding
    private val inventoryListViewModel: InventoryListViewModel by viewModels {
        InventoryListViewModelFactory(InventoryRepository(this))
    }
    private lateinit var inventoryAdapter: InventoryAdapter

    /** Un inventaire au moins est en cours : conditionne l'action de synchronisation */
    private var hasActiveInventory = false
    private var pendingSyncCount = 0

    /**
     * `onResume` suit immédiatement `onCreate` au premier affichage : sans ce drapeau,
     * le chargement initial serait lancé deux fois.
     */
    private var isFirstResume = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventoryListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Écran racine : pas de flèche de retour
        setupToolbar(binding.toolbar, showBack = false)

        setupRecyclerView()
        setupListeners()
        setupObservers()

        inventoryListViewModel.loadActiveInventories()
    }

    /**
     * Un inventaire ouvert depuis le poste web, ou une progression modifiée pendant le
     * comptage, doivent apparaître sans que l'opérateur ait à quitter l'application. Le
     * retour sur cet écran déclenche donc un rechargement.
     */
    override fun onResume() {
        super.onResume()
        if (isFirstResume) {
            isFirstResume = false
            return
        }
        triggerRefresh()
    }

    /**
     * Rechargement déclenché autrement que par le balayage. On allume malgré tout
     * l'indicateur du SwipeRefreshLayout : il se superpose à la liste existante, là où
     * la roue centrale masquerait un contenu déjà affiché.
     */
    private fun triggerRefresh() {
        binding.swipeRefresh.isRefreshing = true
        inventoryListViewModel.refreshInventories()
    }

    /** Le menu de base est remplacé : il porte ici la synchronisation en plus */
    override val showLogoutMenu: Boolean = false

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_inventory_list, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        // Sans inventaire en cours, il n'y a rien à synchroniser
        menu?.findItem(R.id.action_sync)?.isVisible = hasActiveInventory
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                triggerRefresh()
                true
            }

            R.id.action_sync -> {
                SyncManager.syncNow(this)
                val message = if (pendingSyncCount > 0) {
                    resources.getQuantityString(
                        R.plurals.sync_started_with_pending, pendingSyncCount, pendingSyncCount
                    )
                } else {
                    getString(R.string.sync_started)
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        inventoryAdapter = InventoryAdapter { inventory ->
            startActivity(
                Intent(this, InventoryDetailActivity::class.java).apply {
                    putExtra(EXTRA_INVENTORY_ID, inventory.id)
                    putExtra(EXTRA_INVENTORY_NAME, inventory.getDisplayName())
                }
            )
        }
        binding.rvInventories.adapter = inventoryAdapter
    }

    private fun setupListeners() {
        // La couleur par défaut de l'indicateur est un gris système, hors charte
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
        binding.swipeRefresh.setOnRefreshListener {
            inventoryListViewModel.refreshInventories()
        }
    }

    private fun setupObservers() {
        inventoryListViewModel.inventoryListState.observe(this) { state ->
            when (state) {
                is InventoryListState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }

                is InventoryListState.Loading -> {
                    // Un rafraîchissement affiche déjà l'indicateur du balayage : la roue
                    // centrale ne sert qu'au tout premier chargement, écran encore vide
                    binding.progressBar.visibility =
                        if (binding.swipeRefresh.isRefreshing) View.GONE else View.VISIBLE
                    binding.emptyState.visibility = View.GONE
                }

                is InventoryListState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    hasActiveInventory = state.inventories.isNotEmpty()
                    invalidateOptionsMenu()

                    // La liste est toujours soumise, y compris vide : c'est elle qui
                    // porte le geste de balayage, elle ne doit donc jamais être masquée
                    inventoryAdapter.submitList(state.inventories)
                    binding.emptyState.visibility =
                        if (state.inventories.isEmpty()) View.VISIBLE else View.GONE
                }

                is InventoryListState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    // L'état vide a été masqué par Loading : le rétablir si l'échec
                    // laisse l'écran sans rien, sinon l'opérateur voit une page blanche
                    binding.emptyState.visibility =
                        if (inventoryAdapter.itemCount == 0) View.VISIBLE else View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Saisies restant à envoyer : signalées sous le titre de la liste
        InventoryRepository(this).pendingSyncCount().asLiveData().observe(this) { count ->
            pendingSyncCount = count
            binding.toolbar.subtitle = if (count > 0) {
                resources.getQuantityString(R.plurals.pending_sync_lines, count, count)
            } else {
                null
            }
        }
    }

    class InventoryListViewModelFactory(
        private val inventoryRepository: InventoryRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InventoryListViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return InventoryListViewModel(inventoryRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        const val EXTRA_INVENTORY_ID = "extra_inventory_id"
        const val EXTRA_INVENTORY_NAME = "extra_inventory_name"
    }
}
