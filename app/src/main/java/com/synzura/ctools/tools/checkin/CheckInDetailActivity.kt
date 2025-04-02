package com.synzura.ctools.tools.checkin

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.synzura.ctools.R
import com.synzura.ctools.databinding.ActivityCheckInDetailBinding
import com.synzura.ctools.utils.StatusBarUtils
import java.text.SimpleDateFormat
import java.util.*
import android.widget.EditText

class CheckInDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckInDetailBinding
    private var checkInItem: CheckInItem? = null
    private lateinit var recordAdapter: CheckInRecordAdapter
    
    // 当前选择的年份和月份
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    
    companion object {
        const val EXTRA_CHECK_IN_ID = "extra_check_in_id"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckInDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 启用沉浸式状态栏
        StatusBarUtils.setImmersiveStatusBar(this, false)
        
        // 获取打卡项目ID
        val itemId = intent.getStringExtra(EXTRA_CHECK_IN_ID)
        
        if (itemId != null) {
            // 从存储库获取打卡项目
            checkInItem = CheckInItemsRepository.getItemById(itemId)
            
            setupToolbar()
            setupUI()
            setupRecordsList(itemId)
            setupHeatmap(itemId)
            setupButtons()
        } else {
            // 如果没有ID，返回上一页
            finish()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = checkInItem?.name ?: getString(R.string.check_in_detail)
        
        // 设置标题居中
        (binding.toolbar as? com.google.android.material.appbar.MaterialToolbar)?.isTitleCentered = true
        
        // 为Toolbar添加状态栏高度的内边距
        StatusBarUtils.addStatusBarPadding(binding.toolbar, this)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // 添加编辑按钮
        binding.toolbar.inflateMenu(R.menu.menu_check_in_detail)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    showEditItemDialog()
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmationDialog()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupUI() {
        checkInItem?.let { item ->
            binding.apply {
                itemIcon.setImageResource(item.iconResId)
                itemName.text = item.name
                itemDescription.text = item.description
                
                if (item.goal > 0) {
                    // 显示进度
                    progressText.text = getString(
                        R.string.check_in_progress,
                        item.count,
                        item.goal,
                        item.unit
                    )
                    
                    // 设置进度条
                    val progress = (item.count.toFloat() / item.goal.toFloat()) * 100
                    progressBar.progress = progress.toInt()
                    progressBar.visibility = View.VISIBLE
                } else {
                    // 显示距离上次打卡的时间
                    val lastRecord = CheckInItemsRepository.getLastRecordForItem(item.id)
                    if (lastRecord != null) {
                        val elapsedTime = getElapsedTimeString(lastRecord.timestamp)
                        progressText.text = getString(R.string.last_check_in_time, elapsedTime)
                    } else {
                        progressText.text = getString(R.string.no_check_in_yet)
                    }
                    progressBar.visibility = View.GONE
                }
            }
        }
    }
    
    /**
     * 获取已过去时间的字符串描述
     */
    private fun getElapsedTimeString(timestamp: Date): String {
        val now = System.currentTimeMillis()
        val elapsedMs = now - timestamp.time
        
        // 转换为适当的时间单位
        val minutes = elapsedMs / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> resources.getQuantityString(R.plurals.days_ago, days.toInt(), days.toInt())
            hours > 0 -> resources.getQuantityString(R.plurals.hours_ago, hours.toInt(), hours.toInt())
            minutes > 0 -> resources.getQuantityString(R.plurals.minutes_ago, minutes.toInt(), minutes.toInt())
            else -> getString(R.string.just_now)
        }
    }
    
    private fun setupRecordsList(itemId: String) {
        // 初始化适配器
        recordAdapter = CheckInRecordAdapter { record ->
            // 删除记录
            CheckInItemsRepository.deleteRecord(itemId, record.id)
            
            // 更新UI
            updateRecordsList(itemId)
            updateUI()
            updateHeatmap(itemId)
        }
        
        // 设置RecyclerView
        binding.recordsList.apply {
            layoutManager = LinearLayoutManager(this@CheckInDetailActivity)
            adapter = recordAdapter
        }
        
        // 加载记录数据
        updateRecordsList(itemId)
    }
    
    private fun updateRecordsList(itemId: String) {
        // 获取所有记录，按时间倒序排列
        val records = CheckInItemsRepository.getRecordsForItem(itemId)
        recordAdapter.submitList(records)
        
        // 显示或隐藏空记录提示
        if (records.isEmpty()) {
            binding.emptyRecordsText.visibility = View.VISIBLE
            binding.recordsList.visibility = View.GONE
        } else {
            binding.emptyRecordsText.visibility = View.GONE
            binding.recordsList.visibility = View.VISIBLE
        }
    }
    
    /**
     * 显示编辑项目对话框
     */
    private fun showEditItemDialog() {
        checkInItem?.let { item ->
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_check_in_item, null)
            val nameEditText = dialogView.findViewById<EditText>(R.id.et_item_name)
            val descEditText = dialogView.findViewById<EditText>(R.id.et_item_description)
            val goalEditText = dialogView.findViewById<EditText>(R.id.et_item_goal)
            val unitEditText = dialogView.findViewById<EditText>(R.id.et_item_unit)
            val iconSelector = dialogView.findViewById<RecyclerView>(R.id.rv_icon_selector)
            
            // 设置当前值
            nameEditText.setText(item.name)
            descEditText.setText(item.description)
            if (item.goal > 0) {
                goalEditText.setText(item.goal.toString())
                unitEditText.setText(item.unit)
            }
            
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
            
            var selectedIconResId = item.iconResId
            
            val iconAdapter = IconSelectorAdapter(icons) { iconResId ->
                selectedIconResId = iconResId
            }
            
            iconSelector.layoutManager = GridLayoutManager(this, 4)
            iconSelector.adapter = iconAdapter
            
            // 默认选中当前图标
            iconAdapter.setSelectedIcon(selectedIconResId)
            
            val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.edit_check_in_item)
                .setView(dialogView)
                .setPositiveButton(R.string.save, null) // 设为null以自定义点击行为
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
                    
                    // 更新打卡项目
                    val updatedItem = item.copy(
                        name = name,
                        description = description,
                        iconResId = selectedIconResId,
                        goal = goal,
                        unit = finalUnit
                    )
                    
                    CheckInItemsRepository.updateItem(updatedItem)
                    
                    // 刷新界面
                    checkInItem = updatedItem
                    setupToolbar()
                    setupUI()
                    
                    dialog.dismiss()
                    
                    // 显示成功提示
                    Snackbar.make(
                        binding.root,
                        getString(R.string.item_updated),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            
            dialog.show()
        }
    }
    
    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmationDialog() {
        checkInItem?.let { item ->
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_item)
                .setMessage(getString(R.string.delete_item_confirm, item.name))
                .setPositiveButton(R.string.delete) { _, _ ->
                    // 删除项目
                    CheckInItemsRepository.deleteItem(item.id)
                    
                    // 显示成功提示
                    Snackbar.make(
                        binding.root,
                        getString(R.string.item_deleted, item.name),
                        Snackbar.LENGTH_LONG
                    ).show()
                    
                    // 返回上一页
                    finish()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }
    
    private fun setupHeatmap(itemId: String) {
        // 初始化热力图月份
        updateMonthYearText()
        
        // 设置月份切换按钮
        binding.btnPrevMonth.setOnClickListener {
            currentMonth--
            if (currentMonth < 0) {
                currentMonth = 11
                currentYear--
            }
            updateMonthYearText()
            updateHeatmap(itemId)
        }
        
        binding.btnNextMonth.setOnClickListener {
            currentMonth++
            if (currentMonth > 11) {
                currentMonth = 0
                currentYear++
            }
            updateMonthYearText()
            updateHeatmap(itemId)
        }
        
        // 更新热力图数据
        updateHeatmap(itemId)
    }
    
    private fun updateMonthYearText() {
        binding.monthYearText.text = getString(
            R.string.month_year_format,
            currentYear,
            currentMonth + 1
        )
    }
    
    private fun updateHeatmap(itemId: String) {
        // 设置当前热力图月份
        binding.heatmapView.setMonth(currentYear, currentMonth)
        
        // 获取该月的打卡数据
        val monthlyCounts = CheckInItemsRepository.getMonthlyCountsForItem(itemId, currentYear, currentMonth)
        
        // 更新热力图显示
        binding.heatmapView.updateData(monthlyCounts)
    }
    
    private fun updateUI() {
        // 更新打卡项目信息
        checkInItem = checkInItem?.id?.let { CheckInItemsRepository.getItemById(it) }
        setupUI()
    }
    
    private fun setupButtons() {
        binding.btnAddCount.setOnClickListener {
            checkInItem?.let { item ->
                // 增加打卡次数
                CheckInItemsRepository.incrementCount(item.id)
                
                // 更新当前显示
                updateUI()
                updateHeatmap(item.id)
                updateRecordsList(item.id)
            }
        }
    }
}