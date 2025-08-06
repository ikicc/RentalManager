package com.morgen.rentalmanager.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.morgen.rentalmanager.myapplication.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object ImportUtils {
    private const val TAG = "ImportUtils"
    
    /**
     * 格式化月份字符串为"yyyy-MM"格式
     * 支持多种输入格式：
     * - "yyyy-MM"
     * - "yyyy/MM"
     * - "yyyy年MM月"
     * - "yyyyMM"
     */
    private fun formatMonth(month: String): String {
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
    
    /**
     * 从JSON文件导入完整数据并写入数据库（新版本）
     * 支持完整数据备份格式，包括表计名称配置和隐私关键词
     */
    suspend fun importCompleteData(context: Context, uri: Uri, repository: TenantRepository): ImportResult {
        return withContext(Dispatchers.IO) {
            val stats = ImportStats()
            val errors = mutableListOf<ImportError>()
            
            try {
                Log.d(TAG, "开始导入完整数据文件: $uri")
                
                // 读取JSON文件内容
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("无法打开文件流")
                
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonString = reader.use { it.readText() }
                
                if (jsonString.isBlank()) {
                    throw IOException("JSON文件内容为空")
                }
                
                // 解析JSON数据
                val jsonObject = JSONObject(jsonString)
                
                // 验证数据结构
                val validationResult = DataValidator.validateBackupFile(jsonObject)
                if (!validationResult.isValid) {
                    validationResult.errors.forEach { error ->
                        errors.add(ImportError(ErrorType.DATA_VALIDATION_ERROR, error))
                    }
                    return@withContext ImportResult(
                        success = false,
                        message = "数据验证失败",
                        stats = stats,
                        errors = errors
                    )
                }
                
                // 解析元数据
                val metadata = parseMetadata(jsonObject)
                Log.d(TAG, "导入数据版本: ${metadata.version}, 数据结构版本: ${metadata.dataStructureVersion}")
                
                var currentStats = stats
                
                // 导入价格数据（包括隐私关键词）
                if (jsonObject.has("prices")) {
                    val pricesResult = importPrices(jsonObject.getJSONObject("prices"), repository)
                    currentStats = currentStats.copy(pricesImported = pricesResult.stats.pricesImported)
                    errors.addAll(pricesResult.errors)
                }
                
                // 导入租户数据
                if (jsonObject.has("tenants")) {
                    val tenantsResult = importTenants(jsonObject.getJSONArray("tenants"), repository)
                    currentStats = currentStats.copy(tenantsImported = tenantsResult.stats.tenantsImported)
                    errors.addAll(tenantsResult.errors)
                }
                
                // 导入表计名称配置
                if (jsonObject.has("meterConfigs")) {
                    val configsResult = importMeterConfigs(jsonObject.getJSONArray("meterConfigs"), repository)
                    currentStats = currentStats.copy(meterConfigsImported = configsResult.stats.meterConfigsImported)
                    errors.addAll(configsResult.errors)
                }
                
                // 导入账单数据（使用新格式）
                if (jsonObject.has("bills")) {
                    val billsResult = importBills(jsonObject.getJSONObject("bills"), repository)
                    currentStats = currentStats.copy(billsImported = billsResult.stats.billsImported)
                    errors.addAll(billsResult.errors)
                }
                
                val success = errors.isEmpty() || errors.all { it.type != ErrorType.DATABASE_ERROR }
                val message = if (success) "数据导入成功" else "数据导入部分失败"
                
                Log.d(TAG, "完整数据导入完成: $message")
                
                ImportResult(
                    success = success,
                    message = message,
                    stats = currentStats.copy(errorsEncountered = errors.size),
                    errors = errors
                )
                
            } catch (e: IOException) {
                Log.e(TAG, "文件读取错误", e)
                errors.add(ImportError(ErrorType.FILE_FORMAT_ERROR, "文件读取错误: ${e.message}"))
                ImportResult(
                    success = false,
                    message = "文件读取失败",
                    stats = stats,
                    errors = errors
                )
            } catch (e: Exception) {
                Log.e(TAG, "数据导入失败", e)
                errors.add(ImportError(ErrorType.UNKNOWN_ERROR, "导入失败: ${e.message}"))
                ImportResult(
                    success = false,
                    message = "导入失败",
                    stats = stats,
                    errors = errors
                )
            }
        }
    }

    /**
     * 从JSON文件导入数据并写入数据库（兼容旧版本）
     */
    suspend fun importFromJson(context: Context, uri: Uri, repository: TenantRepository) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始导入JSON文件: $uri")
                
                // 读取JSON文件内容
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("无法打开文件流")
                
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonString = reader.use { it.readText() }
                
                if (jsonString.isBlank()) {
                    Log.e(TAG, "JSON文件内容为空")
                    throw IOException("JSON文件内容为空")
                }
                
                // 解析JSON数据
                val jsonObject = JSONObject(jsonString)
                
                // 解析价格数据
                if (jsonObject.has("prices")) {
                    val pricesObj = jsonObject.getJSONObject("prices")
                    val waterPrice = pricesObj.optDouble("waterPrice", 4.0)
                    val electricityPrice = pricesObj.optDouble("electricityPrice", 1.0)
                    
                    // 保存价格数据
                    repository.savePrice(waterPrice, electricityPrice)
                    Log.d(TAG, "价格数据导入成功: 水价=$waterPrice, 电价=$electricityPrice")
                }
                
                // 解析租户数据
                if (jsonObject.has("tenants")) {
                    val tenantsArray = jsonObject.getJSONArray("tenants")
                    val tenants = mutableListOf<Tenant>()
                    
                    for (i in 0 until tenantsArray.length()) {
                        val tenantObj = tenantsArray.getJSONObject(i)
                        val roomNumber = tenantObj.getString("room_number")
                        val name = tenantObj.getString("name")
                        val rent = tenantObj.getDouble("rent")
                        
                        val tenant = Tenant(
                            roomNumber = roomNumber.trim(),
                            name = name.trim(),
                            rent = if (rent < 0) 0.0 else rent
                        )
                        tenants.add(tenant)
                        
                        // 保存租户数据
                        repository.insert(tenant)
                    }
                    
                    Log.d(TAG, "租户数据导入成功，数量: ${tenants.size}")
                }
                
                // 解析账单数据
                if (jsonObject.has("bills")) {
                    val billsObj = jsonObject.getJSONObject("bills")
                    var billCount = 0
                    
                    // 遍历每个房间的账单
                    billsObj.keys().forEach { roomNumber ->
                        val monthlyBillsObj = billsObj.getJSONObject(roomNumber)
                        
                        // 遍历每个月份的账单
                        monthlyBillsObj.keys().forEach { originalMonth ->
                            val month = formatMonth(originalMonth)
                            val billObj = monthlyBillsObj.getJSONObject(originalMonth)
                            
                            // 创建账单明细列表
                            val details = mutableListOf<BillDetail>()
                            
                            // 处理水表数据
                            if (billObj.has("water")) {
                                val waterObj = billObj.getJSONObject("water")
                                val previous = waterObj.optDouble("previous", 0.0)
                                val current = waterObj.optDouble("current", 0.0)
                                val usage = waterObj.optDouble("usage", current - previous)
                                val amount = waterObj.optDouble("amount", 0.0)
                                
                                details.add(BillDetail(
                                    parentBillId = 0,
                                    type = "water",
                                    name = "主水表",
                                    previousReading = previous,
                                    currentReading = current,
                                    usage = usage,
                                    amount = amount
                                ))
                            }
                            
                            // 处理电表数据
                            if (billObj.has("electricity")) {
                                val electricityObj = billObj.getJSONObject("electricity")
                                val previous = electricityObj.optDouble("previous", 0.0)
                                val current = electricityObj.optDouble("current", 0.0)
                                val usage = electricityObj.optDouble("usage", current - previous)
                                val amount = electricityObj.optDouble("amount", 0.0)
                                
                                details.add(BillDetail(
                                    parentBillId = 0,
                                    type = "electricity",
                                    name = "主电表",
                                    previousReading = previous,
                                    currentReading = current,
                                    usage = usage,
                                    amount = amount
                                ))
                            }
                            
                            // 处理表计数据
                            if (billObj.has("meters")) {
                                val metersArray = billObj.getJSONArray("meters")
                                
                                for (i in 0 until metersArray.length()) {
                                    val meterObj = metersArray.getJSONObject(i)
                                    val type = meterObj.optString("type", "unknown")
                                    val name = meterObj.optString("name", "${type}表${i + 1}")
                                    val previous = meterObj.optString("previous", "0").toDoubleOrNull() ?: 0.0
                                    val current = meterObj.optString("current", "0").toDoubleOrNull() ?: 0.0
                                    val usage = meterObj.optString("usage", "${current - previous}").toDoubleOrNull() ?: (current - previous)
                                    val amount = meterObj.optString("amount", "0").toDoubleOrNull() ?: 0.0
                                    
                                    details.add(BillDetail(
                                        parentBillId = 0,
                                        type = type,
                                        name = name,
                                        previousReading = previous,
                                        currentReading = current,
                                        usage = usage,
                                        amount = amount
                                    ))
                                }
                            }
                            
                            // 处理租金
                            if (billObj.has("rent")) {
                                val rent = billObj.getDouble("rent")
                                if (rent > 0) {
                                    details.add(BillDetail(
                                        parentBillId = 0,
                                        type = "rent",
                                        name = "房租",
                                        amount = rent
                                    ))
                                }
                            }
                            
                            // 确保至少有一个明细
                            if (details.isEmpty()) {
                                details.add(BillDetail(
                                    parentBillId = 0,
                                    type = "other",
                                    name = "其他费用",
                                    amount = 0.0
                                ))
                            }
                            
                            // 计算总金额
                            val totalAmount = billObj.optDouble("total", details.sumOf { it.amount })
                            
                            // 创建账单
                            val bill = Bill(
                                tenantRoomNumber = roomNumber,
                                month = month,
                                totalAmount = totalAmount,
                                createdDate = System.currentTimeMillis()
                            )
                            
                            // 保存账单和明细
                            repository.saveBill(bill, details)
                            billCount++
                        }
                    }
                    
                    Log.d(TAG, "账单数据导入成功，数量: $billCount")
                }
                
                Log.d(TAG, "JSON导入完成")
            } catch (e: IOException) {
                Log.e(TAG, "文件读取错误", e)
                throw IOException("文件读取错误: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "JSON导入失败", e)
                throw e
            }
        }
    }
    
    /**
     * 解析元数据
     */
    private fun parseMetadata(jsonObject: JSONObject): BackupMetadata {
        val metadata = if (jsonObject.has("metadata")) {
            jsonObject.getJSONObject("metadata")
        } else {
            // 兼容旧格式，创建默认元数据
            JSONObject().apply {
                put("version", "1.0")
                put("dataStructureVersion", "1.0")
                put("exportTime", "")
                put("appVersion", "")
            }
        }
        
        val totalRecords = mutableMapOf<String, Int>()
        if (metadata.has("totalRecords")) {
            val totalRecordsObj = metadata.getJSONObject("totalRecords")
            totalRecordsObj.keys().forEach { key ->
                totalRecords[key] = totalRecordsObj.getInt(key)
            }
        }
        
        return BackupMetadata(
            version = metadata.optString("version", "1.0"),
            exportTime = metadata.optString("exportTime", ""),
            appVersion = metadata.optString("appVersion", ""),
            dataStructureVersion = metadata.optString("dataStructureVersion", "1.0"),
            totalRecords = totalRecords
        )
    }
    
    /**
     * 导入租户数据
     */
    private suspend fun importTenants(tenantsArray: JSONArray, repository: TenantRepository): ImportResultWithErrors {
        var tenantsImported = 0
        val errors = mutableListOf<ImportError>()
        
        try {
            for (i in 0 until tenantsArray.length()) {
                val tenantObj = tenantsArray.getJSONObject(i)
                
                // 验证租户数据
                val validationResult = DataValidator.validateTenantData(tenantObj)
                if (!validationResult.isValid) {
                    validationResult.errors.forEach { error ->
                        errors.add(ImportError(ErrorType.DATA_VALIDATION_ERROR, "租户数据验证失败: $error"))
                    }
                    continue
                }
                
                try {
                    // 支持新旧格式的字段名
                    val roomNumber = tenantObj.optString("roomNumber") 
                        .ifBlank { tenantObj.optString("room_number", "") }
                    val name = tenantObj.getString("name")
                    val rent = tenantObj.getDouble("rent")
                    
                    val tenant = Tenant(
                        roomNumber = roomNumber.trim(),
                        name = name.trim(),
                        rent = if (rent < 0) 0.0 else rent
                    )
                    
                    // 保存租户数据
                    repository.insert(tenant)
                    tenantsImported++
                    
                } catch (e: Exception) {
                    Log.e(TAG, "导入租户数据失败", e)
                    errors.add(ImportError(ErrorType.DATABASE_ERROR, "保存租户数据失败: ${e.message}"))
                }
            }
            
            Log.d(TAG, "租户数据导入成功，数量: $tenantsImported")
            
        } catch (e: Exception) {
            Log.e(TAG, "处理租户数据时发生错误", e)
            errors.add(ImportError(ErrorType.UNKNOWN_ERROR, "处理租户数据失败: ${e.message}"))
        }
        
        return ImportResultWithErrors(
            ImportStats(tenantsImported = tenantsImported, errorsEncountered = errors.size),
            errors
        )
    }
    
    /**
     * 导入价格数据（包括隐私关键词）
     */
    private suspend fun importPrices(pricesObj: JSONObject, repository: TenantRepository): ImportResultWithErrors {
        var pricesImported = 0
        val errors = mutableListOf<ImportError>()
        
        try {
            // 验证价格数据
            val validationResult = DataValidator.validatePriceData(pricesObj)
            if (!validationResult.isValid) {
                validationResult.errors.forEach { error ->
                    errors.add(ImportError(ErrorType.DATA_VALIDATION_ERROR, "价格数据验证失败: $error"))
                }
                return ImportResultWithErrors(
                    ImportStats(errorsEncountered = errors.size),
                    errors
                )
            }
            
            val waterPrice = pricesObj.optDouble("waterPrice", 4.0)
            val electricityPrice = pricesObj.optDouble("electricityPrice", 1.0)
            
            // 保存价格数据
            repository.savePrice(waterPrice, electricityPrice)
            pricesImported++
            
            // 处理隐私关键词
            if (pricesObj.has("privacyKeywords")) {
                try {
                    val keywords = mutableListOf<String>()
                    val keywordsData = pricesObj.get("privacyKeywords")
                    
                    when (keywordsData) {
                        is JSONArray -> {
                            // 新格式：直接是JSON数组
                            for (i in 0 until keywordsData.length()) {
                                val keyword = keywordsData.getString(i).trim()
                                if (keyword.isNotBlank()) {
                                    keywords.add(keyword)
                                }
                            }
                        }
                        is String -> {
                            // 兼容旧格式：JSON字符串
                            if (keywordsData.isNotBlank()) {
                                val keywordArray = JSONArray(keywordsData)
                                for (i in 0 until keywordArray.length()) {
                                    val keyword = keywordArray.getString(i).trim()
                                    if (keyword.isNotBlank()) {
                                        keywords.add(keyword)
                                    }
                                }
                            }
                        }
                    }
                    
                    // 保存隐私关键词
                    repository.savePrivacyKeywords(keywords)
                    Log.d(TAG, "隐私关键词导入成功，数量: ${keywords.size}")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "导入隐私关键词失败", e)
                    errors.add(ImportError(ErrorType.DATA_VALIDATION_ERROR, "隐私关键词导入失败: ${e.message}"))
                }
            }
            
            Log.d(TAG, "价格数据导入成功: 水价=$waterPrice, 电价=$electricityPrice")
            
        } catch (e: Exception) {
            Log.e(TAG, "导入价格数据失败", e)
            errors.add(ImportError(ErrorType.DATABASE_ERROR, "保存价格数据失败: ${e.message}"))
        }
        
        return ImportResultWithErrors(
            ImportStats(pricesImported = pricesImported, errorsEncountered = errors.size),
            errors
        )
    }
    
    /**
     * 导入表计名称配置
     */
    private suspend fun importMeterConfigs(configsArray: JSONArray, repository: TenantRepository): ImportResultWithErrors {
        var meterConfigsImported = 0
        val errors = mutableListOf<ImportError>()
        
        try {
            for (i in 0 until configsArray.length()) {
                val configObj = configsArray.getJSONObject(i)
                
                // 验证表计配置数据
                val validationResult = DataValidator.validateMeterConfig(configObj)
                if (!validationResult.isValid) {
                    validationResult.errors.forEach { error ->
                        errors.add(ImportError(ErrorType.DATA_VALIDATION_ERROR, "表计配置验证失败: $error"))
                    }
                    continue
                }
                
                try {
                    val meterType = configObj.getString("meterType")
                    val defaultName = configObj.getString("defaultName")
                    val customName = configObj.getString("customName")
                    val tenantRoomNumber = configObj.optString("tenantRoomNumber", "")
                    
                    // 使用repository的表计名称管理方法
                    val result = repository.saveMeterCustomName(
                        defaultName = defaultName,
                        customName = customName,
                        meterType = meterType,
                        tenantRoomNumber = tenantRoomNumber
                    )
                    
                    if (result.isSuccess) {
                        meterConfigsImported++
                    } else {
                        errors.add(ImportError(
                            ErrorType.DATABASE_ERROR, 
                            "保存表计配置失败: ${result.exceptionOrNull()?.message}"
                        ))
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "导入表计配置失败", e)
                    errors.add(ImportError(ErrorType.DATABASE_ERROR, "保存表计配置失败: ${e.message}"))
                }
            }
            
            Log.d(TAG, "表计配置导入成功，数量: $meterConfigsImported")
            
        } catch (e: Exception) {
            Log.e(TAG, "处理表计配置时发生错误", e)
            errors.add(ImportError(ErrorType.UNKNOWN_ERROR, "处理表计配置失败: ${e.message}"))
        }
        
        return ImportResultWithErrors(
            ImportStats(meterConfigsImported = meterConfigsImported, errorsEncountered = errors.size),
            errors
        )
    }
    
    /**
     * 导入账单数据（支持新格式）
     */
    private suspend fun importBills(billsObj: JSONObject, repository: TenantRepository): ImportResultWithErrors {
        var billsImported = 0
        val errors = mutableListOf<ImportError>()
        
        try {
            // 遍历每个房间的账单
            billsObj.keys().forEach { roomNumber ->
                val monthlyBillsObj = billsObj.getJSONObject(roomNumber)
                
                // 遍历每个月份的账单
                monthlyBillsObj.keys().forEach { originalMonth ->
                    try {
                        val month = formatMonth(originalMonth)
                        val billObj = monthlyBillsObj.getJSONObject(originalMonth)
                        
                        // 验证账单数据
                        val validationResult = DataValidator.validateBillData(billObj)
                        if (!validationResult.isValid) {
                            validationResult.errors.forEach { error ->
                                errors.add(ImportError(ErrorType.DATA_VALIDATION_ERROR, "账单数据验证失败: $error"))
                            }
                            return@forEach
                        }
                        
                        // 创建账单明细列表
                        val details = mutableListOf<BillDetail>()
                        
                        // 处理水表数据
                        if (billObj.has("water")) {
                            val waterObj = billObj.getJSONObject("water")
                            val previous = waterObj.optDouble("previous", 0.0)
                            val current = waterObj.optDouble("current", 0.0)
                            val usage = waterObj.optDouble("usage", current - previous)
                            val amount = waterObj.optDouble("amount", 0.0)
                            val pricePerUnit = waterObj.optDouble("pricePerUnit", 0.0)
                            
                            details.add(BillDetail(
                                parentBillId = 0,
                                type = "water",
                                name = "主水表",
                                previousReading = previous,
                                currentReading = current,
                                usage = usage,
                                pricePerUnit = if (pricePerUnit > 0) pricePerUnit else null,
                                amount = amount
                            ))
                        }
                        
                        // 处理电表数据
                        if (billObj.has("electricity")) {
                            val electricityObj = billObj.getJSONObject("electricity")
                            val previous = electricityObj.optDouble("previous", 0.0)
                            val current = electricityObj.optDouble("current", 0.0)
                            val usage = electricityObj.optDouble("usage", current - previous)
                            val amount = electricityObj.optDouble("amount", 0.0)
                            val pricePerUnit = electricityObj.optDouble("pricePerUnit", 0.0)
                            
                            details.add(BillDetail(
                                parentBillId = 0,
                                type = "electricity",
                                name = "主电表",
                                previousReading = previous,
                                currentReading = current,
                                usage = usage,
                                pricePerUnit = if (pricePerUnit > 0) pricePerUnit else null,
                                amount = amount
                            ))
                        }
                        
                        // 处理额外表计数据（新格式）
                        if (billObj.has("extraMeters")) {
                            val extraMeters = billObj.getJSONArray("extraMeters")
                            for (i in 0 until extraMeters.length()) {
                                val meterObj = extraMeters.getJSONObject(i)
                                val type = meterObj.optString("type", "extra")
                                val name = meterObj.optString("name", "${type}表${i + 1}")
                                val previous = meterObj.optDouble("previous", 0.0)
                                val current = meterObj.optDouble("current", 0.0)
                                val usage = meterObj.optDouble("usage", current - previous)
                                val amount = meterObj.optDouble("amount", 0.0)
                                val pricePerUnit = meterObj.optDouble("pricePerUnit", 0.0)
                                
                                details.add(BillDetail(
                                    parentBillId = 0,
                                    type = type,
                                    name = name,
                                    previousReading = previous,
                                    currentReading = current,
                                    usage = usage,
                                    pricePerUnit = if (pricePerUnit > 0) pricePerUnit else null,
                                    amount = amount
                                ))
                            }
                        }
                        
                        // 处理额外费用（新格式）
                        if (billObj.has("extraFees")) {
                            val extraFees = billObj.getJSONArray("extraFees")
                            for (i in 0 until extraFees.length()) {
                                val feeObj = extraFees.getJSONObject(i)
                                val type = feeObj.optString("type", "extra")
                                val name = feeObj.optString("name", "额外费用${i + 1}")
                                val amount = feeObj.optDouble("amount", 0.0)
                                
                                details.add(BillDetail(
                                    parentBillId = 0,
                                    type = type,
                                    name = name,
                                    amount = amount
                                ))
                            }
                        }
                        
                        // 兼容旧格式的表计数据
                        if (billObj.has("meters")) {
                            val metersArray = billObj.getJSONArray("meters")
                            for (i in 0 until metersArray.length()) {
                                val meterObj = metersArray.getJSONObject(i)
                                val type = meterObj.optString("type", "unknown")
                                val name = meterObj.optString("name", "${type}表${i + 1}")
                                val previous = meterObj.optString("previous", "0").toDoubleOrNull() ?: 0.0
                                val current = meterObj.optString("current", "0").toDoubleOrNull() ?: 0.0
                                val usage = meterObj.optString("usage", "${current - previous}").toDoubleOrNull() ?: (current - previous)
                                val amount = meterObj.optString("amount", "0").toDoubleOrNull() ?: 0.0
                                
                                details.add(BillDetail(
                                    parentBillId = 0,
                                    type = type,
                                    name = name,
                                    previousReading = previous,
                                    currentReading = current,
                                    usage = usage,
                                    amount = amount
                                ))
                            }
                        }
                        
                        // 处理租金
                        if (billObj.has("rent")) {
                            val rent = billObj.getDouble("rent")
                            if (rent > 0) {
                                details.add(BillDetail(
                                    parentBillId = 0,
                                    type = "rent",
                                    name = "房租",
                                    amount = rent
                                ))
                            }
                        }
                        
                        // 确保至少有一个明细
                        if (details.isEmpty()) {
                            details.add(BillDetail(
                                parentBillId = 0,
                                type = "other",
                                name = "其他费用",
                                amount = 0.0
                            ))
                        }
                        
                        // 计算总金额
                        val totalAmount = billObj.optDouble("totalAmount", 
                            billObj.optDouble("total", details.sumOf { it.amount }))
                        
                        // 获取创建时间
                        val createdDate = billObj.optLong("createdDate", System.currentTimeMillis())
                        
                        // 创建账单
                        val bill = Bill(
                            tenantRoomNumber = roomNumber,
                            month = month,
                            totalAmount = totalAmount,
                            createdDate = createdDate
                        )
                        
                        // 保存账单和明细
                        repository.saveBill(bill, details)
                        billsImported++
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "导入账单失败: $roomNumber - $originalMonth", e)
                        errors.add(ImportError(ErrorType.DATABASE_ERROR, "保存账单失败: ${e.message}"))
                    }
                }
            }
            
            Log.d(TAG, "账单数据导入成功，数量: $billsImported")
            
        } catch (e: Exception) {
            Log.e(TAG, "处理账单数据时发生错误", e)
            errors.add(ImportError(ErrorType.UNKNOWN_ERROR, "处理账单数据失败: ${e.message}"))
        }
        
        return ImportResultWithErrors(
            ImportStats(billsImported = billsImported, errorsEncountered = errors.size),
            errors
        )
    }
}