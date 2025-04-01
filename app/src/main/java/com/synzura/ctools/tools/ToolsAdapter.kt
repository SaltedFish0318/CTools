package com.synzura.ctools.tools

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.synzura.ctools.databinding.ItemToolBinding

class ToolsAdapter : ListAdapter<ToolItem, ToolsAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemToolBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemToolBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: ToolItem) {
            binding.toolName.text = item.name
            binding.toolDescription.text = item.description
            binding.toolIcon.setImageResource(item.iconRes)
            
            binding.root.setOnClickListener {
                val context = it.context
                context.startActivity(item.launchIntent(context))
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ToolItem>() {
            override fun areItemsTheSame(oldItem: ToolItem, newItem: ToolItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ToolItem, newItem: ToolItem): Boolean {
                return oldItem == newItem
            }
        }
    }
} 