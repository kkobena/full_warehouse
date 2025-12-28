package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.CashMovement
import com.kobe.warehouse.reports.databinding.ItemMouvementBinding

/**
 * Adapter for displaying cash movements in the Cash Balance report.
 */
class CashMovementAdapter : ListAdapter<CashMovement, CashMovementAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMouvementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemMouvementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movement: CashMovement) {
            binding.tvLibelle.text = movement.libelle

            val context = binding.root.context
            if (movement.isEntree()) {
                binding.iconType.setImageResource(R.drawable.ic_arrow_upward)
                binding.iconType.setColorFilter(ContextCompat.getColor(context, R.color.success))
                binding.tvMontant.text = "+ ${movement.getFormattedMontant()}"
                binding.tvMontant.setTextColor(ContextCompat.getColor(context, R.color.success))
            } else {
                binding.iconType.setImageResource(R.drawable.ic_arrow_downward)
                binding.iconType.setColorFilter(ContextCompat.getColor(context, R.color.error))
                binding.tvMontant.text = "- ${movement.getFormattedMontant()}"
                binding.tvMontant.setTextColor(ContextCompat.getColor(context, R.color.error))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CashMovement>() {
        override fun areItemsTheSame(oldItem: CashMovement, newItem: CashMovement): Boolean {
            return oldItem.id == newItem.id && oldItem.libelle == newItem.libelle
        }

        override fun areContentsTheSame(oldItem: CashMovement, newItem: CashMovement): Boolean {
            return oldItem == newItem
        }
    }
}
