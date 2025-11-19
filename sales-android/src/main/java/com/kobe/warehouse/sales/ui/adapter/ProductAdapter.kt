package com.kobe.warehouse.sales.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.kobe.warehouse.sales.R
import com.kobe.warehouse.sales.data.model.Product

/**
 * Product Adapter
 * RecyclerView adapter for displaying product search results
 * Supports both list and grid layouts
 */
class ProductAdapter(
    private val onProductClick: (Product) -> Unit,
    private val isGridLayout: Boolean = false
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val layoutId = if (isGridLayout) {
            R.layout.item_product_grid
        } else {
            R.layout.item_product_list
        }

        val view = LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.productCard)
        private val tvLibelle: TextView = itemView.findViewById(R.id.tvLibelle)
        private val tvCode: TextView = itemView.findViewById(R.id.tvCode)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvStock: TextView = itemView.findViewById(R.id.tvStock)
        private val btnAdd: View = itemView.findViewById(R.id.btnAdd)

        fun bind(product: Product) {
            tvLibelle.text = product.libelle
            tvCode.text = product.code
            tvPrice.text = product.getFormattedPrice()

            // Stock display with color coding
            tvStock.text = "Stock: ${product.totalQuantity}"
            tvStock.setTextColor(
                itemView.context.getColor(
                    when {
                        product.totalQuantity == 0 -> R.color.error
                        product.totalQuantity < 10 -> R.color.warning
                        else -> R.color.success
                    }
                )
            )

            // Disable add button if out of stock
            btnAdd.isEnabled = product.isInStock()
            btnAdd.alpha = if (product.isInStock()) 1.0f else 0.5f

            // Click listeners
            cardView.setOnClickListener {
                if (product.isInStock()) {
                    onProductClick(product)
                }
            }
            btnAdd.setOnClickListener {
                if (product.isInStock()) {
                    onProductClick(product)
                }
            }
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}
