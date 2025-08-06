package com.morgen.rentalmanager.myapplication.miniprogram

import android.util.Log
import com.morgen.rentalmanager.myapplication.*
import kotlin.math.max

/**
 * 数据转换器，将小程序数据格式转换为Android应用数据格式
 */
class DataConverter {
    private val tag = "DataConverter"

    /**
     * 转换租户数据
     * @param miniprogramTenants 小程序租户数据列表
     * @return Android应用租户数据列表
     */
    fun convertTenants(miniprogramTenants: List<MiniprogramTenant>): List<Tenant> {
        Log.d(tag, "开始转换租户数据，数量: ${miniprogramTenants.size}")
        
        return miniprogramTenants.mapIndexed { index, miniprogramTenant ->
            try {
                val tenant = Tenant(
                    roomNumber = miniprogramTenant.roomNumber.trim(),
                    name = miniprogramTenant.name.trim(),
                    rent = max(0.0, miniprogramTenant.rent) // 确保租金不为负数
                )
                
                Log.d(tag, "转换租户 ${index + 1}: ${tenant.roomNumber} - ${tenant.name}")
                tenant
            } catch (e: Exception) {
                Log.e(tag, "转换租户数据失败: ${miniprogramTenant.roomNumber}", e)
                throw DataConversionException("转换租户数据失败 (房间号: ${miniprogramTenant.roomNumber}): ${e.message}")
            }
        }.also {
            Log.d(tag, "租户数据转换完成，成功转换 ${it.size} 个租户")
        }
    }

    /**
     * 转换账单数据
     * @param miniprogramBills 小程序账单数据
     * @return Android应用账单数据列表
     */
    fun convertBills(miniprogramBills: Map<String, Map<String, MiniprogramBill>>): List<BillWithDetails> {
        Log.d(tag, "开始转换账单数据，房间数量: ${miniprogramBills.size}")
        
        val billsWithDetails = mutableListOf<BillWithDetails>()
        var totalBillCount = 0
        val errors = mutableListOf<String>()
        
        miniprogramBills.forEach { (roomNumber, monthlyBills) ->
            Log.d(tag, "转换房间 $roomNumber 的账单，月份数量: ${monthlyBills.size}")
            
            monthlyBills.forEach { (originalMonth, miniprogramBill) ->
                try {
                    // 确保月份格式为"yyyy-MM"
                    val month = formatMonth(originalMonth)
                    
                    val billWithDetails = convertSingleBill(roomNumber, month, miniprogramBill)
                    billsWithDetails.add(billWithDetails)
                    totalBillCount++
                    
                    Log.d(tag, "转换账单: $roomNumber - $month, 明细数量: ${billWithDetails.details.size}")
                } catch (e: Exception) {
                    // 记录错误但继续处理其他账单
                    val errorMsg = "转换账单失败: $roomNumber - $originalMonth: ${e.message}"
                    Log.e(tag, errorMsg, e)
                    errors.add(errorMsg)
                }
            }
        }
        
        // 如果有错误但也有成功转换的账单，则继续返回成功的部分
        if (errors.isNotEmpty()) {
            Log.w(tag, "账单转换过程中发生 ${errors.size} 个错误，但成功转换了 $totalBillCount 个账单")
            errors.forEach { Log.w(tag, "错误详情: $it") }
            
            // 如果没有成功转换任何账单，则抛出异常
            if (billsWithDetails.isEmpty()) {
                throw DataConversionException("所有账单转换均失败: ${errors.firstOrNull()}")
            }
        } else {
            Log.d(tag, "账单数据转换完成，成功转换 $totalBillCount 个账单")
        }
        
        return billsWithDetails
    }

    /**
     * 转换单个账单
     * @param roomNumber 房间号
     * @param month 账单月份（标准格式：yyyy-MM）
     * @param miniprogramBill 小程序账单数据
     * @return 转换后的Android账单数据（包含明细）
     */
    private fun convertSingleBill(roomNumber: String, month: String, miniprogramBill: MiniprogramBill): BillWithDetails {
        Log.d(tag, "转换单个账单: 房间号=$roomNumber, 月份=$month")
        val details = mutableListOf<BillDetail>()
        
        try {
            // 打印原始账单数据，帮助调试
            Log.d(tag, "原始账单数据: water=${miniprogramBill.water != null}, electricity=${miniprogramBill.electricity != null}, " +
                  "meters=${miniprogramBill.meters?.size ?: 0}, extraData=${miniprogramBill.extraData != null}, " +
                  "rent=${miniprogramBill.rent}, total=${miniprogramBill.total}")
            
            // 转换主水表/电表（MeterData => BillDetail）
            miniprogramBill.water?.let { data ->
                try {
                    val waterDetail = convertMeterDataToBillDetail(data, "water", "主水表")
                    details.add(waterDetail)
                    Log.d(tag, "添加水表明细: 用量=${waterDetail.usage}, 金额=${waterDetail.amount}")
                } catch (e: Exception) {
                    Log.e(tag, "转换水表数据失败", e)
                    // 创建一个基本的水表明细，确保不会因为一个明细失败而导致整个账单转换失败
                    details.add(BillDetail(
                        parentBillId = 0,
                        type = "water",
                        name = "主水表",
                        amount = data.amount ?: 0.0
                    ))
                }
            }

            miniprogramBill.electricity?.let { data ->
                try {
                    val electricityDetail = convertMeterDataToBillDetail(data, "electricity", "主电表")
                    details.add(electricityDetail)
                    Log.d(tag, "添加电表明细: 用量=${electricityDetail.usage}, 金额=${electricityDetail.amount}")
                } catch (e: Exception) {
                    Log.e(tag, "转换电表数据失败", e)
                    // 创建一个基本的电表明细
                    details.add(BillDetail(
                        parentBillId = 0,
                        type = "electricity",
                        name = "主电表",
                        amount = data.amount ?: 0.0
                    ))
                }
            }

            // 转换其他表计数据（meters数组）
            miniprogramBill.meters?.forEachIndexed { index, meter ->
                try {
                    val meterType = meter.type ?: "unknown"
                    val detail = convertMeterToBillDetail(meter, meterType)
                    
                    // 如果名称为空，给一个默认名称
                    val finalDetail = if (detail.name.isBlank()) {
                        when (meterType) {
                            "water" -> detail.copy(name = "水表${index + 1}")
                            "electricity" -> detail.copy(name = "电表${index + 1}")
                            else -> detail.copy(name = "${meterType}表${index + 1}")
                        }
                    } else {
                        detail
                    }
                    
                    details.add(finalDetail)
                    Log.d(tag, "添加表计明细: 类型=$meterType, 名称=${finalDetail.name}, 用量=${finalDetail.usage}, 金额=${finalDetail.amount}")
                } catch (e: Exception) {
                    Log.e(tag, "转换表计数据失败: index=$index", e)
                    // 创建一个基本的表计明细
                    val meterType = meter.type ?: "unknown"
                    val amount = meter.getAmountAsDouble() ?: 0.0
                    details.add(BillDetail(
                        parentBillId = 0,
                        type = meterType,
                        name = meter.name ?: "${meterType}表${index + 1}",
                        amount = amount
                    ))
                }
            }
            
            // 转换额外数据中的表计
            miniprogramBill.extraData?.let { extraData ->
                // 处理子水表
                extraData.subWaterMeters?.forEachIndexed { index, meter ->
                    try {
                        val detail = convertMeterToBillDetail(meter, "water")
                        // 如果名称为空，给一个默认名称
                        val finalDetail = if (detail.name.isBlank()) {
                            detail.copy(name = "子水表${index + 1}")
                        } else {
                            detail
                        }
                        details.add(finalDetail)
                        Log.d(tag, "添加子水表明细: 名称=${finalDetail.name}, 用量=${finalDetail.usage}, 金额=${finalDetail.amount}")
                    } catch (e: Exception) {
                        Log.e(tag, "转换子水表数据失败: index=$index", e)
                        // 创建一个基本的子水表明细
                        val amount = meter.getAmountAsDouble() ?: 0.0
                        details.add(BillDetail(
                            parentBillId = 0,
                            type = "water",
                            name = meter.name ?: "子水表${index + 1}",
                            amount = amount
                        ))
                    }
                }
                
                // 处理子电表
                extraData.subElectricityMeters?.forEachIndexed { index, meter ->
                    try {
                        val detail = convertMeterToBillDetail(meter, "electricity")
                        // 如果名称为空，给一个默认名称
                        val finalDetail = if (detail.name.isBlank()) {
                            detail.copy(name = "子电表${index + 1}")
                        } else {
                            detail
                        }
                        details.add(finalDetail)
                        Log.d(tag, "添加子电表明细: 名称=${finalDetail.name}, 用量=${finalDetail.usage}, 金额=${finalDetail.amount}")
                    } catch (e: Exception) {
                        Log.e(tag, "转换子电表数据失败: index=$index", e)
                        // 创建一个基本的子电表明细
                        val amount = meter.getAmountAsDouble() ?: 0.0
                        details.add(BillDetail(
                            parentBillId = 0,
                            type = "electricity",
                            name = meter.name ?: "子电表${index + 1}",
                            amount = amount
                        ))
                    }
                }
            }

            // 转换额外费用
            miniprogramBill.extraFees?.forEach { fee ->
                try {
                    val extraDetail = BillDetail(
                        parentBillId = 0, 
                        type = "extra", 
                        name = fee.name.ifBlank { "额外费用" }, 
                        amount = fee.amount
                    )
                    details.add(extraDetail)
                    Log.d(tag, "添加额外费用: 名称=${extraDetail.name}, 金额=${extraDetail.amount}")
                } catch (e: Exception) {
                    Log.e(tag, "转换额外费用失败", e)
                }
            }
            
            // 添加租金明细（如果存在）
            miniprogramBill.rent?.let { rent ->
                if (rent > 0) {
                    val rentDetail = BillDetail(
                        parentBillId = 0, 
                        type = "rent", 
                        name = "房租", 
                        amount = rent
                    )
                    details.add(rentDetail)
                    Log.d(tag, "添加租金明细: 金额=$rent")
                }
            }

            // 确保至少有一个明细
            if (details.isEmpty()) {
                Log.w(tag, "账单没有明细，添加一个空明细")
                details.add(BillDetail(
                    parentBillId = 0,
                    type = "other",
                    name = "其他费用",
                    amount = 0.0
                ))
            }

            // 计算总金额（优先使用小程序提供的总金额，如果没有则自行计算）
            val totalAmount = miniprogramBill.total ?: details.sumOf { it.amount }
            
            // 创建账单
            val bill = Bill(
                tenantRoomNumber = roomNumber,
                month = month,
                totalAmount = totalAmount,
                createdDate = System.currentTimeMillis()
            )
            
            // 注意：当前Bill类不支持paid和notes字段，如果需要这些字段，需要先修改Bill类
            
            Log.d(tag, "账单转换完成: 房间号=$roomNumber, 月份=$month, 总金额=$totalAmount, 明细数量=${details.size}")
            return BillWithDetails(bill = bill, details = details)
            
        } catch (e: Exception) {
            Log.e(tag, "转换账单失败: $roomNumber - $month", e)
            throw DataConversionException("转换账单数据失败 (房间号: $roomNumber, 月份: $month): ${e.message}", e)
        }
    }

    private fun convertMeterDataToBillDetail(data: MiniprogramMeterData, type:String, defaultName:String): BillDetail {
        return BillDetail(
            parentBillId = 0,
            type = type,
            name = defaultName,
            previousReading = data.previous,
            currentReading = data.current,
            usage = data.usage,
            amount = data.amount ?: 0.0,
            pricePerUnit = null
        )
    }

    /**
     * 转换表计数据为账单明细
     */
    private fun convertMeterToBillDetail(meter: MiniprogramMeter, type: String): BillDetail {
        val amount = meter.getAmountAsDouble() ?: 0.0
        val meterName = meter.name ?: ""
        
        return BillDetail(
            parentBillId = 0, // 将在保存时设置
            type = type,
            name = meterName,
            previousReading = meter.getPreviousAsDouble(),
            currentReading = meter.getCurrentAsDouble(),
            usage = meter.getUsageAsDouble(),
            pricePerUnit = null,
            amount = amount
        )
    }

    /**
     * 转换价格数据
     * @param miniprogramPrices 小程序价格数据
     * @return Android应用价格数据
     */
    fun convertPrices(miniprogramPrices: MiniprogramPrices?): Price {
        if (miniprogramPrices == null) {
            Log.d(tag, "小程序价格数据为空，使用默认价格")
            return Price(water = 4.0, electricity = 1.0)
        }
        
        Log.d(tag, "转换价格数据: 水价=${miniprogramPrices.waterPrice}, 电价=${miniprogramPrices.electricityPrice}")
        
        return try {
            // 确保价格不为负数，如果为负数或为零则使用默认值
            val waterPrice = when {
                miniprogramPrices.waterPrice <= 0 -> {
                    Log.w(tag, "水价不是正数: ${miniprogramPrices.waterPrice}，使用默认值4.0")
                    4.0
                }
                miniprogramPrices.waterPrice > 100 -> {
                    Log.w(tag, "水价异常高: ${miniprogramPrices.waterPrice}，可能单位不正确，但仍使用该值")
                    miniprogramPrices.waterPrice
                }
                else -> miniprogramPrices.waterPrice
            }
            
            val electricityPrice = when {
                miniprogramPrices.electricityPrice <= 0 -> {
                    Log.w(tag, "电价不是正数: ${miniprogramPrices.electricityPrice}，使用默认值1.0")
                    1.0
                }
                miniprogramPrices.electricityPrice > 10 -> {
                    Log.w(tag, "电价异常高: ${miniprogramPrices.electricityPrice}，可能单位不正确，但仍使用该值")
                    miniprogramPrices.electricityPrice
                }
                else -> miniprogramPrices.electricityPrice
            }
            
            Price(
                water = waterPrice,
                electricity = electricityPrice
            ).also {
                Log.d(tag, "价格数据转换完成: 水价=${it.water}, 电价=${it.electricity}")
            }
        } catch (e: Exception) {
            Log.e(tag, "转换价格数据失败", e)
            throw DataConversionException("转换价格数据失败: ${e.message}")
        }
    }
    
    /**
     * 从小程序数据中提取价格数据
     * @param miniprogramData 小程序数据
     * @return Android应用价格数据
     */
    fun extractAndConvertPrices(miniprogramData: MiniprogramData): Price {
        // 尝试从小程序数据中提取价格
        val miniprogramPrices = miniprogramData.prices
        
        // 如果小程序数据中没有价格信息，则尝试从账单中推断价格
        if (miniprogramPrices == null) {
            Log.d(tag, "小程序数据中没有价格信息，尝试从账单中推断价格")
            
            var waterPrice = 4.0  // 默认水价
            var electricityPrice = 1.0  // 默认电价
            
            // 从账单中查找水电表计数据，尝试计算价格
            miniprogramData.bills.values.forEach { monthlyBills ->
                monthlyBills.values.forEach { bill ->
                    // 尝试从水表数据中推断水价
                    bill.water?.let { water ->
                        if (water.usage != null && water.usage > 0 && water.amount != null && water.amount > 0) {
                            val calculatedPrice = water.amount / water.usage
                            if (calculatedPrice > 0 && calculatedPrice < 100) {  // 合理范围检查
                                waterPrice = calculatedPrice
                            }
                        }
                    }
                    
                    // 尝试从电表数据中推断电价
                    bill.electricity?.let { electricity ->
                        if (electricity.usage != null && electricity.usage > 0 && electricity.amount != null && electricity.amount > 0) {
                            val calculatedPrice = electricity.amount / electricity.usage
                            if (calculatedPrice > 0 && calculatedPrice < 10) {  // 合理范围检查
                                electricityPrice = calculatedPrice
                            }
                        }
                    }
                }
            }
            
            Log.d(tag, "从账单中推断的价格: 水价=$waterPrice, 电价=$electricityPrice")
            return Price(water = waterPrice, electricity = electricityPrice)
        }
        
        // 使用小程序提供的价格数据
        return convertPrices(miniprogramPrices)
    }

    /**
     * 验证转换后的数据
     */
    fun validateConvertedData(tenants: List<Tenant>, bills: List<BillWithDetails>, price: Price? = null): ValidationResult {
        Log.d(tag, "验证转换后的数据")
        
        // 使用DataValidator验证转换后的数据
        val validator = DataValidator()
        return validator.validateAndroidData(tenants, bills, price)
    }
}

/**
 * 数据转换异常
 */
class DataConversionException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

/**
 * 格式化月份字符串为"yyyy-MM"格式
 * 支持多种输入格式：
 * - "yyyy-MM"
 * - "yyyy/MM"
 * - "yyyy年MM月"
 * - "yyyyMM"
 */
fun formatMonth(month: String): String {
    return try {
        when {
            // 已经是标准格式 "yyyy-MM"
            month.matches(Regex("\\d{4}-\\d{2}")) -> month
            
            // 格式 "yyyy/MM"
            month.matches(Regex("\\d{4}/\\d{2}")) -> month.replace("/", "-")
            
            // 格式 "yyyy年MM月"
            month.matches(Regex("\\d{4}年\\d{2}月")) -> {
                val year = month.substring(0, 4)
                val monthNum = month.substring(5, 7)
                "$year-$monthNum"
            }
            
            // 格式 "yyyyMM"
            month.matches(Regex("\\d{6}")) -> {
                val year = month.substring(0, 4)
                val monthNum = month.substring(4, 6)
                "$year-$monthNum"
            }
            
            // 其他格式，尝试提取数字
            else -> {
                val digits = month.filter { it.isDigit() }
                if (digits.length >= 6) {
                    val year = digits.substring(0, 4)
                    val monthNum = digits.substring(4, 6).padStart(2, '0')
                    "$year-$monthNum"
                } else {
                    // 无法解析，返回原始字符串
                    month
                }
            }
        }
    } catch (e: Exception) {
        // 解析失败，返回原始字符串
        month
    }
}