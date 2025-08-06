package com.morgen.rentalmanager.myapplication.miniprogram

import android.util.Log
import com.morgen.rentalmanager.myapplication.Tenant
import com.morgen.rentalmanager.myapplication.BillWithDetails
import com.morgen.rentalmanager.myapplication.Price

/**
 * 数据验证器，用于验证小程序JSON数据和转换后的Android数据的完整性和有效性
 */
class DataValidator {
    private val tag = "DataValidator"
    
    /**
     * 验证小程序JSON数据
     * @param data 小程序数据
     * @return 验证结果
     */
    fun validateMiniprogramData(data: MiniprogramData): ValidationResult {
        Log.d(tag, "开始验证小程序数据")
        
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // 验证租户数据
        validateTenants(data.tenants, errors, warnings)
        
        // 验证账单数据
        validateBills(data.bills, data.tenants, errors, warnings)
        
        // 验证价格数据
        validatePrices(data.prices, errors, warnings)
        
        // 验证合同数据（如果有）
        data.contracts?.let { validateContracts(it, data.tenants, errors, warnings) }
        
        val isValid = errors.isEmpty()
        Log.d(tag, "数据验证完成: 有效=$isValid, 错误数量=${errors.size}, 警告数量=${warnings.size}")
        
        return ValidationResult(
            isValid = isValid,
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 验证租户数据
     */
    private fun validateTenants(
        tenants: List<MiniprogramTenant>,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        Log.d(tag, "验证租户数据，数量: ${tenants.size}")
        
        if (tenants.isEmpty()) {
            warnings.add("没有租户数据")
            return
        }
        
        // 检查重复的房间号
        val roomNumbers = tenants.map { it.roomNumber }
        val uniqueRoomNumbers = roomNumbers.toSet()
        if (roomNumbers.size != uniqueRoomNumbers.size) {
            val duplicates = roomNumbers.groupBy { it }.filter { it.value.size > 1 }.keys
            errors.add("存在重复的房间号: ${duplicates.joinToString(", ")}")
        }
        
        // 验证每个租户的数据
        tenants.forEachIndexed { index, tenant ->
            // 验证房间号
            if (tenant.roomNumber.isBlank()) {
                errors.add("租户 #${index + 1} 的房间号为空")
            }
            
            // 验证姓名
            if (tenant.name.isBlank()) {
                errors.add("租户 #${index + 1} (房间号: ${tenant.roomNumber}) 的姓名为空")
            }
            
            // 验证租金
            if (tenant.rent < 0) {
                errors.add("租户 #${index + 1} (房间号: ${tenant.roomNumber}) 的租金为负数: ${tenant.rent}")
            }
        }
    }
    
    /**
     * 验证账单数据
     */
    private fun validateBills(
        bills: Map<String, Map<String, MiniprogramBill>>,
        tenants: List<MiniprogramTenant>,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        Log.d(tag, "验证账单数据，房间数量: ${bills.size}")
        
        if (bills.isEmpty()) {
            warnings.add("没有账单数据")
            return
        }
        
        // 获取所有租户的房间号
        val tenantRoomNumbers = tenants.map { it.roomNumber }.toSet()
        
        // 验证每个房间的账单
        bills.forEach { (roomNumber, monthlyBills) ->
            // 检查账单对应的租户是否存在
            if (roomNumber !in tenantRoomNumbers) {
                warnings.add("账单对应的租户不存在: 房间号 $roomNumber")
            }
            
            // 验证每个月份的账单
            monthlyBills.forEach { (month, bill) ->
                // 验证月份格式
                if (!isValidMonthFormat(month)) {
                    warnings.add("账单月份格式不标准: $month (房间号: $roomNumber)")
                }
                
                // 验证水表数据
                bill.water?.let { water ->
                    validateMeterData(water, "水表", roomNumber, month, errors, warnings)
                }
                
                // 验证电表数据
                bill.electricity?.let { electricity ->
                    validateMeterData(electricity, "电表", roomNumber, month, errors, warnings)
                }
                
                // 验证其他表计数据
                bill.meters?.forEach { meter ->
                    validateMeter(meter, roomNumber, month, errors, warnings)
                }
                
                // 验证额外数据
                bill.extraData?.let { extraData ->
                    // 验证子水表
                    extraData.subWaterMeters?.forEach { meter ->
                        validateMeter(meter, roomNumber, month, errors, warnings)
                    }
                    
                    // 验证子电表
                    extraData.subElectricityMeters?.forEach { meter ->
                        validateMeter(meter, roomNumber, month, errors, warnings)
                    }
                }
                
                // 验证额外费用
                bill.extraFees?.forEach { fee ->
                    if (fee.name.isBlank()) {
                        warnings.add("额外费用名称为空 (房间号: $roomNumber, 月份: $month)")
                    }
                    if (fee.amount < 0) {
                        warnings.add("额外费用金额为负数: ${fee.name} = ${fee.amount} (房间号: $roomNumber, 月份: $month)")
                    }
                }
                
                // 验证总金额
                bill.total?.let { total ->
                    if (total < 0) {
                        errors.add("账单总金额为负数: $total (房间号: $roomNumber, 月份: $month)")
                    }
                }
            }
        }
    }
    
    /**
     * 验证表计数据
     */
    private fun validateMeterData(
        data: MiniprogramMeterData,
        meterType: String,
        roomNumber: String,
        month: String,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        // 验证读数
        if (data.previous != null && data.previous < 0) {
            warnings.add("$meterType 上次读数为负数: ${data.previous} (房间号: $roomNumber, 月份: $month)")
        }
        
        if (data.current != null && data.current < 0) {
            warnings.add("$meterType 本次读数为负数: ${data.current} (房间号: $roomNumber, 月份: $month)")
        }
        
        // 验证用量
        if (data.usage != null) {
            if (data.usage < 0) {
                warnings.add("$meterType 用量为负数: ${data.usage} (房间号: $roomNumber, 月份: $month)")
            }
            
            // 检查用量是否与读数一致
            if (data.previous != null && data.current != null) {
                val calculatedUsage = data.current - data.previous
                if (Math.abs(calculatedUsage - data.usage) > 0.01) {
                    warnings.add("$meterType 用量与读数不一致: 读数差=${calculatedUsage}, 用量=${data.usage} (房间号: $roomNumber, 月份: $month)")
                }
            }
        }
        
        // 验证金额
        if (data.amount != null && data.amount < 0) {
            warnings.add("$meterType 金额为负数: ${data.amount} (房间号: $roomNumber, 月份: $month)")
        }
    }
    
    /**
     * 验证表计
     */
    private fun validateMeter(
        meter: MiniprogramMeter,
        roomNumber: String,
        month: String,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        // 验证类型
        if (meter.type?.isBlank() != false) {
            warnings.add("表计类型为空 (房间号: $roomNumber, 月份: $month)")
        }
        
        // 验证名称
        if (meter.name?.isBlank() != false) {
            warnings.add("表计名称为空 (房间号: $roomNumber, 月份: $month, 类型: ${meter.type ?: "未知"})")
        }
        
        // 验证读数（使用辅助方法转换字符串到Double）
        val previousReading = meter.getPreviousAsDouble()
        val currentReading = meter.getCurrentAsDouble()
        val usage = meter.getUsageAsDouble()
        val amount = meter.getAmountAsDouble()
        
        if (previousReading != null && previousReading < 0) {
            warnings.add("表计 ${meter.name} 上次读数为负数: $previousReading (房间号: $roomNumber, 月份: $month)")
        }
        
        if (currentReading != null && currentReading < 0) {
            warnings.add("表计 ${meter.name} 本次读数为负数: $currentReading (房间号: $roomNumber, 月份: $month)")
        }
        
        // 验证用量
        if (usage != null) {
            if (usage < 0) {
                warnings.add("表计 ${meter.name} 用量为负数: $usage (房间号: $roomNumber, 月份: $month)")
            }
            
            // 检查用量是否与读数一致
            if (previousReading != null && currentReading != null) {
                val calculatedUsage = currentReading - previousReading
                if (Math.abs(calculatedUsage - usage) > 0.01) {
                    warnings.add("表计 ${meter.name} 用量与读数不一致: 读数差=${calculatedUsage}, 用量=${usage} (房间号: $roomNumber, 月份: $month)")
                }
            }
        }
        
        // 验证金额
        if (amount != null && amount < 0) {
            warnings.add("表计 ${meter.name} 金额为负数: $amount (房间号: $roomNumber, 月份: $month)")
        }
    }
    
    /**
     * 验证价格数据
     */
    private fun validatePrices(
        prices: MiniprogramPrices?,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        if (prices == null) {
            warnings.add("没有价格数据，将使用默认价格")
            return
        }
        
        Log.d(tag, "验证价格数据: 水价=${prices.waterPrice}, 电价=${prices.electricityPrice}")
        
        // 验证水价
        if (prices.waterPrice <= 0) {
            warnings.add("水价不是正数: ${prices.waterPrice}，将使用默认值")
        } else if (prices.waterPrice > 100) {
            warnings.add("水价异常高: ${prices.waterPrice}，请确认单位是否正确")
        }
        
        // 验证电价
        if (prices.electricityPrice <= 0) {
            warnings.add("电价不是正数: ${prices.electricityPrice}，将使用默认值")
        } else if (prices.electricityPrice > 10) {
            warnings.add("电价异常高: ${prices.electricityPrice}，请确认单位是否正确")
        }
    }
    
    /**
     * 验证合同数据
     */
    private fun validateContracts(
        contracts: Map<String, MiniprogramContract>,
        tenants: List<MiniprogramTenant>,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        Log.d(tag, "验证合同数据，数量: ${contracts.size}")
        
        if (contracts.isEmpty()) {
            warnings.add("没有合同数据")
            return
        }
        
        // 获取所有租户的房间号
        val tenantRoomNumbers = tenants.map { it.roomNumber }.toSet()
        
        // 验证每个合同
        contracts.forEach { (roomNumber, contract) ->
            // 检查合同对应的租户是否存在
            if (roomNumber !in tenantRoomNumbers) {
                warnings.add("合同对应的租户不存在: 房间号 $roomNumber")
            }
            
            // 验证开始日期
            if (contract.startDate.isNullOrBlank()) {
                warnings.add("合同开始日期为空 (房间号: $roomNumber)")
            } else if (!isValidDateFormat(contract.startDate)) {
                warnings.add("合同开始日期格式不正确: ${contract.startDate} (房间号: $roomNumber)")
            }
            
            // 验证结束日期
            if (contract.endDate.isNullOrBlank()) {
                warnings.add("合同结束日期为空 (房间号: $roomNumber)")
            } else if (!isValidDateFormat(contract.endDate)) {
                warnings.add("合同结束日期格式不正确: ${contract.endDate} (房间号: $roomNumber)")
            }
            
            // 验证押金
            if (contract.deposit != null && contract.deposit < 0) {
                warnings.add("合同押金为负数: ${contract.deposit} (房间号: $roomNumber)")
            }
        }
    }
    
    /**
     * 检查月份格式是否有效
     */
    private fun isValidMonthFormat(month: String): Boolean {
        // 支持的格式：yyyy-MM, yyyy/MM, yyyyMM, yyyy年MM月
        return month.matches(Regex("\\d{4}-\\d{2}")) ||
               month.matches(Regex("\\d{4}/\\d{2}")) ||
               month.matches(Regex("\\d{6}")) ||
               month.matches(Regex("\\d{4}年\\d{2}月"))
    }
    
    /**
     * 检查日期格式是否有效
     */
    private fun isValidDateFormat(date: String): Boolean {
        // 支持的格式：yyyy-MM-dd, yyyy/MM/dd, yyyyMMdd, yyyy年MM月dd日
        return date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) ||
               date.matches(Regex("\\d{4}/\\d{2}/\\d{2}")) ||
               date.matches(Regex("\\d{8}")) ||
               date.matches(Regex("\\d{4}年\\d{2}月\\d{2}日"))
    }
    
    /**
     * 验证转换后的Android数据
     * @param tenants 转换后的租户数据
     * @param bills 转换后的账单数据
     * @param price 转换后的价格数据
     * @return 验证结果
     */
    fun validateAndroidData(
        tenants: List<Tenant>,
        bills: List<BillWithDetails>,
        price: Price? = null
    ): ValidationResult {
        Log.d(tag, "开始验证转换后的Android数据")
        
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // 验证租户数据
        validateAndroidTenants(tenants, errors, warnings)
        
        // 验证账单数据
        validateAndroidBills(bills, tenants, errors, warnings)
        
        // 验证价格数据
        price?.let { validateAndroidPrice(it, errors, warnings) }
        
        val isValid = errors.isEmpty()
        Log.d(tag, "Android数据验证完成: 有效=$isValid, 错误数量=${errors.size}, 警告数量=${warnings.size}")
        
        return ValidationResult(
            isValid = isValid,
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 验证转换后的租户数据
     */
    private fun validateAndroidTenants(
        tenants: List<Tenant>,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        Log.d(tag, "验证转换后的租户数据，数量: ${tenants.size}")
        
        if (tenants.isEmpty()) {
            warnings.add("没有租户数据")
            return
        }
        
        // 检查重复的房间号
        val roomNumbers = tenants.map { it.roomNumber }
        val uniqueRoomNumbers = roomNumbers.toSet()
        if (roomNumbers.size != uniqueRoomNumbers.size) {
            val duplicates = roomNumbers.groupBy { it }.filter { it.value.size > 1 }.keys
            errors.add("存在重复的房间号: ${duplicates.joinToString(", ")}")
        }
        
        // 验证每个租户的数据
        tenants.forEach { tenant ->
            // 验证房间号
            if (tenant.roomNumber.isBlank()) {
                errors.add("租户房间号为空")
            }
            
            // 验证姓名
            if (tenant.name.isBlank()) {
                errors.add("租户姓名为空: 房间号 ${tenant.roomNumber}")
            }
            
            // 验证租金
            if (tenant.rent < 0) {
                errors.add("租户租金为负数: ${tenant.rent} (房间号: ${tenant.roomNumber})")
            }
        }
    }
    
    /**
     * 验证转换后的账单数据
     */
    private fun validateAndroidBills(
        bills: List<BillWithDetails>,
        tenants: List<Tenant>,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        Log.d(tag, "验证转换后的账单数据，数量: ${bills.size}")
        
        if (bills.isEmpty()) {
            warnings.add("没有账单数据")
            return
        }
        
        // 获取所有租户的房间号
        val tenantRoomNumbers = tenants.map { it.roomNumber }.toSet()
        
        // 验证每个账单
        bills.forEach { billWithDetails ->
            val bill = billWithDetails.bill
            
            // 验证房间号
            if (bill.tenantRoomNumber.isBlank()) {
                errors.add("账单房间号为空")
            }
            
            // 检查账单对应的租户是否存在
            if (bill.tenantRoomNumber !in tenantRoomNumbers) {
                warnings.add("账单对应的租户不存在: 房间号 ${bill.tenantRoomNumber}, 月份 ${bill.month}")
            }
            
            // 验证月份
            if (bill.month.isBlank()) {
                errors.add("账单月份为空: 房间号 ${bill.tenantRoomNumber}")
            } else if (!bill.month.matches(Regex("\\d{4}-\\d{2}"))) {
                errors.add("账单月份格式不正确: ${bill.month} (房间号: ${bill.tenantRoomNumber})")
            }
            
            // 验证总金额
            if (bill.totalAmount < 0) {
                errors.add("账单总金额为负数: ${bill.totalAmount} (房间号: ${bill.tenantRoomNumber}, 月份: ${bill.month})")
            }
            
            // 验证账单明细
            val details = billWithDetails.details
            if (details.isEmpty()) {
                warnings.add("账单没有明细: 房间号 ${bill.tenantRoomNumber}, 月份 ${bill.month}")
            } else {
                // 检查明细总金额是否与账单总金额一致
                val detailsTotal = details.sumOf { it.amount }
                if (Math.abs(detailsTotal - bill.totalAmount) > 0.01) {
                    warnings.add("账单明细总金额与账单总金额不一致: 明细总金额=${detailsTotal}, 账单总金额=${bill.totalAmount} (房间号: ${bill.tenantRoomNumber}, 月份: ${bill.month})")
                }
                
                // 验证每个明细
                details.forEach { detail ->
                    // 验证类型
                    if (detail.type.isBlank()) {
                        warnings.add("账单明细类型为空: 房间号 ${bill.tenantRoomNumber}, 月份 ${bill.month}")
                    }
                    
                    // 验证名称
                    if (detail.name.isBlank()) {
                        warnings.add("账单明细名称为空: 房间号 ${bill.tenantRoomNumber}, 月份 ${bill.month}")
                    }
                    
                    // 验证金额
                    if (detail.amount < 0) {
                        warnings.add("账单明细金额为负数: ${detail.amount} (${detail.name}, 房间号: ${bill.tenantRoomNumber}, 月份: ${bill.month})")
                    }
                    
                    // 验证表计读数
                    if (detail.type == "water" || detail.type == "electricity") {
                        // 如果有上次读数和本次读数，检查用量是否一致
                        if (detail.previousReading != null && detail.currentReading != null && detail.usage != null) {
                            val calculatedUsage = detail.currentReading - detail.previousReading
                            if (Math.abs(calculatedUsage - detail.usage) > 0.01) {
                                warnings.add("账单明细用量与读数不一致: 读数差=${calculatedUsage}, 用量=${detail.usage} (${detail.name}, 房间号: ${bill.tenantRoomNumber}, 月份: ${bill.month})")
                            }
                        }
                        
                        // 如果有用量和单价，检查金额是否一致
                        if (detail.usage != null && detail.pricePerUnit != null) {
                            val calculatedAmount = detail.usage * detail.pricePerUnit
                            if (Math.abs(calculatedAmount - detail.amount) > 0.01) {
                                warnings.add("账单明细金额与用量和单价不一致: 用量×单价=${calculatedAmount}, 金额=${detail.amount} (${detail.name}, 房间号: ${bill.tenantRoomNumber}, 月份: ${bill.month})")
                            }
                        }
                    }
                }
            }
        }
        
        // 检查是否有重复的账单（同一房间号和月份）
        val billKeys = bills.map { "${it.bill.tenantRoomNumber}:${it.bill.month}" }
        val uniqueBillKeys = billKeys.toSet()
        if (billKeys.size != uniqueBillKeys.size) {
            val duplicates = billKeys.groupBy { it }.filter { it.value.size > 1 }.keys
            warnings.add("存在重复的账单: ${duplicates.joinToString(", ")}")
        }
    }
    
    /**
     * 验证转换后的价格数据
     */
    private fun validateAndroidPrice(
        price: Price,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        Log.d(tag, "验证转换后的价格数据: 水价=${price.water}, 电价=${price.electricity}")
        
        // 验证水价
        if (price.water <= 0) {
            warnings.add("水价不是正数: ${price.water}")
        } else if (price.water > 100) {
            warnings.add("水价异常高: ${price.water}，请确认单位是否正确")
        }
        
        // 验证电价
        if (price.electricity <= 0) {
            warnings.add("电价不是正数: ${price.electricity}")
        } else if (price.electricity > 10) {
            warnings.add("电价异常高: ${price.electricity}，请确认单位是否正确")
        }
    }
}