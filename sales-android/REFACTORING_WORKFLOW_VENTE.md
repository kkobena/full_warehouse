# Refactorisation Workflow Vente - Tiers Payants et Ayants Droits

## ✅ Composants créés

### 1. Layouts

#### **`item_customer_tiers_payant.xml`**
Layout pour afficher un tiers payant du client avec :
- **Nom** du tiers payant
- **Rang (Priorité)** : R0, C1, C2, C3 (visible uniquement en mode Assurance)
- **Taux de couverture** : pourcentage
- **Champ Numéro de bon** : saisie pour chaque tiers payant
- **Bouton Modifier taux** (mode Assurance uniquement)
- **Bouton Retirer** (mode Assurance uniquement)

#### **`include_customer_info_display.xml`**
Layout pour afficher les informations client complètes :
- **Informations client** : Nom, Prénom, Matricule
- **Section Ayant droit** (Assurance uniquement) :
  - Affichage de l'ayant droit sélectionné
  - Bouton "Sélectionner" un ayant droit existant
  - Bouton "Créer" un nouvel ayant droit
  - Bouton pour retirer l'ayant droit
- **Section Tiers Payants** :
  - RecyclerView avec la liste des tiers payants
  - Bouton "Ajouter tiers payant" (Assurance uniquement)

### 2. Adapter

#### **`CustomerTiersPayantAdapter.kt`**
Adapter pour gérer l'affichage des tiers payants avec deux modes :

**Mode Carnet** (`isAssuranceMode = false`) :
- Affiche : Nom, Taux, Numéro de bon
- Masque : Rang, Boutons (Modifier/Retirer)

**Mode Assurance** (`isAssuranceMode = true`) :
- Affiche : Nom, Rang, Taux, Numéro de bon
- Affiche les boutons : Modifier taux, Retirer

**Callbacks** :
```kotlin
onNumeroBonChanged: (ClientTiersPayant, String) -> Unit
onTauxModifyClicked: (ClientTiersPayant) -> Unit
onRemoveClicked: (ClientTiersPayant) -> Unit
```

---

## 🔧 Intégration dans UnifiedSaleActivity

### Étapes d'intégration

#### 1. Ajouter le layout dans `activity_unified_sale.xml`

Remplacer ou compléter les includes existants :

```xml
<!-- Section affichage client et données -->
<include
    android:id="@+id/includeCustomerInfoDisplay"
    layout="@layout/include_customer_info_display"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:visibility="gone" />
```

#### 2. Créer l'adapter dans `UnifiedSaleActivity.kt`

```kotlin
class UnifiedSaleActivity : AppCompatActivity() {

    // Ajouter les adapters
    private lateinit var tiersPayantsAdapter: CustomerTiersPayantAdapter

    private fun setupAdapters() {
        // ... existing adapters ...

        // Adapter tiers payants (sera initialisé selon le type de vente)
        tiersPayantsAdapter = CustomerTiersPayantAdapter(
            isAssuranceMode = false, // Sera mis à jour dynamiquement
            onNumeroBonChanged = { tiersPayant, numBon ->
                // Mettre à jour le numéro de bon
                viewModel.updateTiersPayantNumBon(tiersPayant, numBon)
            },
            onTauxModifyClicked = { tiersPayant ->
                // Afficher dialog pour modifier le taux
                showModifyTauxDialog(tiersPayant)
            },
            onRemoveClicked = { tiersPayant ->
                // Confirmer et retirer le tiers payant
                confirmRemoveTiersPayant(tiersPayant)
            }
        )

        binding.includeCustomerInfoDisplay.rvTiersPayants.apply {
            layoutManager = LinearLayoutManager(this@UnifiedSaleActivity)
            adapter = tiersPayantsAdapter
        }
    }
}
```

#### 3. Observer le client sélectionné et afficher les données

```kotlin
private fun observeViewModel() {
    // ... existing observers ...

    // Observer le client sélectionné
    viewModel.selectedCustomer.observe(this) { customer ->
        if (customer != null) {
            displayCustomerInfo(customer)
        } else {
            binding.includeCustomerInfoDisplay.root.visibility = View.GONE
        }
    }
}

private fun displayCustomerInfo(customer: Customer) {
    binding.includeCustomerInfoDisplay.apply {
        root.visibility = View.VISIBLE

        // Informations de base
        tvCustomerLastName.text = customer.lastName
        tvCustomerFirstName.text = customer.firstName

        // Matricule (visible pour Carnet et Assurance)
        val saleType = viewModel.currentSaleType.value
        if (saleType is SaleType.Carnet || saleType is SaleType.Assurance) {
            layoutMatricule.visibility = View.VISIBLE
            // Récupérer le matricule du premier tiers payant
            val matricule = customer.tiersPayants?.firstOrNull()?.num
            tvCustomerMatricule.text = matricule ?: "N/A"
        } else {
            layoutMatricule.visibility = View.GONE
        }

        // Section Tiers Payants
        if (saleType is SaleType.Carnet || saleType is SaleType.Assurance) {
            layoutTiersPayantsSection.visibility = View.VISIBLE
            displayTiersPayants(customer, saleType)
        } else {
            layoutTiersPayantsSection.visibility = View.GONE
        }

        // Section Ayant droit (Assurance uniquement)
        if (saleType is SaleType.Assurance) {
            layoutAyantDroitSection.visibility = View.VISIBLE
            setupAyantDroitSection(customer)
        } else {
            layoutAyantDroitSection.visibility = View.GONE
        }
    }
}

private fun displayTiersPayants(customer: Customer, saleType: SaleType) {
    // Récupérer les tiers payants du client
    val tiersPayants = customer.tiersPayants ?: emptyList()

    // Configurer l'adapter selon le type de vente
    val isAssuranceMode = saleType is SaleType.Assurance
    tiersPayantsAdapter = CustomerTiersPayantAdapter(
        isAssuranceMode = isAssuranceMode,
        onNumeroBonChanged = { tiersPayant, numBon ->
            viewModel.updateTiersPayantNumBon(tiersPayant, numBon)
        },
        onTauxModifyClicked = { tiersPayant ->
            showModifyTauxDialog(tiersPayant)
        },
        onRemoveClicked = { tiersPayant ->
            confirmRemoveTiersPayant(tiersPayant)
        }
    )
    binding.includeCustomerInfoDisplay.rvTiersPayants.adapter = tiersPayantsAdapter
    tiersPayantsAdapter.submitList(tiersPayants)

    // Bouton ajouter tiers payant (Assurance uniquement)
    if (isAssuranceMode) {
        binding.includeCustomerInfoDisplay.btnAddTiersPayant.visibility = View.VISIBLE
        binding.includeCustomerInfoDisplay.btnAddTiersPayant.setOnClickListener {
            showAddTiersPayantDialog(customer)
        }
    } else {
        binding.includeCustomerInfoDisplay.btnAddTiersPayant.visibility = View.GONE
    }
}

private fun setupAyantDroitSection(customer: Customer) {
    binding.includeCustomerInfoDisplay.apply {
        // Bouton sélectionner ayant droit
        btnSelectAyantDroit.setOnClickListener {
            showSelectAyantDroitDialog(customer)
        }

        // Bouton créer ayant droit
        btnAddAyantDroit.setOnClickListener {
            showCreateAyantDroitDialog(customer)
        }

        // Observer l'ayant droit sélectionné
        viewModel.selectedAyantDroit.observe(this@UnifiedSaleActivity) { ayantDroit ->
            if (ayantDroit != null) {
                layoutSelectedAyantDroit.visibility = View.VISIBLE
                tvAyantDroitName.text = "${ayantDroit.firstName} ${ayantDroit.lastName}"
                btnRemoveAyantDroit.setOnClickListener {
                    viewModel.removeAyantDroit()
                }
            } else {
                layoutSelectedAyantDroit.visibility = View.GONE
            }
        }
    }
}
```

#### 4. Ajouter les méthodes de dialogue

```kotlin
private fun showModifyTauxDialog(tiersPayant: ClientTiersPayant) {
    // TODO: Créer un dialog pour modifier le taux
    val input = android.widget.EditText(this)
    input.setText(tiersPayant.taux.toString())
    input.inputType = android.text.InputType.TYPE_CLASS_NUMBER

    MaterialAlertDialogBuilder(this)
        .setTitle("Modifier le taux de couverture")
        .setMessage("${tiersPayant.tiersPayantName}")
        .setView(input)
        .setPositiveButton("Modifier") { _, _ ->
            val newTaux = input.text.toString().toIntOrNull()
            if (newTaux != null && newTaux in 0..100) {
                viewModel.updateTiersPayantTaux(tiersPayant, newTaux)
            } else {
                Toast.makeText(this, "Taux invalide (0-100)", Toast.LENGTH_SHORT).show()
            }
        }
        .setNegativeButton("Annuler", null)
        .show()
}

private fun confirmRemoveTiersPayant(tiersPayant: ClientTiersPayant) {
    MaterialAlertDialogBuilder(this)
        .setTitle("Retirer le tiers payant ?")
        .setMessage("Voulez-vous retirer ${tiersPayant.tiersPayantName} de cette vente ?")
        .setPositiveButton("Retirer") { _, _ ->
            viewModel.removeTiersPayant(tiersPayant)
        }
        .setNegativeButton("Annuler", null)
        .show()
}

private fun showAddTiersPayantDialog(customer: Customer) {
    // TODO: Créer un dialog pour ajouter un tiers payant
    // Utiliser TiersPayantSelectorDialog ou créer un nouveau
}

private fun showSelectAyantDroitDialog(customer: Customer) {
    // TODO: Créer un dialog pour sélectionner parmi les ayants droits du client
    // Appeler customer.ayantDroits via le backend
}

private fun showCreateAyantDroitDialog(customer: Customer) {
    // TODO: Créer un dialog pour créer un nouvel ayant droit
    // Réutiliser AssureCustomerCreateDialogFragment ou créer un nouveau
}
```

---

## 📋 Méthodes à ajouter au ViewModel

Dans `UnifiedSaleViewModel.kt`, ajouter :

```kotlin
// LiveData pour l'ayant droit sélectionné (Assurance uniquement)
private val _selectedAyantDroit = MutableLiveData<Customer?>()
val selectedAyantDroit: LiveData<Customer?> = _selectedAyantDroit

fun selectAyantDroit(ayantDroit: Customer) {
    _selectedAyantDroit.value = ayantDroit
}

fun removeAyantDroit() {
    _selectedAyantDroit.value = null
}

fun updateTiersPayantNumBon(tiersPayant: ClientTiersPayant, numBon: String) {
    // Mettre à jour le numéro de bon dans la liste
    val customer = _selectedCustomer.value ?: return
    val updatedTiersPayants = customer.tiersPayants?.map {
        if (it.tiersPayantId == tiersPayant.tiersPayantId) {
            it.copy(numBon = numBon)
        } else {
            it
        }
    }
    _selectedCustomer.value = customer.copy(tiersPayants = updatedTiersPayants)
}

fun updateTiersPayantTaux(tiersPayant: ClientTiersPayant, newTaux: Int) {
    // Mettre à jour le taux dans la liste
    val customer = _selectedCustomer.value ?: return
    val updatedTiersPayants = customer.tiersPayants?.map {
        if (it.tiersPayantId == tiersPayant.tiersPayantId) {
            it.copy(taux = newTaux)
        } else {
            it
        }
    }
    _selectedCustomer.value = customer.copy(tiersPayants = updatedTiersPayants)
}

fun removeTiersPayant(tiersPayant: ClientTiersPayant) {
    // Retirer le tiers payant de la liste
    val customer = _selectedCustomer.value ?: return
    val updatedTiersPayants = customer.tiersPayants?.filter {
        it.tiersPayantId != tiersPayant.tiersPayantId
    }
    _selectedCustomer.value = customer.copy(tiersPayants = updatedTiersPayants)
}

fun addTiersPayant(tiersPayant: ClientTiersPayant) {
    // Ajouter un tiers payant à la liste
    val customer = _selectedCustomer.value ?: return
    val updatedTiersPayants = (customer.tiersPayants ?: emptyList()) + tiersPayant
    _selectedCustomer.value = customer.copy(tiersPayants = updatedTiersPayants)
}
```

---

## 🎯 Résumé des workflows conformes UX

### Vente Carnet
✅ **Affichage client :**
- Nom, Prénom, Matricule

✅ **Tiers Payants (provenant de `customer.tiersPayants`) :**
- Libellé
- Taux de couverture
- Champ numéro de bon

### Vente Assurance
✅ **Affichage client :**
- Nom, Prénom, Matricule

✅ **Ayant Droit :**
- Affichage si sélectionné
- Bouton : Sélectionner parmi existants
- Bouton : Créer nouveau

✅ **Tiers Payants (provenant de `customer.tiersPayants`) :**
- Libellé
- Rang (R0, C1, C2, C3)
- Taux de couverture
- Champ numéro de bon
- Bouton : Modifier taux
- Bouton : Retirer
- Bouton : Ajouter tiers payant

---

## ✅ Statut

**BUILD SUCCESSFUL** - Tous les composants compilent correctement.

**Prochaines étapes :**
1. Intégrer les layouts dans `activity_unified_sale.xml`
2. Implémenter les méthodes dans `UnifiedSaleActivity.kt`
3. Ajouter les méthodes au `UnifiedSaleViewModel.kt`
4. Créer les dialogs manquants (sélection ayant droit, création ayant droit, ajout tiers payant)
5. Tester le workflow complet pour Carnet et Assurance
