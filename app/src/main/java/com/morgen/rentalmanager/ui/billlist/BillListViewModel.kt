package com.morgen.rentalmanager.ui.billlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.morgen.rentalmanager.myapplication.TenantRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.io.IOException
import java.sql.SQLException

/**
 * 账单列表功能的ViewModel
 * 负责管理选中月份、UI状态和业务逻辑
 */
class BillListViewModel(
    private val repository: TenantRepository? = null
) : ViewModel() {
    
    init {
        // 设置Repository到DataMapper以支持自定义名称
        repository?.let { 
            BillListDataMapper.setRepository(it)
            android.util.Log.d("BillListViewModel", "Repository已设置到DataMapper，支持自定义名称显示")
        }
    }
    
    // 私有状态流
    private val _selectedMonth = MutableStateFlow(BillListDataMapper.getCurrentMonth())
    private val _uiState = MutableStateFlow<BillListUiState>(BillListUiState.Loading)
    private val _isDarkTheme = MutableStateFlow(false)
    private val _isRefreshing = MutableStateFlow(false)
    
    // 公开的状态流
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()
    val uiState: StateFlow<BillListUiState> = _uiState.asStateFlow()
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    init {
        // 初始化时加载数据
        loadBillData()
        
        // 监听月份变化自动刷新数据 - 添加异常处理
        viewModelScope.launch {
            try {
                _selectedMonth.collect { month ->
                    if (month.isNotBlank()) {
                        loadBillData(month)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BillListViewModel", "月份监听异常", e)
                handleError(e)
            }
        }
        
        // 监听租户数据变更事件，确保租金变更后立即刷新账单列表
        viewModelScope.launch {
            try {
                com.morgen.rentalmanager.myapplication.DataSyncManager.tenantDataChanged.collect { roomNumber ->
                    android.util.Log.d("BillListViewModel", "收到租户数据变更通知: $roomNumber，刷新账单列表")
                    // 刷新当前月份的账单数据
                    loadBillData(_selectedMonth.value, forceRefresh = true)
                }
            } catch (e: Exception) {
                android.util.Log.e("BillListViewModel", "租户数据监听异常", e)
                handleError(e)
            }
        }
        
        // 监听价格数据变更事件，确保单价变更后立即刷新
        viewModelScope.launch {
            try {
                com.morgen.rentalmanager.myapplication.DataSyncManager.priceDataChanged.collect {
                    android.util.Log.d("BillListViewModel", "收到价格数据变更通知，刷新账单列表")
                    // 刷新当前月份的账单数据
                    loadBillData(_selectedMonth.value, forceRefresh = true)
                }
            } catch (e: Exception) {
                android.util.Log.e("BillListViewModel", "价格数据监听异常", e)
                handleError(e)
            }
        }
        
        // 监听表名称变更事件，确保自定义名称变更后立即刷新
        viewModelScope.launch {
            try {
                com.morgen.rentalmanager.myapplication.DataSyncManager.meterNameChanged.collect { (originalName, customName) ->
                    android.util.Log.d("BillListViewModel", "收到表名称变更通知: $originalName -> $customName，刷新账单列表")
                    // 刷新当前月份的账单数据
                    loadBillData(_selectedMonth.value, forceRefresh = true)
                }
            } catch (e: Exception) {
                android.util.Log.e("BillListViewModel", "表名称变更监听异常", e)
                handleError(e)
            }
        }
    }
    
    /**
     * 选择月份
     * 需求: 1.2, 1.3 - 月份选择功能
     */
    fun selectMonth(month: String) {
        if (!BillListUtils.isValidMonthFormat(month)) {
            _uiState.value = BillListDataMapper.mapToErrorState("无效的月份格式: $month")
            return
        }
        
        if (_selectedMonth.value != month) {
            _selectedMonth.value = month
        }
    }
    
    /**
     * 刷新数据
     * 需求: 7.1, 7.2 - 数据加载和错误处理
     */
    fun refreshData() {
        loadBillData(_selectedMonth.value, forceRefresh = true)
    }
    
    /**
     * 设置深色主题
     * 需求: 6.1, 6.2 - 响应式设计和主题适配
     */
    fun setDarkTheme(isDark: Boolean) {
        if (_isDarkTheme.value != isDark) {
            _isDarkTheme.value = isDark
            // 如果当前是成功状态，更新图表颜色
            val currentState = _uiState.value
            if (currentState is BillListUiState.Success) {
                val updatedChartData = BillListDataMapper.updateChartDataTheme(
                    currentState.chartData,
                    isDark
                )
                _uiState.value = currentState.copy(chartData = updatedChartData)
            }
        }
    }
    
    /**
     * 重试加载数据
     * 需求: 7.2 - 错误处理和重试功能
     */
    fun retryLoadData() {
        loadBillData(_selectedMonth.value, forceRefresh = true)
    }
    
    // 添加加载任务管理，避免并发问题
    private var currentLoadingJob: kotlinx.coroutines.Job? = null
    
    /**
     * 加载账单数据
     * 需求: 2.2 - 账单汇总统计, 7.1 - 数据加载
     */
    private fun loadBillData(month: String = _selectedMonth.value, forceRefresh: Boolean = false) {
        // 取消之前的加载任务，避免并发问题
        currentLoadingJob?.cancel()
        
        currentLoadingJob = viewModelScope.launch {
            try {
                // 验证输入参数
                if (month.isBlank()) {
                    _uiState.value = BillListDataMapper.mapToErrorState("月份参数不能为空")
                    return@launch
                }
                
                // 设置加载状态
                if (forceRefresh) {
                    _isRefreshing.value = true
                } else {
                    _uiState.value = BillListUiState.Loading
                }
                
                // 在IO线程中加载数据，添加超时控制
                val result = withContext(Dispatchers.IO) {
                    try {
                        kotlinx.coroutines.withTimeout(30000) { // 30秒超时
                            loadDataFromRepository(month)
                        }
                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        BillListDataMapper.mapToErrorState("数据加载超时，请重试")
                    }
                }
                
                // 检查协程是否被取消
                if (!currentCoroutineContext().isActive) return@launch
                
                // 更新UI状态
                _uiState.value = result
                
            } catch (e: kotlinx.coroutines.CancellationException) {
                // 协程被取消，不需要处理
                android.util.Log.d("BillListViewModel", "数据加载被取消")
            } catch (e: Exception) {
                android.util.Log.e("BillListViewModel", "数据加载异常", e)
                handleError(e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    /**
     * 从Repository加载数据
     */
    private suspend fun loadDataFromRepository(month: String): BillListUiState {
        return try {
            // 如果没有repository，返回空状态（用于预览和测试）
            if (repository == null) {
                return BillListDataMapper.mapToEmptyState(month)
            }
            
            // 验证月份格式
            if (!BillListUtils.isValidMonthFormat(month)) {
                return BillListDataMapper.mapToErrorState("无效的月份格式: $month")
            }
            
            // 获取所有租户 - 添加超时和空值检查
            val tenants = try {
                repository.allTenants.first()
            } catch (e: Exception) {
                emptyList() // 如果获取租户失败，使用空列表
            }
            
            // 获取指定月份的所有账单 - 添加空值检查和异常处理
            val billsWithDetails = try {
                repository.getAllBillsWithDetails().first()
                    .filter { billWithDetail ->
                        try {
                            billWithDetail.bill.month == month
                        } catch (e: Exception) {
                            false // 如果比较失败，排除该项
                        }
                    }
            } catch (e: Exception) {
                emptyList() // 如果获取账单失败，使用空列表
            }
            
            // 使用数据映射器转换和验证数据（现在支持自定义名称）
            BillListDataMapper.validateAndMap(
                tenants = tenants,
                billsWithDetails = billsWithDetails,
                selectedMonth = month,
                isDarkTheme = _isDarkTheme.value
            )
            
        } catch (e: Exception) {
            // 记录详细错误信息用于调试
            android.util.Log.e("BillListViewModel", "数据加载失败", e)
            BillListDataMapper.mapToErrorState(
                message = "数据加载失败: ${e.message}",
                throwable = e
            )
        }
    }
    
    /**
     * 处理错误
     * 需求: 7.1, 7.2 - 错误处理
     */
    private fun handleError(throwable: Throwable) {
        val errorMessage = when (throwable) {
            is IOException -> "网络连接异常，请检查网络设置"
            is SQLException -> "数据读取失败，请重试"
            is IllegalArgumentException -> "参数错误: ${throwable.message}"
            is IllegalStateException -> "状态错误: ${throwable.message}"
            else -> "未知错误: ${throwable.message}"
        }
        
        _uiState.value = BillListDataMapper.mapToErrorState(errorMessage, throwable)
    }
    
    /**
     * 获取当前成功状态的数据
     */
    fun getCurrentSuccessData(): BillListUiState.Success? {
        return _uiState.value as? BillListUiState.Success
    }
    
    /**
     * 检查是否有数据
     */
    fun hasData(): Boolean {
        val currentState = _uiState.value
        return currentState is BillListUiState.Success && currentState.hasBills()
    }
    
    /**
     * 获取账单数量
     */
    fun getBillCount(): Int {
        val currentState = _uiState.value
        return if (currentState is BillListUiState.Success) {
            currentState.getBillCount()
        } else {
            0
        }
    }
    
    /**
     * 获取格式化的月份显示
     */
    fun getFormattedMonth(): String {
        return BillListDataMapper.formatDisplayMonth(_selectedMonth.value)
    }
    
    /**
     * 检查是否为当前月份
     */
    fun isCurrentMonth(): Boolean {
        return _selectedMonth.value == BillListDataMapper.getCurrentMonth()
    }
    
    /**
     * 获取上一个月份
     */
    fun getPreviousMonth(): String {
        return try {
            val currentMonth = _selectedMonth.value
            val parts = currentMonth.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            
            if (month == 1) {
                "${year - 1}-12"
            } else {
                "$year-${String.format("%02d", month - 1)}"
            }
        } catch (e: Exception) {
            _selectedMonth.value
        }
    }
    
    /**
     * 获取下一个月份
     */
    fun getNextMonth(): String {
        return try {
            val currentMonth = _selectedMonth.value
            val parts = currentMonth.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            
            if (month == 12) {
                "${year + 1}-01"
            } else {
                "$year-${String.format("%02d", month + 1)}"
            }
        } catch (e: Exception) {
            _selectedMonth.value
        }
    }
    
    /**
     * 切换到上一个月
     */
    fun selectPreviousMonth() {
        selectMonth(getPreviousMonth())
    }
    
    /**
     * 切换到下一个月
     */
    fun selectNextMonth() {
        selectMonth(getNextMonth())
    }
    
    /**
     * 重置到当前月份
     */
    fun resetToCurrentMonth() {
        selectMonth(BillListDataMapper.getCurrentMonth())
    }
    
    /**
     * 清理资源
     */
    override fun onCleared() {
        super.onCleared()
        // 取消所有正在进行的协程任务
        currentLoadingJob?.cancel()
        android.util.Log.d("BillListViewModel", "ViewModel资源已清理")
    }
}
/**
 *
 BillListViewModel的工厂类
 * 用于创建ViewModel实例并注入依赖
 */
class BillListViewModelFactory(
    private val repository: TenantRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BillListViewModel::class.java)) {
            return BillListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}