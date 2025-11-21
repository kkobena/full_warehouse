package com.kobe.warehouse.sales.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.sales.data.model.Payment
import com.kobe.warehouse.sales.databinding.ItemPaymentDetailBinding
import java.text.NumberFormat
import java.util.Locale

class PaymentDetailAdapter : RecyclerView.Adapter<PaymentDetailAdapter.ViewHolder>() {

    private val items = mutableListOf<Payment>()

    fun submitList(newItems: List<Payment>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
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
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

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
