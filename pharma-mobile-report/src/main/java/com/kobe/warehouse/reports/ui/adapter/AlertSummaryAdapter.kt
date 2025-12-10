package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.AlertSummary
import com.kobe.warehouse.reports.databinding.ItemAlertSummaryBinding

/**
 * Adapter for displaying alert summaries in a RecyclerView.
 */
class AlertSummaryAdapter(
    private val onItemClick: (AlertSummary) -> Unit
) : ListAdapter<AlertSummary, AlertSummaryAdapter.ViewHolder>(AlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlertSummaryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemAlertSummaryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(alert: AlertSummary) {
            binding.tvSeverityEmoji.text = alert.getSeverityEmoji()
            binding.tvAlertMessage.text = alert.message
            binding.tvAlertCount.text = alert.count.toString()
        }
    }

    private class AlertDiffCallback : DiffUtil.ItemCallback<AlertSummary>() {
        override fun areItemsTheSame(oldItem: AlertSummary, newItem: AlertSummary): Boolean {
            return oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: AlertSummary, newItem: AlertSummary): Boolean {
            return oldItem == newItem
        }
    }
}
