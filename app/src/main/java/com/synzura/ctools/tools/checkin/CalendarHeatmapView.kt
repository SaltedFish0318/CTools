package com.synzura.ctools.tools.checkin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * 日历热力图自定义View
 * 以月份日历的形式展示打卡频率，颜色深浅表示打卡次数多少
 */
class CalendarHeatmapView @JvmOverloads constructor(
    context: Context, 
    attrs: AttributeSet? = null, 
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 颜色渐变，从浅到深
    private val colorLevels = listOf(
        Color.parseColor("#EEEEEE"),  // 0次
        Color.parseColor("#D4E6F1"),  // 1-2次
        Color.parseColor("#A9CCE3"),  // 3-4次
        Color.parseColor("#7FB3D5"),  // 5-6次
        Color.parseColor("#5499C7"),  // 7-8次
        Color.parseColor("#2980B9")   // 9+次
    )
    
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#666666")
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    
    private val cellRect = RectF()
    private val cellPadding = 4f
    private var cellSize = 0f
    private val weekDays = 7
    private val daysInView = 30 // 显示当月的天数
    
    // 当前月份的第一天
    private var firstDayOfMonth = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }
    
    // 存储日期及对应的打卡次数
    private val dateCountMap = mutableMapOf<String, Int>()
    
    // 日期格式化工具
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    /**
     * 设置要显示的月份和年份
     */
    fun setMonth(year: Int, month: Int) {
        firstDayOfMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        invalidate()
    }
    
    /**
     * 更新打卡数据
     * @param counts 日期与打卡次数的映射
     */
    fun updateData(counts: Map<String, Int>) {
        dateCountMap.clear()
        dateCountMap.putAll(counts)
        invalidate()
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        
        // 计算单元格大小
        cellSize = (width - paddingLeft - paddingRight) / 7f
        
        // 计算需要的行数
        val startDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val rowsNeeded = ((startDayOfWeek + daysInMonth - 1) / 7) + 1
        
        // 设置高度
        val height = paddingTop + paddingBottom + (rowsNeeded * cellSize).toInt() + 40 // 40 是用于顶部星期文字的高度
        
        setMeasuredDimension(width, height)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width - paddingLeft - paddingRight
        
        // 计算第一天是星期几 (0代表星期天)
        val startDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // 绘制星期标题
        val weekLabels = arrayOf("日", "一", "二", "三", "四", "五", "六")
        for (i in 0 until weekDays) {
            canvas.drawText(
                weekLabels[i],
                paddingLeft + i * cellSize + cellSize / 2,
                paddingTop + 30f,
                textPaint
            )
        }
        
        // 绘制日期单元格
        for (day in 1..daysInMonth) {
            val dayOfWeek = (startDayOfWeek + day - 1) % 7
            val row = (startDayOfWeek + day - 1) / 7
            
            // 计算单元格位置
            val left = paddingLeft + dayOfWeek * cellSize + cellPadding
            val top = paddingTop + row * cellSize + 40 + cellPadding // 40 是用于顶部星期文字的高度
            val right = left + cellSize - 2 * cellPadding
            val bottom = top + cellSize - 2 * cellPadding
            
            cellRect.set(left, top, right, bottom)
            
            // 获取当天的日期字符串
            val calendar = (firstDayOfMonth.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, day - 1)
            }
            val dateString = dateFormat.format(calendar.time)
            
            // 根据打卡次数选择颜色
            val count = dateCountMap[dateString] ?: 0
            val colorIndex = when {
                count == 0 -> 0
                count in 1..2 -> 1
                count in 3..4 -> 2
                count in 5..6 -> 3
                count in 7..8 -> 4
                else -> 5
            }
            
            // 绘制单元格背景
            paint.color = colorLevels[colorIndex]
            canvas.drawRoundRect(cellRect, 8f, 8f, paint)
            
            // 绘制日期文字
            textPaint.textSize = 20f
            canvas.drawText(
                day.toString(),
                cellRect.centerX(),
                cellRect.centerY() + textPaint.textSize / 3,
                textPaint
            )
        }
    }
} 