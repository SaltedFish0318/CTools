package com.synzura.ctools.tools.checkin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.synzura.ctools.R
import java.util.Collections

class CheckInItemAdapter(
    private val onItemClick: (CheckInItem) -> Unit,
    private val onAddClick: (CheckInItem) -> Unit
) : RecyclerView.Adapter<CheckInItemAdapter.ViewHolder>() {

    private val items = mutableListOf<CheckInItem>()

    fun updateItems(newItems: List<CheckInItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getCurrentItems(): List<CheckInItem> {
        return items.toList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_check_in, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.icon_image)
        private val nameTextView: TextView = itemView.findViewById(R.id.name_text)
        private val countTextView: TextView = itemView.findViewById(R.id.count_text)
        private val addButton: ImageButton = itemView.findViewById(R.id.btn_add)

        fun bind(item: CheckInItem) {
            iconImageView.setImageResource(item.iconResId)
            nameTextView.text = item.name
            
            // 显示打卡次数，如果有目标则显示进度
            if (item.goal > 0) {
                countTextView.text = itemView.context.getString(
                    R.string.count_with_goal,
                    item.count,
                    item.goal,
                    item.unit
                )
            } else {
                countTextView.text = itemView.context.getString(
                    R.string.count_without_goal,
                    item.count,
                    item.unit
                )
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }

            addButton.setOnClickListener {
                onAddClick(item)
            }
        }
    }
} 