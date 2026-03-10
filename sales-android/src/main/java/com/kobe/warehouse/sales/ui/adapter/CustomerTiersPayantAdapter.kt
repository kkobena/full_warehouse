package com.kobe.warehouse.sales.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.sales.data.model.ClientTiersPayant
import com.kobe.warehouse.sales.databinding.ItemCustomerTiersPayantBinding

/**
 * Adapter pour afficher les tiers payants d'un client
 * Supporte deux modes : Carnet et Assurance
 */
class CustomerTiersPayantAdapter(
    private val isAssuranceMode: Boolean = false,
    private val onNumeroBonChanged: (ClientTiersPayant, String) -> Unit = { _, _ -> },
    private val onTauxModifyClicked: (ClientTiersPayant) -> Unit = {},
    private val onRemoveClicked: (ClientTiersPayant) -> Unit = {}
) : ListAdapter<ClientTiersPayant, CustomerTiersPayantAdapter.TiersPayantViewHolder>(TiersPayantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TiersPayantViewHolder {
        val binding = ItemCustomerTiersPayantBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TiersPayantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TiersPayantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TiersPayantViewHolder(
        private val binding: ItemCustomerTiersPayantBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentTiersPayant: ClientTiersPayant? = null
        private var isUpdatingText = false

        init {
            // Add TextWatcher once in init instead of in bind()
            binding.etNumeroBon.addTextChangedListener { text ->
                if (!isUpdatingText && currentTiersPayant != null) {
                    onNumeroBonChanged(currentTiersPayant!!, text.toString())
                }
            }

            // Auto-scroll when numBon gets focus so keyboard doesn't hide it
            binding.etNumeroBon.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    // Delay to let keyboard appear first
                    view.postDelayed({
                        val scrollView = findParentScrollView(view)
                        if (scrollView != null) {
                            val offset = IntArray(2)
                            view.getLocationInWindow(offset)
                            val scrollOffset = IntArray(2)
                            scrollView.getLocationInWindow(scrollOffset)
                            val scrollTo = offset[1] - scrollOffset[1] + scrollView.scrollY - scrollView.height / 3
                            scrollView.smoothScrollTo(0, scrollTo.coerceAtLeast(0))
                        }
                    }, 350)
                }
            }
        }

        fun bind(tiersPayant: ClientTiersPayant) {
            currentTiersPayant = tiersPayant

            binding.apply {
                // Nom du tiers payant
                tvTiersPayantName.text = tiersPayant.tiersPayantName ?: "Tiers payant #${tiersPayant.tiersPayantId}"

                // Taux de couverture
                tvTauxCouverture.text = "${tiersPayant.taux}%"

                // Numéro de bon - only update if text is different to preserve cursor position
                val newText = tiersPayant.numBon ?: ""
                if (etNumeroBon.text.toString() != newText) {
                    isUpdatingText = true
                    etNumeroBon.setText(newText)
                    isUpdatingText = false
                }

                // Mode Assurance : afficher rang, bouton modifier taux, bouton retirer
                if (isAssuranceMode) {
                    // Afficher le rang (priorité)
                    chipRang.visibility = View.VISIBLE
                    chipRang.text = tiersPayant.priorite?.getDisplayLabel() ?: "R0"

                    // Bouton modifier taux
                    btnModifyTaux.visibility = View.VISIBLE
                    btnModifyTaux.setOnClickListener {
                        onTauxModifyClicked(tiersPayant)
                    }

                    // Bouton retirer
                    btnRemoveTiersPayant.visibility = View.VISIBLE
                    btnRemoveTiersPayant.setOnClickListener {
                        onRemoveClicked(tiersPayant)
                    }
                } else {
                    // Mode Carnet : masquer rang et boutons
                    chipRang.visibility = View.GONE
                    btnModifyTaux.visibility = View.GONE
                    btnRemoveTiersPayant.visibility = View.GONE
                }
            }
        }
    }

    private fun findParentScrollView(view: View): NestedScrollView? {
        var parent: ViewParent? = view.parent
        while (parent != null) {
            if (parent is NestedScrollView) return parent
            parent = parent.parent
        }
        return null
    }

    private class TiersPayantDiffCallback : DiffUtil.ItemCallback<ClientTiersPayant>() {
        override fun areItemsTheSame(oldItem: ClientTiersPayant, newItem: ClientTiersPayant): Boolean {
            return oldItem.tiersPayantId == newItem.tiersPayantId
        }

        override fun areContentsTheSame(oldItem: ClientTiersPayant, newItem: ClientTiersPayant): Boolean {
            // Compare only non-EditText fields to avoid rebind during text input
            // numBon is excluded because it's being edited by the user
            return oldItem.tiersPayantId == newItem.tiersPayantId &&
                   oldItem.tiersPayantName == newItem.tiersPayantName &&
                   oldItem.taux == newItem.taux &&
                   oldItem.priorite == newItem.priorite &&
                   oldItem.num == newItem.num
            // Don't compare numBon to prevent rebind while user is typing
        }
    }
}
