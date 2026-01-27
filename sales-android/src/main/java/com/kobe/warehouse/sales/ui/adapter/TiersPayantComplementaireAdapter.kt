package com.kobe.warehouse.sales.ui.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.sales.databinding.ItemTiersPayantComplementaireBinding
import com.kobe.warehouse.sales.domain.model.TiersPayant

/**
 * Adapter for Tiers Payant Complementaire management
 * Displays complementary insurance providers with editable coverage rates
 */
class TiersPayantComplementaireAdapter(
    private val onTauxChanged: (TiersPayant, Int) -> Unit,
    private val onRemove: (TiersPayant) -> Unit
) : ListAdapter<TiersPayant, TiersPayantComplementaireAdapter.ComplementaireViewHolder>(ComplementaireDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComplementaireViewHolder {
        val binding = ItemTiersPayantComplementaireBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ComplementaireViewHolder(binding, onTauxChanged, onRemove)
    }

    override fun onBindViewHolder(holder: ComplementaireViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ComplementaireViewHolder(
        private val binding: ItemTiersPayantComplementaireBinding,
        private val onTauxChanged: (TiersPayant, Int) -> Unit,
        private val onRemove: (TiersPayant) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentTiersPayant: TiersPayant? = null
        private var textWatcher: TextWatcher? = null

        fun bind(tiersPayant: TiersPayant) {
            currentTiersPayant = tiersPayant

            binding.apply {
                tvTiersPayantName.text = tiersPayant.name

                // Remove previous text watcher
                textWatcher?.let { etTauxCouverture.removeTextChangedListener(it) }

                // Set current taux
                etTauxCouverture.setText(tiersPayant.tauxCouverture.toString())

                // Add new text watcher
                textWatcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val newTaux = s?.toString()?.toIntOrNull() ?: 0
                        if (newTaux in 0..100) {
                            onTauxChanged(tiersPayant, newTaux)
                        }
                    }
                }
                etTauxCouverture.addTextChangedListener(textWatcher)

                // Remove button
                btnRemoveTiersPayant.setOnClickListener {
                    onRemove(tiersPayant)
                }
            }
        }
    }

    private class ComplementaireDiffCallback : DiffUtil.ItemCallback<TiersPayant>() {
        override fun areItemsTheSame(oldItem: TiersPayant, newItem: TiersPayant): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TiersPayant, newItem: TiersPayant): Boolean {
            return oldItem == newItem
        }
    }
}
