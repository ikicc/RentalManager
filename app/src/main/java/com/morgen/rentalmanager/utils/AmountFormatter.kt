package com.morgen.rentalmanager.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * 金额格式化工具类
 * 统一处理应用中所有金额的显示格式，确保保留两位小数
 */
object AmountFormatter {
    
    // 创建专用的DecimalFormat实例，确保始终显示两位小数
    private val amountFormat = DecimalFormat("#0.00", DecimalFormatSymbols(Locale.getDefault()))
    private val usageFormat = DecimalFormat("#0.0", DecimalFormatSymbols(Locale.getDefault()))
    
    /**
     * 格式化金额，保留两位小数
     * @param amount 金额数值
     * @return 格式化后的金额字符串，如 "123.45"
     */
    fun formatAmount(amount: Double): String {
        return try {
            if (amount.isNaN() || !amount.isFinite()) {
                "0.00"
            } else {
                amountFormat.format(amount)
            }
        } catch (e: Exception) {
            "0.00"
        }
    }
    
    /**
     * 格式化金额，保留两位小数，并添加货币符号
     * @param amount 金额数值
     * @param currency 货币符号，默认为"¥"
     * @return 格式化后的金额字符串，如 "¥123.45"
     */
    fun formatAmountWithCurrency(amount: Double, currency: String = "¥"): String {
        return "$currency${formatAmount(amount)}"
    }
    
    /**
     * 格式化用量，保留一位小数
     * @param usage 用量数值
     * @return 格式化后的用量字符串，如 "123.4"
     */
    fun formatUsage(usage: Double): String {
        return try {
            if (usage.isNaN() || !usage.isFinite()) {
                "0.0"
            } else {
                usageFormat.format(usage)
            }
        } catch (e: Exception) {
            "0.0"
        }
    }
    
    /**
     * 格式化用量，保留一位小数，并添加单位
     * @param usage 用量数值
     * @param unit 单位，默认为"度"
     * @return 格式化后的用量字符串，如 "123.4度"
     */
    fun formatUsageWithUnit(usage: Double, unit: String = "度"): String {
        return "${formatUsage(usage)}$unit"
    }
    
    /**
     * 根据类型格式化用量，保留一位小数，并添加正确的单位
     * @param usage 用量数值
     * @param type 类型（"water"为水费，"electricity"为电费）
     * @return 格式化后的用量字符串，如 "123.4方" 或 "123.4度"
     */
    fun formatUsageWithTypeUnit(usage: Double, type: String): String {
        val unit = when (type) {
            "water" -> "方"
            "electricity" -> "度"
            else -> "度"
        }
        return formatUsageWithUnit(usage, unit)
    }
    
    /**
     * 安全地将字符串转换为Double
     * @param value 字符串值
     * @param defaultValue 默认值
     * @return 转换后的Double值
     */
    fun parseAmount(value: String, defaultValue: Double = 0.0): Double {
        return try {
            val cleanValue = value.replace("¥", "").replace(",", "").trim()
            if (cleanValue.isEmpty()) defaultValue else cleanValue.toDouble()
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    /**
     * 验证金额是否有效
     * @param amount 金额数值
     * @return 是否为有效金额
     */
    fun isValidAmount(amount: Double): Boolean {
        return !amount.isNaN() && amount.isFinite() && amount >= 0
    }
    
    /**
     * 验证用量是否有效
     * @param usage 用量数值
     * @return 是否为有效用量
     */
    fun isValidUsage(usage: Double): Boolean {
        return !usage.isNaN() && usage.isFinite() && usage >= 0
    }
}