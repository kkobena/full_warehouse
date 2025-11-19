package com.kobe.warehouse.sales.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.model.PaymentMode

/**
 * Payment Mode Adapter
 * RecyclerView adapter for displaying payment methods
 */
class PaymentModeAdapter(
    private val onPaymentModeClick: (PaymentMode) -> Unit
) : ListAdapter<PaymentMode, PaymentModeAdapter.PaymentModeViewHolder>(PaymentModeDiffCallback()) {

    private var selectedCode: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentModeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_mode, parent, false)
        return PaymentModeViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentModeViewHolder, position: Int) {
        val paymentMode = getItem(position)
        holder.bind(paymentMode, paymentMode.code == selectedCode)
    }

    /**
     * Set selected payment mode
     */
    fun setSelected(code: String) {
        selectedCode = code
        notifyDataSetChanged()
    }

    inner class PaymentModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.paymentModeCard)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        private val tvLibelle: TextView = itemView.findViewById(R.id.tvLibelle)
        private val tvGroup: TextView = itemView.findViewById(R.id.tvGroup)

        fun bind(paymentMode: PaymentMode, isSelected: Boolean) {
            tvLibelle.text = paymentMode.libelle
            tvGroup.text = paymentMode.getGroupLabel()

            // Set icon based on payment group
            val iconRes = when (paymentMode.group) {
                "CASH" -> R.drawable.ic_money
                "CARD" -> R.drawable.ic_credit_card
                "MOBILE_MONEY" -> R.drawable.ic_phone
                else -> R.drawable.ic_payment
            }
            ivIcon.setImageResource(iconRes)

            // Highlight selected
            cardView.strokeColor = itemView.context.getColor(
                if (isSelected) R.color.primary else R.color.divider
            )
            cardView.strokeWidth = if (isSelected) 4 else 1

            cardView.setOnClickListener { onPaymentModeClick(paymentMode) }
        }
    }

    class PaymentModeDiffCallback : DiffUtil.ItemCallback<PaymentMode>() {
        override fun areItemsTheSame(oldItem: PaymentMode, newItem: PaymentMode): Boolean {
            return oldItem.code == newItem.code
        }

        override fun areContentsTheSame(oldItem: PaymentMode, newItem: PaymentMode): Boolean {
            return oldItem == newItem
        }
    }
}
