package com.morgen.rentalmanager.ui.billlist

import androidx.compose.ui.graphics.Color
import com.morgen.rentalmanager.utils.AmountFormatter

/**
 * 账单汇总数据模型
 * 用于显示当月所有租户的费用汇总统计
 */
data class BillSummary(
    val totalRent: Double = 0.0,
    val totalWater: Double = 0.0,
    val totalElectricity: Double = 0.0,
    val totalAmount: Double = 0.0
) {
    /**
     * 验证汇总数据的有效性
     */
    fun isValid(): Boolean {
        return totalRent >= 0 && totalWater >= 0 && totalElectricity >= 0 && totalAmount >= 0
    }
    
    /**
     * 检查是否有任何费用数据
     */
    fun hasAnyAmount(): Boolean {
        return totalRent > 0 || totalWater > 0 || totalElectricity > 0
    }
    
    /**
     * 格式化金额显示
     */
    fun formatAmount(amount: Double): String {
        return AmountFormatter.formatAmount(amount)
    }
    
    /**
     * 获取格式化的总租金
     */
    fun getFormattedTotalRent(): String = formatAmount(totalRent)
    
    /**
     * 获取格式化的总水费
     */
    fun getFormattedTotalWater(): String = formatAmount(totalWater)
    
    /**
     * 获取格式化的总电费
     */
    fun getFormattedTotalElectricity(): String = formatAmount(totalElectricity)
    
    /**
     * 获取格式化的总金额
     */
    fun getFormattedTotalAmount(): String = formatAmount(totalAmount)
}

/**
 * 水表/电表详情数据模型
 * 用于显示单个水表或电表的详细信息（包含自定义名称）
 */
data class MeterDetail(
    val name: String,           // 显示名称（可能是自定义名称）
    val originalName: String,   // 原始名称（数据库中的名称）
    val type: String,           // "water" or "electricity"
    val amount: Double = 0.0,
    val usage: Double = 0.0,
    val isPrimary: Boolean = false  // 是否为主表
) {
    /**
     * 获取格式化的金额
     */
    fun getFormattedAmount(): String = AmountFormatter.formatAmount(amount)
    
    /**
     * 获取格式化的用量
     */
    fun getFormattedUsage(): String = AmountFormatter.formatUsage(usage)
    
    /**
     * 获取用量单位
     */
    fun getUsageUnit(): String = if (type == "water") "方" else "度"
    
    /**
     * 获取完整的显示文本
     */
    fun getDisplayText(): String {
        return if (usage > 0) {
            "$name: ${getFormattedAmount()}元 (${getFormattedUsage()}${getUsageUnit()})"
        } else {
            "$name: ${getFormattedAmount()}元"
        }
    }
}

/**
 * 账单列表项数据模型
 * 用于显示单个租户的账单信息
 */
data class BillItem(
    val roomNumber: String,
    val tenantName: String,
    val phone: String = "",
    val rent: Double = 0.0,
    val waterAmount: Double = 0.0,
    val waterUsage: Double = 0.0,
    val electricityAmount: Double = 0.0,
    val electricityUsage: Double = 0.0,
    val totalAmount: Double = 0.0,
    // 新增详细水表和电表信息
    val waterMeters: List<MeterDetail> = emptyList(),
    val electricityMeters: List<MeterDetail> = emptyList(),
    val extraFees: List<MeterDetail> = emptyList()  // 其他费用
) {
    /**
     * 验证账单项数据的有效性
     */
    fun isValid(): Boolean {
        return try {
            roomNumber.isNotBlank() && 
            rent >= 0 && 
            waterAmount >= 0 && 
            electricityAmount >= 0 &&
            totalAmount >= 0 &&
            !rent.isNaN() &&
            !waterAmount.isNaN() &&
            !electricityAmount.isNaN() &&
            !totalAmount.isNaN() &&
            rent.isFinite() &&
            waterAmount.isFinite() &&
            electricityAmount.isFinite() &&
            totalAmount.isFinite()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 格式化金额显示
     */
    private fun formatAmount(amount: Double): String {
        return AmountFormatter.formatAmount(amount)
    }
    
    /**
     * 格式化用量显示
     */
    private fun formatUsage(usage: Double): String {
        return AmountFormatter.formatUsage(usage)
    }
    
    /**
     * 获取格式化的租金
     */
    fun getFormattedRent(): String = formatAmount(rent)
    
    /**
     * 获取格式化的水费
     */
    fun getFormattedWaterAmount(): String = formatAmount(waterAmount)
    
    /**
     * 获取格式化的水费用量
     */
    fun getFormattedWaterUsage(): String = formatUsage(waterUsage)
    
    /**
     * 获取格式化的电费
     */
    fun getFormattedElectricityAmount(): String = formatAmount(electricityAmount)
    
    /**
     * 获取格式化的电费用量
     */
    fun getFormattedElectricityUsage(): String = formatUsage(electricityUsage)
    
    /**
     * 获取格式化的总金额
     */
    fun getFormattedTotalAmount(): String = formatAmount(totalAmount)
    
    /**
     * 获取水费显示文本（包含用量信息）
     */
    fun getWaterDisplayText(): String {
        return if (waterUsage > 0) {
            "${getFormattedWaterAmount()}元 (${getFormattedWaterUsage()}方)"
        } else {
            "${getFormattedWaterAmount()}元"
        }
    }
    
    /**
     * 获取电费显示文本（包含用量信息）
     */
    fun getElectricityDisplayText(): String {
        return if (electricityUsage > 0) {
            "${getFormattedElectricityAmount()}元 (${getFormattedElectricityUsage()}度)"
        } else {
            "${getFormattedElectricityAmount()}元"
        }
    }
    
    /**
     * 检查是否有水费
     */
    fun hasWaterAmount(): Boolean = waterAmount > 0
    
    /**
     * 检查是否有电费
     */
    fun hasElectricityAmount(): Boolean = electricityAmount > 0
    
    /**
     * 检查是否有任何费用
     */
    fun hasAnyAmount(): Boolean = rent > 0 || waterAmount > 0 || electricityAmount > 0
    
    /**
     * 获取所有水表的显示文本列表
     */
    fun getWaterMeterDisplayTexts(): List<String> {
        return waterMeters.map { it.getDisplayText() }
    }
    
    /**
     * 获取所有电表的显示文本列表
     */
    fun getElectricityMeterDisplayTexts(): List<String> {
        return electricityMeters.map { it.getDisplayText() }
    }
    
    /**
     * 获取所有其他费用的显示文本列表
     */
    fun getExtraFeeDisplayTexts(): List<String> {
        return extraFees.map { it.getDisplayText() }
    }
    
    /**
     * 检查是否有多个水表
     */
    fun hasMultipleWaterMeters(): Boolean = waterMeters.size > 1
    
    /**
     * 检查是否有多个电表
     */
    fun hasMultipleElectricityMeters(): Boolean = electricityMeters.size > 1
    
    /**
     * 检查是否有其他费用
     */
    fun hasExtraFees(): Boolean = extraFees.isNotEmpty()
}

/**
 * 图表数据模型
 * 用于费用分布饼图显示
 */
data class ChartData(
    val label: String,
    val value: Double,
    val color: Color,
    val percentage: Float = 0f
) {
    /**
     * 验证图表数据的有效性
     */
    fun isValid(): Boolean {
        return label.isNotBlank() && value >= 0 && percentage >= 0f && percentage <= 100f
    }
    
    /**
     * 格式化数值显示
     */
    fun getFormattedValue(): String {
        return AmountFormatter.formatAmount(value)
    }
    
    /**
     * 格式化百分比显示
     */
    fun getFormattedPercentage(): String {
        return String.format("%.1f%%", percentage)
    }
    
    /**
     * 获取显示文本
     */
    fun getDisplayText(): String {
        return "$label: ${getFormattedValue()}元 (${getFormattedPercentage()})"
    }
    
    companion object {
        /**
         * 从汇总数据创建图表数据列表
         */
        fun fromBillSummary(
            summary: BillSummary,
            rentColor: Color,
            waterColor: Color,
            electricityColor: Color
        ): List<ChartData> {
            val total = summary.totalAmount
            if (total <= 0) return emptyList()
            
            val chartData = mutableListOf<ChartData>()
            
            if (summary.totalRent > 0) {
                chartData.add(
                    ChartData(
                        label = "租金",
                        value = summary.totalRent,
                        color = rentColor,
                        percentage = (summary.totalRent / total * 100).toFloat()
                    )
                )
            }
            
            if (summary.totalWater > 0) {
                chartData.add(
                    ChartData(
                        label = "水费",
                        value = summary.totalWater,
                        color = waterColor,
                        percentage = (summary.totalWater / total * 100).toFloat()
                    )
                )
            }
            
            if (summary.totalElectricity > 0) {
                chartData.add(
                    ChartData(
                        label = "电费",
                        value = summary.totalElectricity,
                        color = electricityColor,
                        percentage = (summary.totalElectricity / total * 100).toFloat()
                    )
                )
            }
            
            return chartData
        }
    }
}

/**
 * 账单列表UI状态管理
 * 用于管理界面的不同状态
 */
sealed class BillListUiState {
    /**
     * 加载中状态
     */
    object Loading : BillListUiState()
    
    /**
     * 成功状态
     */
    data class Success(
        val selectedMonth: String,
        val summary: BillSummary,
        val bills: List<BillItem>,
        val chartData: List<ChartData>
    ) : BillListUiState() {
        /**
         * 验证成功状态数据的有效性
         */
        fun isValid(): Boolean {
            return selectedMonth.isNotBlank() &&
                   summary.isValid() &&
                   bills.all { it.isValid() } &&
                   chartData.all { it.isValid() }
        }
        
        /**
         * 检查是否有账单数据
         */
        fun hasBills(): Boolean = bills.isNotEmpty()
        
        /**
         * 检查是否有图表数据
         */
        fun hasChartData(): Boolean = chartData.isNotEmpty()
        
        /**
         * 获取账单数量
         */
        fun getBillCount(): Int = bills.size
    }
    
    /**
     * 错误状态
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : BillListUiState() {
        /**
         * 获取用户友好的错误信息
         */
        fun getUserFriendlyMessage(): String {
            return when {
                message.contains("网络") -> "网络连接异常，请检查网络设置"
                message.contains("数据库") -> "数据读取失败，请重试"
                message.isEmpty() -> "未知错误，请重试"
                else -> message
            }
        }
        
        /**
         * 检查是否为网络错误
         */
        fun isNetworkError(): Boolean {
            return message.contains("网络") || 
                   throwable?.javaClass?.simpleName?.contains("IOException") == true
        }
        
        /**
         * 检查是否为数据库错误
         */
        fun isDatabaseError(): Boolean {
            return message.contains("数据库") || 
                   throwable?.javaClass?.simpleName?.contains("SQLException") == true
        }
    }
    
    /**
     * 空状态
     */
    data class Empty(
        val selectedMonth: String
    ) : BillListUiState() {
        /**
         * 获取空状态提示信息
         */
        fun getEmptyMessage(): String {
            return "${selectedMonth}月暂无账单数据"
        }
    }
}