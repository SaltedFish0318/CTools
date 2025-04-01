package com.synzura.ctools.tools.checkin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.synzura.ctools.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * 打卡记录适配器 - 时间轴样式
 * 用于显示打卡记录列表
 */
class CheckInRecordAdapter(
    private val onDeleteClick: (CheckInRecord) -> Unit
) : ListAdapter<CheckInRecord, CheckInRecordAdapter.ViewHolder>(RecordDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_check_in_record_timeline, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = getItem(position)
        holder.bind(record, position == 0, position == itemCount - 1)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeText: TextView = itemView.findViewById(R.id.record_time)
        private val dateText: TextView = itemView.findViewById(R.id.record_date)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btn_delete_record)
        private val timelineLine: View = itemView.findViewById(R.id.timeline_line)

        fun bind(record: CheckInRecord, isFirst: Boolean, isLast: Boolean) {
            // 格式化时间显示
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            timeText.text = timeFormat.format(record.timestamp)
            dateText.text = record.date
            
            deleteButton.setOnClickListener {
                onDeleteClick(record)
            }
            
            // 处理时间轴线的显示
            if (isFirst && isLast) {
                // 如果只有一条记录，不显示线
                timelineLine.visibility = View.INVISIBLE
            } else if (isFirst) {
                // 第一条记录，只显示底部的线
                val layoutParams = timelineLine.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.topMargin = itemView.height / 2
                timelineLine.layoutParams = layoutParams
            } else if (isLast) {
                // 最后一条记录，只显示顶部的线
                val layoutParams = timelineLine.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.bottomMargin = itemView.height / 2
                timelineLine.layoutParams = layoutParams
            }
        }
    }

    class RecordDiffCallback : DiffUtil.ItemCallback<CheckInRecord>() {
        override fun areItemsTheSame(oldItem: CheckInRecord, newItem: CheckInRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CheckInRecord, newItem: CheckInRecord): Boolean {
            return oldItem == newItem
        }
    }
}