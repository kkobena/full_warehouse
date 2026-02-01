package com.kobe.warehouse.sales.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.sales.databinding.ItemTiersPayantBinding
import com.kobe.warehouse.sales.data.model.TiersPayant

/**
 * Adapter for Tiers Payant selection (AutoCompleteTextView dropdown)
 */
class TiersPayantAdapter(
    private val onTiersPayantClick: (TiersPayant) -> Unit
) : ListAdapter<TiersPayant, TiersPayantAdapter.TiersPayantViewHolder>(TiersPayantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TiersPayantViewHolder {
        val binding = ItemTiersPayantBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TiersPayantViewHolder(binding, onTiersPayantClick)
    }

    override fun onBindViewHolder(holder: TiersPayantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TiersPayantViewHolder(
        private val binding: ItemTiersPayantBinding,
        private val onTiersPayantClick: (TiersPayant) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tiersPayant: TiersPayant) {
            binding.apply {
                tvTiersPayantName.text = tiersPayant.getDisplayName()
                tvTiersPayantCode.text = tiersPayant.codeOrganisme ?: ""
                tvTauxCouverture.text = tiersPayant.getCategoryLabel()

                root.setOnClickListener {
                    onTiersPayantClick(tiersPayant)
                }
            }
        }
    }

    private class TiersPayantDiffCallback : DiffUtil.ItemCallback<TiersPayant>() {
        override fun areItemsTheSame(oldItem: TiersPayant, newItem: TiersPayant): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TiersPayant, newItem: TiersPayant): Boolean {
            return oldItem == newItem
        }
    }
}
