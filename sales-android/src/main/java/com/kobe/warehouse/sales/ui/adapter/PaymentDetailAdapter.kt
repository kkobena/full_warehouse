package com.kobe.warehouse.sales.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.sales.data.model.Payment
import com.kobe.warehouse.sales.databinding.ItemPaymentDetailBinding
import java.text.NumberFormat
import java.util.Locale

class PaymentDetailAdapter : ListAdapter<Payment, PaymentDetailAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Payment>() {
            override fun areItemsTheSame(oldItem: Payment, newItem: Payment): Boolean {
                return oldItem.id == newItem.id && oldItem.paymentModeCode == newItem.paymentModeCode
            }

            override fun areContentsTheSame(oldItem: Payment, newItem: Payment): Boolean {
                return oldItem.paidAmount == newItem.paidAmount
                        && oldItem.paymentMode?.libelle == newItem.paymentMode?.libelle
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPaymentDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemPaymentDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val numberFormat = NumberFormat.getNumberInstance(Locale.FRANCE)

        fun bind(payment: Payment) {
            binding.tvPaymentMode.text = payment.paymentMode?.libelle ?: "Espèces"
            binding.tvPaymentAmount.text = formatAmount(payment.paidAmount)
        }

        private fun formatAmount(amount: Int): String {
            return "${numberFormat.format(amount)} FCFA"
        }
    }
}
