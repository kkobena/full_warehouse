package com.kobe.warehouse.sales.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.sales.data.model.Customer
import com.kobe.warehouse.sales.databinding.ItemCustomerSearchBinding

/**
 * Adapter for customer search results
 */
class CustomerSearchAdapter(
    private val onCustomerClick: (Customer) -> Unit
) : ListAdapter<Customer, CustomerSearchAdapter.CustomerViewHolder>(CustomerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CustomerViewHolder(binding, onCustomerClick)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CustomerViewHolder(
        private val binding: ItemCustomerSearchBinding,
        private val onCustomerClick: (Customer) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(customer: Customer) {
            binding.apply {
                tvCustomerName.text = "${customer.firstName} ${customer.lastName}"
                tvCustomerPhone.text = customer.phone ?: ""

                // Display customer type
                // TODO: Add type field to Customer model for proper type display
                tvCustomerType.text = "Client"

                root.setOnClickListener {
                    onCustomerClick(customer)
                }
            }
        }
    }

    private class CustomerDiffCallback : DiffUtil.ItemCallback<Customer>() {
        override fun areItemsTheSame(oldItem: Customer, newItem: Customer): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Customer, newItem: Customer): Boolean {
            return oldItem == newItem
        }
    }
}
