package com.synzura.ctools.tools

import android.content.Context
import android.content.Intent
import com.synzura.ctools.R
import com.synzura.ctools.tools.checkin.CheckInActivity

object ToolsRepository {
    
    fun getAllTools(context: Context): List<ToolItem> {
        return listOf(
            ToolItem(
                id = "check_in",
                name = context.getString(R.string.tool_check_in),
                iconRes = R.drawable.ic_check_in,
                description = context.getString(R.string.check_in_description),
                launchIntent = { 
                    Intent(it, CheckInActivity::class.java)
                }
            )
            // 后续添加更多工具
        )
    }
} 