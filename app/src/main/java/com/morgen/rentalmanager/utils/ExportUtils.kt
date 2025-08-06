package com.morgen.rentalmanager.utils

import android.content.Context
import com.morgen.rentalmanager.myapplication.TenantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 完整数据导出工具类
 * 实现应用的完整数据备份功能，包括所有数据类型的导出
 */
object ExportUtils {
    
    private const val DATA_STRUCTURE_VERSION = "2.0"
    private const val APP_VERSION = "1.0.0"
    
    /**
     * 导出完整数据
     * 包括租户、账单、价格配置、表计名称配置等所有数据
     */
    suspend fun exportCompleteData(context: Context, repository: TenantRepository): String = withContext(Dispatchers.IO) {
        try {
            // 创建根JSON对象
            val jsonObject = JSONObject()
            
            // 添加元数据
            jsonObject.put("metadata", exportMetadata(repository))
            
            // 导出各类数据
            jsonObject.put("tenants", exportTenants(repository))
            jsonObject.put("bills", exportBills(repository))
            jsonObject.put("prices", exportPrices(repository))
            jsonObject.put("meterConfigs", exportMeterConfigs(repository))
            
            // 生成文件名并保存
            val fileName = generateFileName()
            return@withContext saveToFile(context, jsonObject, fileName)
            
        } catch (e: Exception) {
            throw Exception("导出完整数据失败: ${e.message}")
        }
    }
    
    /**
     * 导出元数据信息
     */
    private suspend fun exportMetadata(repository: TenantRepository): JSONObject {
        val metadata = JSONObject()
        
        // 基本信息
        metadata.put("version", DATA_STRUCTURE_VERSION)
        metadata.put("exportTime", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date()))
        metadata.put("appVersion", APP_VERSION)
        metadata.put("dataStructureVersion", DATA_STRUCTURE_VERSION)
        
        // 统计信息
        val totalRecords = JSONObject()
        val tenants = repository.allTenants.first()
        val bills = repository.getAllBillsWithDetails().first()
        val meterConfigs = repository.getAllMeterNameConfigs()
        
        totalRecords.put("tenants", tenants.size)
        totalRecords.put("bills", bills.size)
        totalRecords.put("meterConfigs", meterConfigs.size)
        
        metadata.put("totalRecords", totalRecords)
        
        return metadata
    }
    
    /**
     * 导出租户数据
     */
    suspend fun exportTenants(repository: TenantRepository): JSONArray {
        val tenantsArray = JSONArray()
        val tenants = repository.allTenants.first()
        
        for (tenant in tenants) {
            val tenantObj = JSONObject()
            tenantObj.put("roomNumber", tenant.roomNumber)
            tenantObj.put("name", tenant.name)
            tenantObj.put("rent", tenant.rent)
            tenantsArray.put(tenantObj)
        }
        
        return tenantsArray
    }
    
    /**
     * 导出账单数据
     * 按照设计文档的格式组织数据结构
     */
    suspend fun exportBills(repository: TenantRepository): JSONObject {
        val billsObj = JSONObject()
        val bills = repository.getAllBillsWithDetails().first()
        
        // 按房间号分组账单
        val billsByRoom = bills.groupBy { it.bill.tenantRoomNumber }
        
        for ((roomNumber, roomBills) in billsByRoom) {
            val roomBillsObj = JSONObject()
            
            // 按月份组织账单
            for (billWithDetails in roomBills) {
                val bill = billWithDetails.bill
                val details = billWithDetails.details
                
                val billObj = JSONObject()
                billObj.put("billId", bill.billId)
                billObj.put("month", bill.month)
                billObj.put("totalAmount", bill.totalAmount)
                billObj.put("createdDate", bill.createdDate)
                
                // 处理水表数据
                val waterDetails = details.filter { it.type == "water" }
                if (waterDetails.isNotEmpty()) {
                    // 主水表
                    val mainWater = waterDetails.firstOrNull { it.name.contains("主") }
                    if (mainWater != null) {
                        val waterObj = JSONObject()
                        waterObj.put("previous", mainWater.previousReading ?: 0.0)
                        waterObj.put("current", mainWater.currentReading ?: 0.0)
                        waterObj.put("usage", mainWater.usage ?: 0.0)
                        waterObj.put("pricePerUnit", mainWater.pricePerUnit ?: 0.0)
                        waterObj.put("amount", mainWater.amount)
                        billObj.put("water", waterObj)
                    }
                    
                    // 额外水表
                    val extraWaterMeters = waterDetails.filter { !it.name.contains("主") }
                    if (extraWaterMeters.isNotEmpty()) {
                        val extraMetersArray = JSONArray()
                        for (meter in extraWaterMeters) {
                            val meterObj = JSONObject()
                            meterObj.put("detailId", meter.detailId)
                            meterObj.put("type", "water")
                            
                            // 获取当前的自定义表计名称
                            val currentName = repository.getMeterDisplayName(meter.name, roomNumber)
                            meterObj.put("name", currentName)
                            
                            meterObj.put("previous", meter.previousReading ?: 0.0)
                            meterObj.put("current", meter.currentReading ?: 0.0)
                            meterObj.put("usage", meter.usage ?: 0.0)
                            meterObj.put("pricePerUnit", meter.pricePerUnit ?: 0.0)
                            meterObj.put("amount", meter.amount)
                            extraMetersArray.put(meterObj)
                        }
                        
                        if (!billObj.has("extraMeters")) {
                            billObj.put("extraMeters", JSONArray())
                        }
                        val extraMeters = billObj.getJSONArray("extraMeters")
                        for (i in 0 until extraMetersArray.length()) {
                            extraMeters.put(extraMetersArray.get(i))
                        }
                    }
                }
                
                // 处理电表数据
                val elecDetails = details.filter { it.type == "electricity" }
                if (elecDetails.isNotEmpty()) {
                    // 主电表
                    val mainElec = elecDetails.firstOrNull { it.name.contains("主") }
                    if (mainElec != null) {
                        val elecObj = JSONObject()
                        elecObj.put("previous", mainElec.previousReading ?: 0.0)
                        elecObj.put("current", mainElec.currentReading ?: 0.0)
                        elecObj.put("usage", mainElec.usage ?: 0.0)
                        elecObj.put("pricePerUnit", mainElec.pricePerUnit ?: 0.0)
                        elecObj.put("amount", mainElec.amount)
                        billObj.put("electricity", elecObj)
                    }
                    
                    // 额外电表
                    val extraElecMeters = elecDetails.filter { !it.name.contains("主") }
                    if (extraElecMeters.isNotEmpty()) {
                        val extraMetersArray = JSONArray()
                        for (meter in extraElecMeters) {
                            val meterObj = JSONObject()
                            meterObj.put("detailId", meter.detailId)
                            meterObj.put("type", "electricity")
                            
                            // 获取当前的自定义表计名称
                            val currentName = repository.getMeterDisplayName(meter.name, roomNumber)
                            meterObj.put("name", currentName)
                            
                            meterObj.put("previous", meter.previousReading ?: 0.0)
                            meterObj.put("current", meter.currentReading ?: 0.0)
                            meterObj.put("usage", meter.usage ?: 0.0)
                            meterObj.put("pricePerUnit", meter.pricePerUnit ?: 0.0)
                            meterObj.put("amount", meter.amount)
                            extraMetersArray.put(meterObj)
                        }
                        
                        if (!billObj.has("extraMeters")) {
                            billObj.put("extraMeters", JSONArray())
                        }
                        val extraMeters = billObj.getJSONArray("extraMeters")
                        for (i in 0 until extraMetersArray.length()) {
                            extraMeters.put(extraMetersArray.get(i))
                        }
                    }
                }
                
                // 处理额外费用
                val extraFees = details.filter { it.type == "extra" }
                if (extraFees.isNotEmpty()) {
                    val feesArray = JSONArray()
                    for (fee in extraFees) {
                        val feeObj = JSONObject()
                        feeObj.put("detailId", fee.detailId)
                        feeObj.put("type", "extra")
                        feeObj.put("name", fee.name)
                        feeObj.put("amount", fee.amount)
                        feesArray.put(feeObj)
                    }
                    billObj.put("extraFees", feesArray)
                }
                
                // 处理租金
                val rentDetails = details.filter { it.type == "rent" }
                if (rentDetails.isNotEmpty()) {
                    val rentDetail = rentDetails.first()
                    billObj.put("rent", rentDetail.amount)
                }
                
                // 将账单添加到房间的月份集合中
                roomBillsObj.put(bill.month, billObj)
            }
            
            // 将房间的账单集合添加到总账单对象中
            billsObj.put(roomNumber, roomBillsObj)
        }
        
        return billsObj
    }
    
    /**
     * 导出价格配置数据
     */
    suspend fun exportPrices(repository: TenantRepository): JSONObject {
        val pricesObj = JSONObject()
        val price = repository.getCurrentPrice()
        
        if (price != null) {
            pricesObj.put("id", price.id)
            pricesObj.put("waterPrice", price.water)
            pricesObj.put("electricityPrice", price.electricity)
            
            // 导出隐私关键词
            try {
                val privacyKeywords = repository.getPrivacyKeywords()
                val keywordsArray = JSONArray()
                privacyKeywords.forEach { keyword ->
                    if (keyword.isNotBlank()) {
                        keywordsArray.put(keyword)
                    }
                }
                pricesObj.put("privacyKeywords", keywordsArray)
                
                // 添加调试日志
                android.util.Log.d("ExportUtils", "导出隐私关键词: $privacyKeywords")
                
            } catch (e: Exception) {
                android.util.Log.e("ExportUtils", "导出隐私关键词失败", e)
                // 如果解析失败，使用空数组
                pricesObj.put("privacyKeywords", JSONArray())
            }
        }
        
        return pricesObj
    }
    
    /**
     * 导出表计名称配置数据
     */
    suspend fun exportMeterConfigs(repository: TenantRepository): JSONArray {
        val configsArray = JSONArray()
        val configs = repository.getAllMeterNameConfigs()
        
        for (config in configs) {
            val configObj = JSONObject()
            configObj.put("id", config.id)
            configObj.put("meterType", config.meterType)
            configObj.put("defaultName", config.defaultName)
            configObj.put("customName", config.customName)
            configObj.put("tenantRoomNumber", config.tenantRoomNumber)
            configObj.put("isActive", config.isActive)
            configObj.put("createdDate", config.createdDate)
            configObj.put("updatedDate", config.updatedDate)
            configsArray.put(configObj)
        }
        
        return configsArray
    }
    
    /**
     * 生成导出文件名
     */
    private fun generateFileName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        return "complete_backup_${dateFormat.format(Date())}.json"
    }
    
    /**
     * 保存数据到文件
     */
    private fun saveToFile(context: Context, data: JSONObject, fileName: String): String {
        val file = File(context.getExternalFilesDir(null), fileName)
        FileOutputStream(file).use { 
            it.write(data.toString(2).toByteArray())
        }
        return file.absolutePath
    }
}