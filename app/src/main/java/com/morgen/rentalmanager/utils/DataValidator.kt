package com.morgen.rentalmanager.utils

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * 数据验证器
 * 负责验证导入数据的格式和完整性
 */
object DataValidator {
    private const val TAG = "DataValidator"
    
    /**
     * 验证备份文件的整体结构
     */
    fun validateBackupFile(jsonObject: JSONObject): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            // 检查必需的顶级字段
            val requiredFields = listOf("metadata", "tenants", "bills", "prices")
            requiredFields.forEach { field ->
                if (!jsonObject.has(field)) {
                    errors.add("缺少必需字段: $field")
                }
            }
            
            // 验证元数据
            if (jsonObject.has("metadata")) {
                val metadataResult = validateMetadata(jsonObject.getJSONObject("metadata"))
                errors.addAll(metadataResult.errors)
                warnings.addAll(metadataResult.warnings)
            }
            
            // 验证数据结构版本
            if (jsonObject.has("metadata")) {
                val metadata = jsonObject.getJSONObject("metadata")
                if (metadata.has("dataStructureVersion")) {
                    val version = metadata.getString("dataStructureVersion")
                    if (version != "2.0" && version != "1.0") {
                        warnings.add("不支持的数据结构版本: $version，将尝试兼容处理")
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "验证备份文件时发生错误", e)
            errors.add("文件格式错误: ${e.message}")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 验证元数据
     */
    private fun validateMetadata(metadata: JSONObject): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            // 检查必需字段
            val requiredFields = listOf("version", "exportTime", "dataStructureVersion")
            requiredFields.forEach { field ->
                if (!metadata.has(field)) {
                    errors.add("元数据缺少必需字段: $field")
                }
            }
            
            // 验证版本格式
            if (metadata.has("version")) {
                val version = metadata.getString("version")
                if (!version.matches(Regex("\\d+\\.\\d+"))) {
                    warnings.add("版本格式不标准: $version")
                }
            }
            
        } catch (e: Exception) {
            errors.add("元数据格式错误: ${e.message}")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 验证租户数据
     */
    fun validateTenantData(tenant: JSONObject): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            // 检查必需字段
            if (!tenant.has("roomNumber") || tenant.getString("roomNumber").isBlank()) {
                errors.add("租户房间号不能为空")
            }
            
            if (!tenant.has("name") || tenant.getString("name").isBlank()) {
                errors.add("租户姓名不能为空")
            }
            
            if (!tenant.has("rent")) {
                errors.add("租户租金不能为空")
            } else {
                val rent = tenant.getDouble("rent")
                if (rent < 0) {
                    warnings.add("租金为负数，将自动调整为0")
                }
            }
            
        } catch (e: Exception) {
            errors.add("租户数据格式错误: ${e.message}")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 验证账单数据
     */
    fun validateBillData(bill: JSONObject): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            // 检查必需字段
            if (!bill.has("month") || bill.getString("month").isBlank()) {
                errors.add("账单月份不能为空")
            } else {
                val month = bill.getString("month")
                if (!month.matches(Regex("\\d{4}-\\d{2}"))) {
                    errors.add("账单月份格式错误，应为YYYY-MM格式")
                }
            }
            
            if (!bill.has("totalAmount")) {
                errors.add("账单总金额不能为空")
            } else {
                val totalAmount = bill.getDouble("totalAmount")
                if (totalAmount < 0) {
                    warnings.add("账单总金额为负数")
                }
            }
            
            // 验证水表数据
            if (bill.has("water")) {
                val waterResult = validateMeterData(bill.getJSONObject("water"), "水表")
                errors.addAll(waterResult.errors)
                warnings.addAll(waterResult.warnings)
            }
            
            // 验证电表数据
            if (bill.has("electricity")) {
                val electricityResult = validateMeterData(bill.getJSONObject("electricity"), "电表")
                errors.addAll(electricityResult.errors)
                warnings.addAll(electricityResult.warnings)
            }
            
            // 验证额外表计数据
            if (bill.has("extraMeters")) {
                val extraMeters = bill.getJSONArray("extraMeters")
                for (i in 0 until extraMeters.length()) {
                    val meterResult = validateExtraMeterData(extraMeters.getJSONObject(i))
                    errors.addAll(meterResult.errors)
                    warnings.addAll(meterResult.warnings)
                }
            }
            
        } catch (e: Exception) {
            errors.add("账单数据格式错误: ${e.message}")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 验证表计数据
     */
    private fun validateMeterData(meter: JSONObject, meterType: String): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            val previous = meter.optDouble("previous", 0.0)
            val current = meter.optDouble("current", 0.0)
            val usage = meter.optDouble("usage", current - previous)
            
            if (current < previous) {
                warnings.add("${meterType}当前读数小于上次读数")
            }
            
            if (usage < 0) {
                warnings.add("${meterType}用量为负数")
            }
            
            if (meter.has("amount")) {
                val amount = meter.getDouble("amount")
                if (amount < 0) {
                    warnings.add("${meterType}费用为负数")
                }
            }
            
        } catch (e: Exception) {
            errors.add("${meterType}数据格式错误: ${e.message}")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 验证额外表计数据
     */
    private fun validateExtraMeterData(meter: JSONObject): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            if (!meter.has("type") || meter.getString("type").isBlank()) {
                errors.add("额外表计类型不能为空")
            }
            
            if (!meter.has("name") || meter.getString("name").isBlank()) {
                errors.add("额外表计名称不能为空")
            }
            
            val meterResult = validateMeterData(meter, "额外表计")
            errors.addAll(meterResult.errors)
            warnings.addAll(meterResult.warnings)
            
        } catch (e: Exception) {
            errors.add("额外表计数据格式错误: ${e.message}")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 验证价格数据
     */
    fun validatePriceData(price: JSONObject): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            if (!price.has("waterPrice")) {
                errors.add("水价不能为空")
            } else {
                val waterPrice = price.getDouble("waterPrice")
                if (waterPrice <= 0) {
                    warnings.add("水价应大于0")
                }
            }
            
            if (!price.has("electricityPrice")) {
                errors.add("电价不能为空")
            } else {
                val electricityPrice = price.getDouble("electricityPrice")
                if (electricityPrice <= 0) {
                    warnings.add("电价应大于0")
                }
            }
            
            // 验证隐私关键词
            if (price.has("privacyKeywords")) {
                try {
                    val keywords = price.get("privacyKeywords")
                    if (keywords is JSONArray) {
                        // 新格式：JSON数组
                        for (i in 0 until keywords.length()) {
                            val keyword = keywords.getString(i)
                            if (keyword.isBlank()) {
                                warnings.add("发现空的隐私关键词")
                            }
                        }
                    } else if (keywords is String) {
                        // 兼容旧格式：JSON字符串
                        val keywordArray = JSONArray(keywords as String)
                        for (i in 0 until keywordArray.length()) {
                            val keyword = keywordArray.getString(i)
                            if (keyword.isBlank()) {
                                warnings.add("发现空的隐私关键词")
                            }
                        }
                    }
                } catch (e: Exception) {
                    warnings.add("隐私关键词格式错误: ${e.message}")
                }
            }
            
        } catch (e: Exception) {
            errors.add("价格数据格式错误: ${e.message}")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 验证表计配置数据
     */
    fun validateMeterConfig(config: JSONObject): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            if (!config.has("meterType") || config.getString("meterType").isBlank()) {
                errors.add("表计类型不能为空")
            } else {
                val meterType = config.getString("meterType")
                if (meterType != "water" && meterType != "electricity") {
                    warnings.add("未知的表计类型: $meterType")
                }
            }
            
            if (!config.has("defaultName") || config.getString("defaultName").isBlank()) {
                errors.add("默认名称不能为空")
            }
            
            if (!config.has("customName") || config.getString("customName").isBlank()) {
                warnings.add("自定义名称为空")
            }
            
        } catch (e: Exception) {
            errors.add("表计配置数据格式错误: ${e.message}")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * 检查数据完整性
     */
    fun checkDataIntegrity(data: JSONObject): IntegrityResult {
        val missingReferences = mutableListOf<String>()
        val orphanedRecords = mutableListOf<String>()
        
        try {
            // 收集所有租户房间号
            val tenantRoomNumbers = mutableSetOf<String>()
            if (data.has("tenants")) {
                val tenants = data.getJSONArray("tenants")
                for (i in 0 until tenants.length()) {
                    val tenant = tenants.getJSONObject(i)
                    if (tenant.has("roomNumber")) {
                        tenantRoomNumbers.add(tenant.getString("roomNumber"))
                    }
                }
            }
            
            // 检查账单数据的租户引用
            if (data.has("bills")) {
                val bills = data.getJSONObject("bills")
                bills.keys().forEach { roomNumber ->
                    if (!tenantRoomNumbers.contains(roomNumber)) {
                        orphanedRecords.add("账单数据中的房间号 $roomNumber 没有对应的租户记录")
                    }
                }
            }
            
            // 检查表计配置的租户引用
            if (data.has("meterConfigs")) {
                val configs = data.getJSONArray("meterConfigs")
                for (i in 0 until configs.length()) {
                    val config = configs.getJSONObject(i)
                    if (config.has("tenantRoomNumber")) {
                        val roomNumber = config.getString("tenantRoomNumber")
                        if (roomNumber.isNotBlank() && !tenantRoomNumbers.contains(roomNumber)) {
                            orphanedRecords.add("表计配置中的房间号 $roomNumber 没有对应的租户记录")
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "检查数据完整性时发生错误", e)
            missingReferences.add("数据完整性检查失败: ${e.message}")
        }
        
        return IntegrityResult(
            isIntact = missingReferences.isEmpty() && orphanedRecords.isEmpty(),
            missingReferences = missingReferences,
            orphanedRecords = orphanedRecords
        )
    }
}