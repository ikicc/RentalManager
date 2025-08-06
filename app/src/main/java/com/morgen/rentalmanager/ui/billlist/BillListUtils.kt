package com.morgen.rentalmanager.ui.billlist

import com.morgen.rentalmanager.myapplication.BillDetail
import com.morgen.rentalmanager.myapplication.BillWithDetails
import com.morgen.rentalmanager.myapplication.Tenant
import java.text.SimpleDateFormat
import java.util.*

/**
 * 账单列表功能的工具类
 * 提供数据转换、验证和格式化功能
 */
object BillListUtils {
    
    /**
     * 月份格式化器
     */
    private val monthFormatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val displayMonthFormatter = SimpleDateFormat("yyyy年MM月", Locale.getDefault())
    
    /**
     * 验证月份格式是否正确
     */
    fun isValidMonthFormat(month: String): Boolean {
        return try {
            monthFormatter.parse(month) != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 格式化月份显示
     */
    fun formatMonthForDisplay(month: String): String {
        return try {
            val date = monthFormatter.parse(month)
            date?.let { displayMonthFormatter.format(it) } ?: month
        } catch (e: Exception) {
            month
        }
    }
    
    /**
     * 获取当前月份字符串
     */
    fun getCurrentMonth(): String {
        return monthFormatter.format(Date())
    }
    
    /**
     * 从数据库数据转换为BillItem
     * 支持自定义水表/电表名称显示
     */
    suspend fun convertToBillItem(
        tenant: Tenant,
        billWithDetails: BillWithDetails?,
        repository: com.morgen.rentalmanager.myapplication.TenantRepository? = null
    ): BillItem {
        if (billWithDetails == null) {
            // 没有账单数据时，只显示租金
            return BillItem(
                roomNumber = tenant.roomNumber,
                tenantName = tenant.name,
                rent = tenant.rent,
                totalAmount = tenant.rent  // 没有账单时，总金额等于租金
            )
        }
        
        val bill = billWithDetails.bill
        val details = billWithDetails.details
        
        // 分类处理各种费用明细
        // 处理水表详情
        val waterDetails = billWithDetails.details
            .filter { it.type == "water" }
            .sortedWith(compareByDescending<BillDetail> { it.name.startsWith("主") }.thenBy { it.name })
            .map { detail ->
                try {
                    // 使用更宽松的表名识别规则
                    val containsWaterMeter = detail.name.contains("水表") 
                    val containsElectricityMeter = detail.name.contains("电表")
                    val isMainMeter = detail.name == "主水表" || detail.name == "主电表"
                    val isExtraMeter = (containsWaterMeter || containsElectricityMeter) && !isMainMeter
                    
                    // 只有额外表使用自定义名称
                    val displayName = if (isExtraMeter) {
                        val customName = repository?.getMeterDisplayName(detail.name, billWithDetails.bill.tenantRoomNumber) ?: detail.name
                        customName.ifBlank { detail.name }
            } else {
                detail.name
            }
            
            MeterDetail(
                name = displayName,
                originalName = detail.name,
                type = "water",
                        usage = detail.usage ?: 0.0,
                amount = detail.amount,
                        isPrimary = detail.name.startsWith("主")
                    )
                } catch (e: Exception) {
                    android.util.Log.e("BillListUtils", "处理水表详情失败", e)
                    MeterDetail(
                        name = detail.name,
                        originalName = detail.name,
                        type = "water",
                usage = detail.usage ?: 0.0,
                        amount = detail.amount,
                isPrimary = detail.name.startsWith("主")
            )
        }
            }

        // 处理电表详情
        val electricityDetails = billWithDetails.details
            .filter { it.type == "electricity" }
            .sortedWith(compareByDescending<BillDetail> { it.name.startsWith("主") }.thenBy { it.name })
            .map { detail ->
                try {
                    // 使用更宽松的表名识别规则
                    val containsWaterMeter = detail.name.contains("水表") 
                    val containsElectricityMeter = detail.name.contains("电表")
                    val isMainMeter = detail.name == "主水表" || detail.name == "主电表"
                    val isExtraMeter = (containsWaterMeter || containsElectricityMeter) && !isMainMeter
                    
                    // 只有额外表使用自定义名称
                    val displayName = if (isExtraMeter) {
                        val customName = repository?.getMeterDisplayName(detail.name, billWithDetails.bill.tenantRoomNumber) ?: detail.name
                        customName.ifBlank { detail.name }
            } else {
                detail.name
            }
            
            MeterDetail(
                name = displayName,
                originalName = detail.name,
                type = "electricity",
                        usage = detail.usage ?: 0.0,
                amount = detail.amount,
                        isPrimary = detail.name.startsWith("主")
                    )
                } catch (e: Exception) {
                    android.util.Log.e("BillListUtils", "处理电表详情失败", e)
                    MeterDetail(
                        name = detail.name,
                        originalName = detail.name,
                        type = "electricity",
                usage = detail.usage ?: 0.0,
                        amount = detail.amount,
                isPrimary = detail.name.startsWith("主")
            )
                }
        }
        
        // 创建其他费用详情列表
        val extraFees = details.filter { it.type == "extra" }.map { detail ->
            MeterDetail(
                name = detail.name,
                originalName = detail.name,
                type = "extra",
                usage = 0.0,
                amount = detail.amount,
                isPrimary = false
            )
        }
        
        // 计算汇总金额
        val waterAmount = waterDetails.sumOf { it.amount }
        val waterUsage = waterDetails.sumOf { it.usage ?: 0.0 }
        val electricityAmount = electricityDetails.sumOf { it.amount }
        val electricityUsage = electricityDetails.sumOf { it.usage ?: 0.0 }
        val extraAmount = extraFees.sumOf { it.amount }
        
        // 验证数据一致性
        val calculatedUtilitiesTotal = waterAmount + electricityAmount + extraAmount
        val actualUtilitiesTotal = bill.totalAmount
        
        val finalUtilitiesAmount = if (Math.abs(calculatedUtilitiesTotal - actualUtilitiesTotal) > 0.01) {
            calculatedUtilitiesTotal
        } else {
            actualUtilitiesTotal
        }
        
        // 总金额包含租金 + 所有费用明细
        val finalTotalAmount = tenant.rent + finalUtilitiesAmount
        
        return BillItem(
            roomNumber = tenant.roomNumber,
            tenantName = tenant.name,
            rent = tenant.rent,
            waterAmount = waterAmount,
            waterUsage = waterUsage,
            electricityAmount = electricityAmount,
            electricityUsage = electricityUsage,
            totalAmount = finalTotalAmount,
            waterMeters = waterDetails,
            electricityMeters = electricityDetails,
            extraFees = extraFees
        )
    }
    
    /**
     * 从BillItem列表计算汇总数据
     */
    fun calculateBillSummary(billItems: List<BillItem>): BillSummary {
        if (billItems.isEmpty()) {
            return BillSummary()
        }
        
        val totalRent = billItems.sumOf { it.rent }
        val totalWater = billItems.sumOf { it.waterAmount }
        val totalElectricity = billItems.sumOf { it.electricityAmount }
        // 现在BillItem的totalAmount已经包含了租金，所以直接求和即可
        val totalAmount = billItems.sumOf { it.totalAmount }
        
        return BillSummary(
            totalRent = totalRent,
            totalWater = totalWater,
            totalElectricity = totalElectricity,
            totalAmount = totalAmount
        )
    }
    
    /**
     * 验证账单数据的完整性
     */
    fun validateBillData(billItems: List<BillItem>): ValidationResult {
        val errors = mutableListOf<String>()
        
        billItems.forEachIndexed { index, item ->
            if (!item.isValid()) {
                errors.add("第${index + 1}个账单项数据无效: 房间号=${item.roomNumber}")
            }
            
            // 基本的数据有效性检查
            // 费用明细总金额应该大于等于水费和电费的总和（可能还有附加费）
            val calculatedUtilitiesTotal = item.waterAmount + item.electricityAmount
            if (calculatedUtilitiesTotal > item.totalAmount + 0.01) {
                errors.add("房间${item.roomNumber}的水电费用超过了总费用明细")
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
    
    /**
     * 排序账单列表
     */
    fun sortBillItems(billItems: List<BillItem>, sortBy: BillSortType = BillSortType.ROOM_NUMBER): List<BillItem> {
        return when (sortBy) {
            BillSortType.ROOM_NUMBER -> billItems.sortedBy { it.roomNumber }
            BillSortType.TENANT_NAME -> billItems.sortedBy { it.tenantName }
            BillSortType.TOTAL_AMOUNT_DESC -> billItems.sortedByDescending { it.totalAmount }
            BillSortType.TOTAL_AMOUNT_ASC -> billItems.sortedBy { it.totalAmount }
        }
    }
    
    /**
     * 过滤账单列表
     */
    fun filterBillItems(
        billItems: List<BillItem>,
        filter: BillFilter
    ): List<BillItem> {
        return billItems.filter { item ->
            when (filter) {
                BillFilter.ALL -> true
                BillFilter.HAS_WATER -> item.hasWaterAmount()
                BillFilter.HAS_ELECTRICITY -> item.hasElectricityAmount()
                BillFilter.RENT_ONLY -> !item.hasWaterAmount() && !item.hasElectricityAmount()
                BillFilter.HAS_UTILITIES -> item.hasWaterAmount() || item.hasElectricityAmount()
            }
        }
    }
    
    /**
     * 搜索账单列表
     */
    fun searchBillItems(
        billItems: List<BillItem>,
        query: String
    ): List<BillItem> {
        if (query.isBlank()) return billItems
        
        val lowerQuery = query.lowercase()
        return billItems.filter { item ->
            item.roomNumber.lowercase().contains(lowerQuery) ||
            item.tenantName.lowercase().contains(lowerQuery)
        }
    }
    
    /**
     * 格式化金额范围显示
     */
    fun formatAmountRange(min: Double, max: Double): String {
        return "${String.format("%.2f", min)} - ${String.format("%.2f", max)}元"
    }
    
    /**
     * 获取金额统计信息
     */
    fun getAmountStatistics(billItems: List<BillItem>): AmountStatistics {
        if (billItems.isEmpty()) {
            return AmountStatistics()
        }
        
        val amounts = billItems.map { it.totalAmount }
        return AmountStatistics(
            min = amounts.minOrNull() ?: 0.0,
            max = amounts.maxOrNull() ?: 0.0,
            average = amounts.average(),
            median = amounts.sorted().let { sorted ->
                val size = sorted.size
                if (size % 2 == 0) {
                    (sorted[size / 2 - 1] + sorted[size / 2]) / 2.0
                } else {
                    sorted[size / 2]
                }
            }
        )
    }
}

/**
 * 数据验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
) {
    fun getErrorMessage(): String {
        return errors.joinToString("\n")
    }
}

/**
 * 账单排序类型
 */
enum class BillSortType {
    ROOM_NUMBER,        // 按房间号排序
    TENANT_NAME,        // 按租户姓名排序
    TOTAL_AMOUNT_DESC,  // 按总金额降序
    TOTAL_AMOUNT_ASC    // 按总金额升序
}

/**
 * 账单过滤类型
 */
enum class BillFilter {
    ALL,            // 显示所有
    HAS_WATER,      // 有水费的
    HAS_ELECTRICITY, // 有电费的
    RENT_ONLY,      // 只有租金的
    HAS_UTILITIES   // 有水电费的
}

/**
 * 金额统计信息
 */
data class AmountStatistics(
    val min: Double = 0.0,
    val max: Double = 0.0,
    val average: Double = 0.0,
    val median: Double = 0.0
) {
    fun getFormattedMin(): String = String.format("%.2f", min)
    fun getFormattedMax(): String = String.format("%.2f", max)
    fun getFormattedAverage(): String = String.format("%.2f", average)
    fun getFormattedMedian(): String = String.format("%.2f", median)
}