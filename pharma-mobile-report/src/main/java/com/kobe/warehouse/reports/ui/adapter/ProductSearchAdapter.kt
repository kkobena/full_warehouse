package com.kobe.warehouse.reports.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.data.model.ProductSearchResult
import com.kobe.warehouse.reports.databinding.ItemProductSearchBinding
import java.util.Locale

/**
 * Adapter for displaying product search results.
 */
class ProductSearchAdapter(
    private val onProductClick: (ProductSearchResult) -> Unit
) : ListAdapter<ProductSearchResult, ProductSearchAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding, onProductClick)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for product search item.
     */
    class ProductViewHolder(
        private val binding: ItemProductSearchBinding,
        private val onProductClick: (ProductSearchResult) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductSearchResult) {
            binding.apply {
                // Product name
                tvProductName.text = product.getName()

                // Stock indicator
                tvStockIndicator.text = product.getStockIndicator()

                // Code CIP
                val codeCip = product.getCodeCip()
                tvCodeCip.text = if (!codeCip.isNullOrBlank()) "CIP: $codeCip" else ""
                tvCodeCip.isVisible = !codeCip.isNullOrBlank()

                // Rayons
                val rayonsText = product.getRayonsText()
                tvRayons.text = rayonsText
                tvRayons.isVisible = rayonsText.isNotBlank()

                // Stock quantity
                tvStock.text = "${product.totalQuantity} unites"

                // Prices
                tvPurchasePrice.text = product.getFormattedPurchasePrice()
                tvSellingPrice.text = product.getFormattedPrice()

                // Margin
                val marginPercent = product.getMarginPercent()
                tvMargin.text = String.format(Locale.FRANCE, "Marge: %.1f%%", marginPercent)
                tvMargin.isVisible = marginPercent > 0

                // Click listener
                root.setOnClickListener {
                    onProductClick(product)
                }
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     */
    class ProductDiffCallback : DiffUtil.ItemCallback<ProductSearchResult>() {
        override fun areItemsTheSame(
            oldItem: ProductSearchResult,
            newItem: ProductSearchResult
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: ProductSearchResult,
            newItem: ProductSearchResult
        ): Boolean {
            return oldItem == newItem
        }
    }
}
