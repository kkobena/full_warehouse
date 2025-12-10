package com.kobe.warehouse.reports.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kobe.warehouse.reports.R
import com.kobe.warehouse.reports.data.model.TodoItem
import com.kobe.warehouse.reports.databinding.ItemTodoBinding

/**
 * Adapter for displaying todo items in a RecyclerView.
 */
class TodoAdapter(
    private val onItemClick: (TodoItem) -> Unit,
    private val onItemChecked: (TodoItem, Boolean) -> Unit
) : ListAdapter<TodoItem, TodoAdapter.ViewHolder>(TodoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTodoBinding.inflate(
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
        private val binding: ItemTodoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.cbTodo.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemChecked(getItem(position), isChecked)
                }
            }
        }

        fun bind(todo: TodoItem) {
            val context = binding.root.context

            // Priority indicator color
            val priorityColor = when (todo.priority) {
                TodoItem.PRIORITY_URGENT -> R.color.error
                TodoItem.PRIORITY_IMPORTANT -> R.color.warning
                else -> R.color.info
            }
            binding.viewPriorityIndicator.setBackgroundColor(
                ContextCompat.getColor(context, priorityColor)
            )

            // Priority badge
            binding.tvPriority.text = getPriorityLabel(todo.priority)
            binding.tvPriority.setBackgroundColor(ContextCompat.getColor(context, priorityColor))

            // Todo content
            binding.tvTodoTitle.text = todo.title
            binding.tvTodoDescription.text = todo.description
            binding.tvActionType.text = todo.actionLabel

            // Due date
            binding.tvDueDate.text = todo.createdAt?.let { "Créé récemment" } ?: ""

            // Checkbox state
            binding.cbTodo.isChecked = todo.isDismissed
        }

        private fun getPriorityLabel(priority: String): String {
            return when (priority) {
                TodoItem.PRIORITY_URGENT -> "URGENT"
                TodoItem.PRIORITY_IMPORTANT -> "IMPORTANT"
                TodoItem.PRIORITY_NORMAL -> "NORMAL"
                else -> priority
            }
        }
    }

    private class TodoDiffCallback : DiffUtil.ItemCallback<TodoItem>() {
        override fun areItemsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
            return oldItem == newItem
        }
    }
}
