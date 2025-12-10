package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.Alert
import com.kobe.warehouse.reports.databinding.ItemAlertDetailBinding

/**
 * Adapter for displaying detailed alerts in a RecyclerView.
 */
class AlertDetailAdapter(
    private val onItemClick: (Alert) -> Unit
) : ListAdapter<Alert, AlertDetailAdapter.ViewHolder>(AlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlertDetailBinding.inflate(
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
        private val binding: ItemAlertDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(alert: Alert) {
            val context = binding.root.context

            // Severity indicator color
            val severityColor = when (alert.severity) {
                Alert.SEVERITY_CRITICAL -> R.color.error
                Alert.SEVERITY_WARNING -> R.color.warning
                else -> R.color.info
            }
            binding.viewSeverityIndicator.setBackgroundColor(
                ContextCompat.getColor(context, severityColor)
            )

            // Alert type
            binding.tvAlertType.text = getAlertTypeLabel(alert.type)

            // Alert title
            binding.tvAlertTitle.text = alert.title

            // Alert description
            binding.tvAlertDescription.text = alert.message

            // Alert date
            binding.tvAlertDate.text = formatDate(alert.createdAt)
        }

        private fun getAlertTypeLabel(type: String): String {
            return when (type) {
                Alert.TYPE_STOCK_RUPTURE -> "Rupture de stock"
                Alert.TYPE_STOCK_LOW -> "Stock bas"
                Alert.TYPE_EXPIRY -> "Péremption"
                Alert.TYPE_CASH_DISCREPANCY -> "Écart caisse"
                Alert.TYPE_INVOICE_OVERDUE -> "Facture impayée"
                else -> type
            }
        }

        private fun formatDate(dateString: String): String {
            // Simple relative date formatting
            return "Détecté récemment"
        }
    }

    private class AlertDiffCallback : DiffUtil.ItemCallback<Alert>() {
        override fun areItemsTheSame(oldItem: Alert, newItem: Alert): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Alert, newItem: Alert): Boolean {
            return oldItem == newItem
        }
    }
}
