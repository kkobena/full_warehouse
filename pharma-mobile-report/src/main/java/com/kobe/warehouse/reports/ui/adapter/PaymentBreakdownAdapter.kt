package com.kobe.warehouse.reports.ui.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.PaymentModeBreakdown
import com.kobe.warehouse.reports.databinding.ItemPaymentBreakdownBinding

/**
 * Adapter for payment breakdown items in Cash Balance.
 */
class PaymentBreakdownAdapter : ListAdapter<PaymentModeBreakdown, PaymentBreakdownAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPaymentBreakdownBinding.inflate(
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
        private val binding: ItemPaymentBreakdownBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PaymentModeBreakdown) {
            binding.tvLabel.text = item.libelle
            binding.tvPercent.text = item.getFormattedPercent()
            binding.tvAmount.text = item.getFormattedMontant()

            // Set color indicator
            try {
                val color = Color.parseColor(item.color)
                val drawable = binding.colorIndicator.background as? GradientDrawable
                drawable?.setColor(color)
            } catch (e: Exception) {
                // Default color if parsing fails
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PaymentModeBreakdown>() {
        override fun areItemsTheSame(oldItem: PaymentModeBreakdown, newItem: PaymentModeBreakdown): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: PaymentModeBreakdown, newItem: PaymentModeBreakdown): Boolean {
            return oldItem == newItem
        }
    }
}
