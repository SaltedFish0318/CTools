package com.synzura.ctools.tools.checkin

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 打卡记录数据类
 */
data class CheckInRecord(
    val id: String,
    val timestamp: Date = Date(),
    val itemId: String = "",
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(timestamp)
) 