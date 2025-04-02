package com.synzura.ctools.tools.checkin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.snackbar.Snackbar
import com.synzura.ctools.R
import com.synzura.ctools.databinding.ActivityCheckInBinding
import com.synzura.ctools.utils.StatusBarUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class CheckInActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckInBinding
    private lateinit var checkInAdapter: CheckInItemAdapter
    
    // 最后删除的项目，用于实现撤销功能
    private var lastDeletedItem: CheckInItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 使用工具类设置沉浸式状态栏
        StatusBarUtils.setImmersiveStatusBar(this, false)
        
        setupToolbar()
        setupRecyclerView()
        setupObservers()
        
        // 初始化打卡项目数据
        CheckInItemsRepository.initialize(this)
        
        // 设置添加按钮的点击事件
        binding.fabAddItem.setOnClickListener {
            showAddItemDialog()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.tool_check_in)
        
        // 设置标题居中
        binding.toolbar.isTitleCentered = true
        
        // 为Toolbar添加状态栏高度的内边距
        StatusBarUtils.addStatusBarPadding(binding.toolbar, this)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        checkInAdapter = CheckInItemAdapter(
            onItemClick = { item ->
                // 打开详情页
                val intent = Intent(this, CheckInDetailActivity::class.java).apply {
                    putExtra(CheckInDetailActivity.EXTRA_CHECK_IN_ID, item.id)
                }
                startActivity(intent)
            },
            onAddClick = { item ->
                // 增加打卡次数
                CheckInItemsRepository.incrementCount(item.id)
            }
        )
        
        binding.checkInItemsList.apply {
            layoutManager = LinearLayoutManager(this@CheckInActivity)
            adapter = checkInAdapter
            
            // 添加拖拽排序功能
            val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                0
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val fromPosition = viewHolder.adapterPosition
                    val toPosition = target.adapterPosition
                    
                    // 通知适配器项目已移动
                    checkInAdapter.moveItem(fromPosition, toPosition)
                    return true
                }
                
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    // 不处理滑动操作
                }
                
                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                        viewHolder?.itemView?.alpha = 0.7f
                    }
                }
                
                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)
                    viewHolder.itemView.alpha = 1.0f
                    
                    // 获取当前项目顺序
                    val currentItems = checkInAdapter.getCurrentItems()
                    
                    // 保存排序结果
                    CheckInItemsRepository.updateItemsOrder(currentItems)
                }
            })
            
            itemTouchHelper.attachToRecyclerView(this)
        }
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            CheckInItemsRepository.checkInItems.collectLatest { items ->
                checkInAdapter.updateItems(items)
            }
        }
    }

    /**
     * 显示添加打卡项目的对话框
     */
    private fun showAddItemDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_check_in_item, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.et_item_name)
        val descEditText = dialogView.findViewById<EditText>(R.id.et_item_description)
        val goalEditText = dialogView.findViewById<EditText>(R.id.et_item_goal)
        val unitEditText = dialogView.findViewById<EditText>(R.id.et_item_unit)
        val iconSelector = dialogView.findViewById<RecyclerView>(R.id.rv_icon_selector)
        
        // 设置图标选择器
        val icons = listOf(
            R.drawable.ic_water,
            R.drawable.ic_exercise,
            R.drawable.ic_restroom,
            R.drawable.ic_medicine,
            R.drawable.ic_sleep,
            R.drawable.ic_fruit,
            R.drawable.ic_reading,
            R.drawable.ic_meditation,
            R.drawable.ic_walking,
            R.drawable.ic_study
        )
        
        var selectedIconResId = icons[0]
        
        val iconAdapter = IconSelectorAdapter(icons) { iconResId ->
            selectedIconResId = iconResId
        }
        
        iconSelector.layoutManager = GridLayoutManager(this, 4)
        iconSelector.adapter = iconAdapter
        
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.add_check_in_item)
            .setView(dialogView)
            .setPositiveButton(R.string.add, null) // 设为null以自定义点击行为
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = nameEditText.text.toString().trim()
                val description = descEditText.text.toString().trim()
                val goalStr = goalEditText.text.toString().trim()
                val unit = unitEditText.text.toString().trim()
                
                if (name.isEmpty()) {
                    nameEditText.error = getString(R.string.field_required)
                    return@setOnClickListener
                }
                
                // 目标和单位可选，如果没有设置目标，则设为0
                val goal = if (goalStr.isEmpty()) 0 else goalStr.toIntOrNull() ?: 0
                val finalUnit = if (goal > 0 && unit.isEmpty()) {
                    unitEditText.error = getString(R.string.field_required_with_goal)
                    return@setOnClickListener
                } else if (goal > 0) unit else ""
                
                // 添加自定义打卡项目
                CheckInItemsRepository.addCustomItem(
                    name = name,
                    description = description,
                    iconResId = selectedIconResId,
                    goal = goal,
                    unit = finalUnit
                )
                
                dialog.dismiss()
            }
        }
        
        dialog.show()
    }
}