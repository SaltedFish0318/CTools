package com.synzura.ctools.tools.checkin

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.synzura.ctools.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.*
import java.util.UUID

/**
 * 打卡项目存储库
 * 管理打卡项目的数据，并提供操作接口
 */
object CheckInItemsRepository {
    private const val PREFS_NAME = "check_in_prefs"
    private const val KEY_PREFIX_COUNT = "count_"
    private const val KEY_PREFIX_LAST_UPDATE = "last_update_"
    private const val KEY_RECORDS = "records"
    private const val DATE_FORMAT = "yyyy-MM-dd"
    
    private lateinit var prefs: SharedPreferences
    private val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
    
    private val _checkInItems = MutableStateFlow<List<CheckInItem>>(emptyList())
    val checkInItems: StateFlow<List<CheckInItem>> = _checkInItems.asStateFlow()

    private val _checkInRecords = MutableStateFlow<Map<String, List<CheckInRecord>>>(emptyMap())
    val checkInRecords: StateFlow<Map<String, List<CheckInRecord>>> = _checkInRecords.asStateFlow()
    
    init {
        // 初始化默认打卡项目
        val defaultItems = listOf(
            CheckInItem(
                id = UUID.randomUUID().toString(),
                name = "喝水",
                description = "保持水分摄入",
                iconResId = R.drawable.ic_water,
                count = 0,
                goal = 8,
                unit = "杯",
                order = 0
            ),
            CheckInItem(
                id = UUID.randomUUID().toString(),
                name = "如厕",
                description = "记录如厕次数",
                iconResId = R.drawable.ic_restroom,
                count = 0,
                goal = 0,
                unit = "次",
                order = 1
            ),
            CheckInItem(
                id = UUID.randomUUID().toString(),
                name = "锻炼",
                description = "每日锻炼",
                iconResId = R.drawable.ic_exercise,
                count = 0,
                goal = 1,
                unit = "小时",
                order = 2
            )
        )
        
        _checkInItems.value = defaultItems
        
        // 初始化空的记录列表
        val emptyRecords = defaultItems.associate { it.id to emptyList<CheckInRecord>() }
        _checkInRecords.value = emptyRecords
    }
    
    /**
     * 初始化存储库
     * @param context 应用上下文
     */
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // 初始化打卡项目列表
        val initialItems = loadUserItems() ?: _checkInItems.value
        
        // 加载打卡记录
        loadRecords()
        
        // 检查是否需要重置每日打卡次数
        val today = dateFormat.format(Date())
        
        // 从持久化存储中加载当天打卡数据
        val loadedItems = initialItems.map { item ->
            val lastUpdateKey = KEY_PREFIX_LAST_UPDATE + item.id
            val lastUpdate = prefs.getString(lastUpdateKey, "") ?: ""
            
            // 如果上次更新不是今天，重置打卡次数
            val count = if (lastUpdate == today) {
                prefs.getInt(KEY_PREFIX_COUNT + item.id, 0)
            } else {
                0
            }
            
            item.copy(count = count)
        }.sortedBy { it.order } // 确保按照order字段排序
        
        _checkInItems.value = loadedItems
        
        // 确保打卡记录与计数一致
        synchronizeRecordsWithCounts()
    }
    
    /**
     * 确保打卡记录与计数一致
     */
    private fun synchronizeRecordsWithCounts() {
        val today = dateFormat.format(Date())
        
        // 对于每个项目，确保今天的记录数量与计数一致
        _checkInItems.value.forEach { item ->
            val recordsForToday = _checkInRecords.value[item.id]?.filter { it.date == today } ?: emptyList()
            
            if (recordsForToday.size != item.count) {
                // 如果记录数量不一致，重新生成记录
                
                // 首先删除该项目今天的所有记录
                _checkInRecords.update { recordsMap ->
                    val currentRecords = recordsMap[item.id] ?: emptyList()
                    recordsMap + (item.id to currentRecords.filter { it.date != today })
                }
                
                // 然后添加正确数量的新记录
                if (item.count > 0) {
                    val newRecords = (1..item.count).map {
                        // 创建时间错开一点，避免完全相同
                        val timestamp = Date(System.currentTimeMillis() - (it * 100))
                        CheckInRecord(
                            id = UUID.randomUUID().toString(),
                            timestamp = timestamp,
                            itemId = item.id
                        )
                    }
                    _checkInRecords.update { recordsMap ->
                        val currentRecords = recordsMap[item.id] ?: emptyList()
                        recordsMap + (item.id to (currentRecords + newRecords))
                    }
                }
            }
        }
        
        // 保存更新后的记录
        saveRecords()
    }
    
    /**
     * 增加打卡次数
     * @param itemId 打卡项目ID
     */
    fun incrementCount(itemId: String) {
        _checkInItems.update { items ->
            items.map { item ->
                if (item.id == itemId) {
                    // 创建打卡记录
                    addRecord(itemId)
                    
                    val newCount = item.count + 1
                    // 保存到SharedPreferences
                    prefs.edit()
                        .putInt(KEY_PREFIX_COUNT + itemId, newCount)
                        .putString(KEY_PREFIX_LAST_UPDATE + itemId, dateFormat.format(Date()))
                        .apply()
                    
                    item.copy(count = newCount)
                } else {
                    item
                }
            }
        }
        
        // 保存记录
        saveRecords()
    }
    
    /**
     * 获取指定ID的打卡项目
     * @param itemId 打卡项目ID
     * @return 打卡项目，如果不存在则返回null
     */
    fun getItemById(itemId: String): CheckInItem? {
        return _checkInItems.value.find { it.id == itemId }
    }
    
    /**
     * 重置所有打卡项目的打卡次数
     */
    fun resetAllCounts() {
        val currentItems = _checkInItems.value.map { it.copy(count = 0) }
        _checkInItems.value = currentItems
        
        // 清除持久化存储中的数据
        val editor = prefs.edit()
        _checkInItems.value.forEach { item ->
            editor.putInt(KEY_PREFIX_COUNT + item.id, 0)
        }
        editor.apply()
    }
    
    /**
     * 添加自定义打卡项目
     * @param name 项目名称
     * @param description 项目描述
     * @param iconResId 图标资源ID
     * @param goal 目标次数 (0表示无限制)
     * @param unit 单位
     * @return 新创建的打卡项目
     */
    fun addCustomItem(name: String, description: String, iconResId: Int, goal: Int = 0, unit: String = "") {
        val nextOrder = _checkInItems.value.size
        val newItem = CheckInItem(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            iconResId = iconResId,
            count = 0,
            goal = goal,
            unit = unit,
            order = nextOrder
        )
        
        _checkInItems.update { items ->
            items + newItem
        }
        
        // 为新项目创建空记录列表
        _checkInRecords.update { recordsMap ->
            recordsMap + (newItem.id to emptyList())
        }
    }
    
    /**
     * 删除打卡项目
     * @param itemId 打卡项目ID
     */
    fun deleteItem(itemId: String) {
        _checkInItems.update { items ->
            items.filter { it.id != itemId }
        }
        
        // 不删除记录，以便于恢复
    }
    
    /**
     * 保存用户自定义的项目列表
     */
    private fun saveUserItems() {
        // 这里简单处理，真实应用中可以使用JSON或其他序列化方式
        val itemIds = _checkInItems.value.map { it.id }.joinToString(",")
        val itemNames = _checkInItems.value.map { it.name }.joinToString(",")
        val itemDescs = _checkInItems.value.map { it.description }.joinToString(",")
        val itemIcons = _checkInItems.value.map { it.iconResId.toString() }.joinToString(",")
        val itemGoals = _checkInItems.value.map { it.goal.toString() }.joinToString(",")
        val itemUnits = _checkInItems.value.map { it.unit }.joinToString(",")
        val itemOrders = _checkInItems.value.map { it.order.toString() }.joinToString(",")
        
        prefs.edit()
            .putString("item_ids", itemIds)
            .putString("item_names", itemNames)
            .putString("item_descs", itemDescs)
            .putString("item_icons", itemIcons)
            .putString("item_goals", itemGoals)
            .putString("item_units", itemUnits)
            .putString("item_orders", itemOrders)
            .apply()
    }
    
    /**
     * 加载用户自定义的项目列表
     * @return 打卡项目列表，如果没有则返回null
     */
    private fun loadUserItems(): List<CheckInItem>? {
        val itemIds = prefs.getString("item_ids", null) ?: return null
        val itemNames = prefs.getString("item_names", null) ?: return null
        val itemDescs = prefs.getString("item_descs", null) ?: return null
        val itemIcons = prefs.getString("item_icons", null) ?: return null
        val itemGoals = prefs.getString("item_goals", null) ?: return null
        val itemUnits = prefs.getString("item_units", null) ?: return null
        val itemOrders = prefs.getString("item_orders", null) ?: return null
        
        val ids = itemIds.split(",")
        val names = itemNames.split(",")
        val descs = itemDescs.split(",")
        val icons = itemIcons.split(",").map { it.toInt() }
        val goals = itemGoals.split(",").map { it.toInt() }
        val units = itemUnits.split(",")
        val orders = itemOrders.split(",").map { it.toInt() }
        
        if (ids.size != names.size || ids.size != descs.size || 
            ids.size != icons.size || ids.size != goals.size || 
            ids.size != units.size || ids.size != orders.size) {
            return null
        }
        
        return ids.indices.map { i ->
            CheckInItem(
                id = ids[i],
                name = names[i],
                description = descs[i],
                iconResId = icons[i],
                goal = goals[i],
                unit = units[i],
                order = orders[i]
            )
        }
    }
    
    /**
     * 添加打卡记录
     */
    private fun addRecord(itemId: String) {
        val newRecord = CheckInRecord(
            id = UUID.randomUUID().toString(),
            timestamp = Date()
        )
        
        _checkInRecords.update { recordsMap ->
            val currentRecords = recordsMap[itemId] ?: emptyList()
            val updatedRecords = currentRecords + newRecord
            recordsMap + (itemId to updatedRecords)
        }
    }
    
    /**
     * 获取指定打卡项目的所有记录
     * @param itemId 打卡项目ID
     * @return 该打卡项目的所有记录列表
     */
    fun getRecordsForItem(itemId: String): List<CheckInRecord> {
        return _checkInRecords.value[itemId] ?: emptyList()
    }
    
    /**
     * 获取最近7天的打卡次数统计
     * @param itemId 打卡项目ID
     * @return 过去7天每天的打卡次数列表，从最早到最近排序
     */
    fun getWeeklyCountsForItem(itemId: String): List<Pair<String, Int>> {
        val calendar = Calendar.getInstance()
        val result = mutableListOf<Pair<String, Int>>()
        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        
        // 计算过去7天
        for (i in 6 downTo 0) {
            val targetDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, -i)
            }
            val formattedDate = dateFormat.format(targetDate.time)
            val fullDate = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(targetDate.time)
            
            // 统计当天的打卡次数
            val count = _checkInRecords.value[itemId]?.count { it.date == fullDate } ?: 0
            result.add(Pair(formattedDate, count))
        }
        
        return result
    }
    
    /**
     * 获取指定月份的打卡次数统计
     * @param itemId 打卡项目ID
     * @param year 年份
     * @param month 月份 (0-11)
     * @return 该月每天的打卡次数映射
     */
    fun getMonthlyCountsForItem(itemId: String, year: Int, month: Int): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val fullDateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        
        // 获取该月的第一天和最后一天
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // 计算该月每一天的打卡次数
        for (day in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val dateString = fullDateFormat.format(calendar.time)
            
            // 统计当天的打卡次数
            val count = _checkInRecords.value[itemId]?.count { it.date == dateString } ?: 0
            result[dateString] = count
        }
        
        return result
    }
    
    /**
     * 获取指定日期的所有打卡记录
     * @param itemId 打卡项目ID
     * @param date 日期字符串，格式为yyyy-MM-dd
     * @return 指定日期的所有打卡记录
     */
    fun getRecordsForItemAndDate(itemId: String, date: String): List<CheckInRecord> {
        return _checkInRecords.value[itemId]?.filter { it.date == date } ?: emptyList()
    }
    
    /**
     * 删除打卡记录
     */
    fun deleteRecord(itemId: String, recordId: String) {
        _checkInRecords.update { recordsMap ->
            val currentRecords = recordsMap[itemId] ?: emptyList()
            val updatedRecords = currentRecords.filter { it.id != recordId }
            
            // 减少对应项目的计数
            _checkInItems.update { items ->
                items.map { item ->
                    if (item.id == itemId) {
                        val newCount = (item.count - 1).coerceAtLeast(0)
                        // 保存到SharedPreferences
                        prefs.edit()
                            .putInt(KEY_PREFIX_COUNT + itemId, newCount)
                            .apply()
                        
                        item.copy(count = newCount)
                    } else {
                        item
                    }
                }
            }
            
            recordsMap + (itemId to updatedRecords)
        }
        
        // 保存更新后的记录
        saveRecords()
    }
    
    /**
     * 保存打卡记录
     */
    private fun saveRecords() {
        val recordsData = _checkInRecords.value.flatMap { (itemId, records) ->
            records.map { record ->
                "${record.id}|${record.itemId}|${record.timestamp.time}|${record.date}"
            }
        }.joinToString("\n")
        
        prefs.edit()
            .putString(KEY_RECORDS, recordsData)
            .apply()
    }
    
    /**
     * 加载打卡记录
     */
    private fun loadRecords() {
        val recordsData = prefs.getString(KEY_RECORDS, "") ?: ""
        if (recordsData.isEmpty()) {
            _checkInRecords.value = emptyMap()
            return
        }
        
        val loadedRecords = recordsData.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size == 4) {
                try {
                    CheckInRecord(
                        id = parts[0],
                        itemId = parts[1],
                        timestamp = Date(parts[2].toLong()),
                        date = parts[3]
                    )
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }.groupBy { it.itemId }
        
        _checkInRecords.value = loadedRecords
    }
    
    /**
     * 获取指定打卡项目的最后一条记录
     * @param itemId 打卡项目ID
     * @return 最后一条记录，如果没有则返回null
     */
    fun getLastRecordForItem(itemId: String): CheckInRecord? {
        return _checkInRecords.value[itemId]?.maxByOrNull { it.timestamp }
    }
    
    /**
     * 更新打卡项目
     * @param item 更新后的打卡项目
     */
    fun updateItem(item: CheckInItem) {
        val currentItems = _checkInItems.value.toMutableList()
        val index = currentItems.indexOfFirst { it.id == item.id }
        
        if (index != -1) {
            currentItems[index] = item
            _checkInItems.value = currentItems
            
            // 保存用户自定义的项目列表
            saveUserItems()
        }
    }
    
    /**
     * 获取特定日期范围内的记录数量
     */
    fun getRecordsCountInRange(itemId: String, startDate: Date, endDate: Date): Int {
        val records = _checkInRecords.value[itemId] ?: emptyList()
        return records.count { 
            it.timestamp.time >= startDate.time && it.timestamp.time <= endDate.time 
        }
    }
    
    /**
     * 获取指定项目在一周内每天的打卡次数
     */
    fun getWeeklyRecordCounts(itemId: String): Map<Date, Int> {
        val records = _checkInRecords.value[itemId] ?: emptyList()
        val result = mutableMapOf<Date, Int>()
        
        // 获取过去7天的日期
        val today = Date()
        val calendar = java.util.Calendar.getInstance()
        calendar.time = today
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        // 获取一周前的日期
        for (i in 6 downTo 0) {
            calendar.time = today
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val dayStart = calendar.time
            
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
            calendar.set(java.util.Calendar.MINUTE, 59)
            calendar.set(java.util.Calendar.SECOND, 59)
            calendar.set(java.util.Calendar.MILLISECOND, 999)
            val dayEnd = calendar.time
            
            val count = records.count { 
                it.timestamp.time >= dayStart.time && it.timestamp.time <= dayEnd.time 
            }
            
            result[dayStart] = count
        }
        
        return result
    }
    
    /**
     * 更新打卡项目的排序
     */
    fun updateItemsOrder(items: List<CheckInItem>) {
        // 根据当前顺序更新order字段
        val updatedItems = items.mapIndexed { index, item ->
            item.copy(order = index)
        }
        
        // 更新内存中的数据
        _checkInItems.value = updatedItems
        
        // 保存到持久化存储
        saveUserItems()
    }
    
    /**
     * 添加打卡项目（用于恢复删除的项目）
     */
    fun addItem(item: CheckInItem) {
        _checkInItems.update { items ->
            items + item
        }
        
        // 如果没有记录，初始化空列表
        if (!_checkInRecords.value.containsKey(item.id)) {
            _checkInRecords.update { recordsMap ->
                recordsMap + (item.id to emptyList())
            }
        }
    }
} 