package com.synzura.ctools

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.synzura.ctools.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupStatusBar()
        setupNavigation()
    }
    
    private fun setupStatusBar() {
        // 设置状态栏和导航栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 获取WindowInsetsController
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        
        // 设置状态栏图标颜色（深色背景使用浅色图标）
        windowInsetsController.isAppearanceLightStatusBars = false
        
        // 设置状态栏颜色
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)
    }
    
    private fun isDarkTheme(): Boolean {
        return resources.configuration.uiMode and 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK == 
                android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
    
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Connect the bottom navigation view with the navigation controller
        binding.bottomNavigation.setupWithNavController(navController)
    }
}