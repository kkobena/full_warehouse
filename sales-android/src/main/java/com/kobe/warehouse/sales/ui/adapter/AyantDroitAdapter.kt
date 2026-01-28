package com.kobe.warehouse.sales.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.databinding.ItemAyantDroitBinding

/**
 * Adapter for Ayants-Droit (beneficiaries) selection
 */
class AyantDroitAdapter(
    private val onAyantDroitClick: (Customer) -> Unit
) : ListAdapter<Customer, AyantDroitAdapter.AyantDroitViewHolder>(AyantDroitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AyantDroitViewHolder {
        val binding = ItemAyantDroitBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AyantDroitViewHolder(binding, onAyantDroitClick)
    }

    override fun onBindViewHolder(holder: AyantDroitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AyantDroitViewHolder(
        private val binding: ItemAyantDroitBinding,
        private val onAyantDroitClick: (Customer) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ayantDroit: Customer) {
            binding.apply {
                tvAyantDroitName.text = "${ayantDroit.firstName} ${ayantDroit.lastName}"

                // Display relation
                // TODO: Add type field to Customer model for proper relation display
                tvAyantDroitRelation.text = "Bénéficiaire"

                root.setOnClickListener {
                    onAyantDroitClick(ayantDroit)
                }
            }
        }
    }

    private class AyantDroitDiffCallback : DiffUtil.ItemCallback<Customer>() {
        override fun areItemsTheSame(oldItem: Customer, newItem: Customer): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Customer, newItem: Customer): Boolean {
            return oldItem == newItem
        }
    }
}
