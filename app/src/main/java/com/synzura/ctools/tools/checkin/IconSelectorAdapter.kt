package com.synzura.ctools.tools.checkin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.synzura.ctools.R

/**
 * 图标选择器适配器
 * 用于在添加打卡项目时选择图标
 */
class IconSelectorAdapter(
    private val icons: List<Int>,
    private val onIconSelected: (Int) -> Unit
) : RecyclerView.Adapter<IconSelectorAdapter.ViewHolder>() {

    private var selectedPosition = 0

    /**
     * 设置默认选中的图标
     */
    fun setSelectedIcon(iconResId: Int) {
        val position = icons.indexOf(iconResId)
        if (position != -1) {
            val oldPosition = selectedPosition
            selectedPosition = position
            if (oldPosition != position) {
                notifyItemChanged(oldPosition)
                notifyItemChanged(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_icon_selector, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val iconResId = icons[position]
        holder.bind(iconResId, position == selectedPosition)
    }

    override fun getItemCount(): Int = icons.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.icon_image)
        private val backgroundView: View = itemView.findViewById(R.id.icon_background)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val oldPosition = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(position)
                    onIconSelected(icons[position])
                }
            }
        }

        fun bind(iconResId: Int, isSelected: Boolean) {
            iconImageView.setImageResource(iconResId)
            
            if (isSelected) {
                backgroundView.setBackgroundResource(R.drawable.circle_selected_background)
                iconImageView.setColorFilter(
                    ContextCompat.getColor(itemView.context, android.R.color.white)
                )
            } else {
                backgroundView.setBackgroundResource(R.drawable.circle_background)
                iconImageView.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.colorPrimary)
                )
            }
        }
    }
} 