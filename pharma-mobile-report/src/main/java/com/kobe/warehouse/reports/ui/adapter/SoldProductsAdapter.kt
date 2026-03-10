package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.RecapProduitVendu
import com.kobe.warehouse.reports.databinding.ItemSoldProductBinding

class SoldProductsAdapter : ListAdapter<RecapProduitVendu, SoldProductsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSoldProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemSoldProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecapProduitVendu) {
            binding.tvProductName.text = item.libelle
            binding.tvProductCode.text = item.codeCip ?: item.codeEanLaboratoire ?: ""
            binding.tvRayon.text = item.rayonName ?: ""
            binding.tvQuantitySold.text = item.getNetQuantity().toString()
            binding.tvSalesAmount.text = item.getFormattedSalesAmount()
            binding.tvStock.text = item.stock.toString()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<RecapProduitVendu>() {
        override fun areItemsTheSame(a: RecapProduitVendu, b: RecapProduitVendu) = a.id == b.id
        override fun areContentsTheSame(a: RecapProduitVendu, b: RecapProduitVendu) = a == b
    }
}
