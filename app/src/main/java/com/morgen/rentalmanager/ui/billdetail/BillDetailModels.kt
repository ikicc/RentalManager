package com.morgen.rentalmanager.ui.billdetail

import com.morgen.rentalmanager.myapplication.BillDetail
import com.morgen.rentalmanager.utils.AmountFormatter

/**
 * 账单详情UI状态
 */
sealed class BillDetailUiState {
    object Loading : BillDetailUiState()
    
    data class Success(
        val billDetail: BillDetailData
    ) : BillDetailUiState()
    
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : BillDetailUiState()
    
    object NotFound : BillDetailUiState()
}

/**
 * 账单详情数据模型
 */
data class BillDetailData(
    val roomNumber: String,
    val tenantName: String,
    val phone: String,
    val month: String,
    val rent: Double,
    val waterAmount: Double,
    val waterUsage: Double,
    val waterPreviousReading: Double?,
    val waterCurrentReading: Double?,
    val waterPricePerUnit: Double,
    val electricityAmount: Double,
    val electricityUsage: Double,
    val electricityPreviousReading: Double?,
    val electricityCurrentReading: Double?,
    val electricityPricePerUnit: Double,
    val extraAmount: Double,
    val totalAmount: Double,
    val details: List<BillDetailItem> = emptyList()
) {
    fun getFormattedRent(): String = AmountFormatter.formatAmount(rent)
    fun getFormattedWaterAmount(): String = AmountFormatter.formatAmount(waterAmount)
    fun getFormattedElectricityAmount(): String = AmountFormatter.formatAmount(electricityAmount)
    fun getFormattedExtraAmount(): String = AmountFormatter.formatAmount(extraAmount)
    fun getFormattedTotalAmount(): String = AmountFormatter.formatAmount(totalAmount)
    
    fun getFormattedWaterUsage(): String = AmountFormatter.formatUsage(waterUsage)
    fun getFormattedElectricityUsage(): String = AmountFormatter.formatUsage(electricityUsage)
    
    fun hasDetails(): Boolean = details.isNotEmpty()
    
    fun hasCalculationInfo(): Boolean {
        return (waterUsage > 0 && waterPricePerUnit > 0) || 
               (electricityUsage > 0 && electricityPricePerUnit > 0)
    }
}

/**
 * 账单明细项数据模型
 */
data class BillDetailItem(
    val name: String,
    val type: String, // "water", "electricity", "extra"
    val previousReading: Double?,
    val currentReading: Double?,
    val usage: Double?,
    val pricePerUnit: Double?,
    val amount: Double
) {
    fun getFormattedAmount(): String = AmountFormatter.formatAmount(amount)
    
    fun hasUsageInfo(): Boolean {
        return (usage != null && pricePerUnit != null && pricePerUnit > 0) ||
               (previousReading != null && currentReading != null)
    }
    
    fun getUsageDescription(): String {
        // 根据类型确定单位
        val unit = when (type) {
            "water" -> "方"
            "electricity" -> "度"
            else -> "度"
        }
        
        return when {
            usage != null && pricePerUnit != null && pricePerUnit > 0 -> {
                val usageStr = AmountFormatter.formatUsage(usage)
                val priceStr = AmountFormatter.formatAmount(pricePerUnit)
                val amountStr = AmountFormatter.formatAmount(amount)
                
                if (previousReading != null && currentReading != null) {
                    val prevStr = AmountFormatter.formatUsage(previousReading)
                    val currStr = AmountFormatter.formatUsage(currentReading)
                    "读数: ${prevStr} → ${currStr}, 用量: ${usageStr}${unit} × ¥${priceStr}/${unit} = ¥${amountStr}"
                } else {
                    "用量: ${usageStr}${unit} × ¥${priceStr}/${unit} = ¥${amountStr}"
                }
            }
            previousReading != null && currentReading != null -> {
                // 即使没有用量信息，但有表数时也显示
                val prevStr = AmountFormatter.formatUsage(previousReading)
                val currStr = AmountFormatter.formatUsage(currentReading)
                "读数: ${prevStr} → ${currStr}"
            }
            else -> "固定费用: ¥${getFormattedAmount()}"
        }
    }
}

/**
 * 将数据库实体转换为UI数据模型的扩展函数
 */
fun List<BillDetail>.toBillDetailItems(): List<BillDetailItem> {
    return this.map { detail ->
        BillDetailItem(
            name = detail.name,
            type = detail.type,
            previousReading = detail.previousReading,
            currentReading = detail.currentReading,
            usage = detail.usage,
            pricePerUnit = detail.pricePerUnit,
            amount = detail.amount
        )
    }
}