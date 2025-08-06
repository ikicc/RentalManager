package com.morgen.rentalmanager.ui.billlist

import androidx.compose.ui.graphics.Color
import com.morgen.rentalmanager.myapplication.BillWithDetails
import com.morgen.rentalmanager.myapplication.Tenant

/**
 * 账单列表数据映射器
 * 负责将数据库实体转换为UI模型
 */
object BillListDataMapper {
    
    // 添加Repository引用以支持自定义名称映射
    private var repository: com.morgen.rentalmanager.myapplication.TenantRepository? = null
    
    /**
     * 设置Repository实例以支持自定义名称映射
     */
    fun setRepository(repo: com.morgen.rentalmanager.myapplication.TenantRepository) {
        repository = repo
    }
    
    /**
     * 将租户和账单数据转换为BillItem列表
     * 支持自定义水表/电表名称显示
     */
    suspend fun mapToBillItems(
        tenants: List<Tenant>,
        billsWithDetails: List<BillWithDetails>,
        selectedMonth: String
    ): List<BillItem> {
        // 创建账单映射表，以房间号为键
        val billsMap = billsWithDetails.associateBy { it.bill.tenantRoomNumber }
        
        return tenants.map { tenant ->
            val billWithDetails = billsMap[tenant.roomNumber]
            // 确保repository不为null，这样才能正确获取自定义名称
            BillListUtils.convertToBillItem(tenant, billWithDetails, repository)
        }.let { items ->
            // 按房间号排序
            BillListUtils.sortBillItems(items, BillSortType.ROOM_NUMBER)
        }
    }
    
    /**
     * 将BillItem列表转换为BillSummary
     */
    fun mapToBillSummary(billItems: List<BillItem>): BillSummary {
        return BillListUtils.calculateBillSummary(billItems)
    }
    
    /**
     * 将BillSummary转换为ChartData列表
     */
    fun mapToChartData(
        summary: BillSummary,
        isDarkTheme: Boolean = false
    ): List<ChartData> {
        // 根据主题选择颜色
        val rentColor = if (isDarkTheme) Color(0xFF4CAF50) else Color(0xFF2E7D32)
        val waterColor = if (isDarkTheme) Color(0xFF2196F3) else Color(0xFF1565C0)
        val electricityColor = if (isDarkTheme) Color(0xFFFF9800) else Color(0xFFE65100)
        
        return ChartData.fromBillSummary(
            summary = summary,
            rentColor = rentColor,
            waterColor = waterColor,
            electricityColor = electricityColor
        )
    }
    
    /**
     * 创建成功状态的UI状态
     */
    fun mapToSuccessState(
        selectedMonth: String,
        billItems: List<BillItem>,
        isDarkTheme: Boolean = false
    ): BillListUiState.Success {
        val summary = mapToBillSummary(billItems)
        val chartData = mapToChartData(summary, isDarkTheme)
        
        return BillListUiState.Success(
            selectedMonth = selectedMonth,
            summary = summary,
            bills = billItems,
            chartData = chartData
        )
    }
    
    /**
     * 创建空状态的UI状态
     */
    fun mapToEmptyState(selectedMonth: String): BillListUiState.Empty {
        return BillListUiState.Empty(selectedMonth = selectedMonth)
    }
    
    /**
     * 创建错误状态的UI状态
     */
    fun mapToErrorState(
        message: String,
        throwable: Throwable? = null
    ): BillListUiState.Error {
        return BillListUiState.Error(
            message = message,
            throwable = throwable
        )
    }
    
    /**
     * 验证并转换数据
     * 支持自定义水表/电表名称显示
     */
    suspend fun validateAndMap(
        tenants: List<Tenant>,
        billsWithDetails: List<BillWithDetails>,
        selectedMonth: String,
        isDarkTheme: Boolean = false
    ): BillListUiState {
        return try {
            // 验证月份格式
            if (!BillListUtils.isValidMonthFormat(selectedMonth)) {
                return mapToErrorState("无效的月份格式: $selectedMonth")
            }
            
            // 转换数据（现在支持自定义名称）
            val billItems = mapToBillItems(tenants, billsWithDetails, selectedMonth)
            
            // 验证数据完整性
            val validationResult = BillListUtils.validateBillData(billItems)
            if (!validationResult.isValid) {
                return mapToErrorState("数据验证失败: ${validationResult.getErrorMessage()}")
            }
            
            // 检查是否有数据
            if (billItems.isEmpty() || !billItems.any { it.hasAnyAmount() }) {
                return mapToEmptyState(selectedMonth)
            }
            
            // 创建成功状态
            mapToSuccessState(selectedMonth, billItems, isDarkTheme)
            
        } catch (e: Exception) {
            mapToErrorState("数据转换失败: ${e.message}", e)
        }
    }
    
    /**
     * 更新图表数据的主题
     */
    fun updateChartDataTheme(
        chartData: List<ChartData>,
        isDarkTheme: Boolean
    ): List<ChartData> {
        return chartData.map { data ->
            val newColor = when (data.label) {
                "租金" -> if (isDarkTheme) Color(0xFF4CAF50) else Color(0xFF2E7D32)
                "水费" -> if (isDarkTheme) Color(0xFF2196F3) else Color(0xFF1565C0)
                "电费" -> if (isDarkTheme) Color(0xFFFF9800) else Color(0xFFE65100)
                else -> data.color
            }
            data.copy(color = newColor)
        }
    }
    
    /**
     * 获取默认的图表颜色
     */
    fun getDefaultChartColors(isDarkTheme: Boolean): Triple<Color, Color, Color> {
        return if (isDarkTheme) {
            Triple(
                Color(0xFF4CAF50), // 租金 - 绿色
                Color(0xFF2196F3), // 水费 - 蓝色
                Color(0xFFFF9800)  // 电费 - 橙色
            )
        } else {
            Triple(
                Color(0xFF2E7D32), // 租金 - 深绿色
                Color(0xFF1565C0), // 水费 - 深蓝色
                Color(0xFFE65100)  // 电费 - 深橙色
            )
        }
    }
    
    /**
     * 格式化显示月份
     */
    fun formatDisplayMonth(month: String): String {
        return BillListUtils.formatMonthForDisplay(month)
    }
    
    /**
     * 获取当前月份
     */
    fun getCurrentMonth(): String {
        return BillListUtils.getCurrentMonth()
    }
}