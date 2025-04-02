package com.synzura.ctools.utils

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.synzura.ctools.R

/**
 * 状态栏工具类，用于设置沉浸式状态栏
 */
object StatusBarUtils {
    
    /**
     * 设置沉浸式状态栏
     * @param activity Activity实例
     * @param darkIcons 是否使用深色图标（浅色背景时使用深色图标）
     */
    fun setImmersiveStatusBar(activity: Activity, darkIcons: Boolean = false) {
        // 设置内容延伸到状态栏和导航栏
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        
        // 获取WindowInsetsController
        val windowInsetsController = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        
        // 设置状态栏图标颜色
        windowInsetsController.isAppearanceLightStatusBars = darkIcons
        
        // 设置状态栏颜色
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        activity.window.statusBarColor = ContextCompat.getColor(activity, R.color.colorPrimary)
    }
    
    /**
     * 获取状态栏高度
     * @param context Context实例
     * @return 状态栏高度（像素）
     */
    fun getStatusBarHeight(context: Context): Int {
        val resources = context.resources
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }
    
    /**
     * 为视图添加状态栏高度的顶部内边距
     * @param view 需要添加内边距的视图
     * @param context Context实例
     */
    fun addStatusBarPadding(view: View, context: Context) {
        val statusBarHeight = getStatusBarHeight(context)
        
        // 直接设置足够的顶部内边距，确保内容不被状态栏遮挡
        view.setPadding(
            view.paddingLeft,
            statusBarHeight,
            view.paddingRight,
            view.paddingBottom
        )
        
        // 如果是MaterialToolbar，进行特殊处理
        if (view is com.google.android.material.appbar.MaterialToolbar) {
            // 确保标题居中显示
            view.isTitleCentered = true
            
            // 获取并调整Toolbar的布局参数
            val params = view.layoutParams
            if (params != null) {
                // 获取actionBarSize
                val typedValue = android.util.TypedValue()
                if (context.theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
                    val actionBarHeight = android.util.TypedValue.complexToDimensionPixelSize(
                        typedValue.data, context.resources.displayMetrics
                    )
                    // 设置Toolbar的高度为actionBarSize + statusBarHeight
                    params.height = actionBarHeight + statusBarHeight
                    view.layoutParams = params
                }
            }
        }
    }
}