package com.synzura.ctools.tools.checkin

/**
 * 打卡项目数据类
 */
data class CheckInItem(
    val id: String,
    val name: String,
    val description: String,
    val iconResId: Int,
    val count: Int = 0,
    val goal: Int = 0,
    val unit: String = "",
    val order: Int = 0
) 