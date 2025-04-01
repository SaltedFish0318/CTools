package com.synzura.ctools.tools

import android.content.Intent
import android.content.Context
import androidx.annotation.DrawableRes

data class ToolItem(
    val id: String,
    val name: String,
    @DrawableRes val iconRes: Int,
    val description: String,
    val launchIntent: (Context) -> Intent
) 