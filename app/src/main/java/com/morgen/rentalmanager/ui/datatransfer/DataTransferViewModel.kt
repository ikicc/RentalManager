package com.morgen.rentalmanager.ui.datatransfer

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.morgen.rentalmanager.myapplication.Bill
import com.morgen.rentalmanager.myapplication.BillDetail
import com.morgen.rentalmanager.myapplication.Tenant
import com.morgen.rentalmanager.myapplication.TenantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import android.util.Log
import java.util.Iterator
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat

sealed class DataTransferStatus {
    object Idle : DataTransferStatus()
    data class Success(
        val message: String, 
        val isExport: Boolean,
        val filePath: String? = null
    ) : DataTransferStatus()
    data class Error(val message: String) : DataTransferStatus()
}

class DataTransferViewModel(
    private val repository: TenantRepository
) : ViewModel() {

    var operationStatus by mutableStateOf<DataTransferStatus>(DataTransferStatus.Idle)
        private set
        
    // 添加是否为导出操作的标志
    var isExportOperation by mutableStateOf(false)
        private set
        
    // 添加备份文件路径
    var backupFilePath by mutableStateOf<String?>(null)
        private set
        
    // 添加备份文件URI
    var backupUri by mutableStateOf<Uri?>(null)
        private set
        
    // 添加备份文件名
    var backupFileName by mutableStateOf<String?>(null)
        private set

    fun exportData(context: Context, uri: Uri) {
        isExportOperation = true
        backupUri = uri  // 保存URI以便之后分享使用
        
        // 保存文件名
        val fileName = getFileName(context, uri)
        backupFileName = fileName
        
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 获取所有租户
                    val tenants = repository.allTenants.first()
                    val tenantsArray = JSONArray()
                    tenants.forEach { tenant ->
                        val tenantJson = JSONObject().apply {
                            put("roomNumber", tenant.roomNumber)
                            put("name", tenant.name)
                            put("rent", tenant.rent)
                        }
                        tenantsArray.put(tenantJson)
                    }
                    
                    // 获取所有账单
                    val billsWithDetails = repository.getAllBillsWithDetails().first()
                    val billsArray = JSONArray()
                    billsWithDetails.forEach { billWithDetails ->
                        val billJson = JSONObject().apply {
                            put("billId", billWithDetails.bill.billId)
                            put("tenantRoomNumber", billWithDetails.bill.tenantRoomNumber)
                            put("month", billWithDetails.bill.month)
                            put("totalAmount", billWithDetails.bill.totalAmount)
                            put("createdDate", billWithDetails.bill.createdDate)
                            
                            // 账单明细
                            val detailsArray = JSONArray()
                            billWithDetails.details.forEach { detail ->
                                val detailJson = JSONObject().apply {
                                    put("detailId", detail.detailId)
                                    put("parentBillId", detail.parentBillId)
                                    put("type", detail.type)
                                    put("name", detail.name)
                                    put("amount", detail.amount)
                                    put("pricePerUnit", detail.pricePerUnit)
                                    put("previousReading", detail.previousReading)
                                    put("currentReading", detail.currentReading)
                                    put("usage", detail.usage)
                                }
                                detailsArray.put(detailJson)
                            }
                            put("details", detailsArray)
                        }
                        billsArray.put(billJson)
                    }

                    // 获取价格和隐私设置
                    val price = repository.getAllPrices()
                    val priceJson = JSONObject().apply {
                        put("water", price.water)
                        put("electricity", price.electricity)
                        put("privacyKeywords", price.privacyKeywords)
                    }

                    // 获取表名配置
                    val meterConfigs = repository.getAllMeterNameConfigs()
                    val configsArray = JSONArray()
                    meterConfigs.forEach { config ->
                        val configJson = JSONObject().apply {
                            put("meterType", config.meterType)
                            put("defaultName", config.defaultName)
                            put("customName", config.customName)
                            put("tenantRoomNumber", config.tenantRoomNumber)
                            put("isActive", config.isActive)
                            put("createdDate", config.createdDate)
                            put("updatedDate", config.updatedDate)
                        }
                        configsArray.put(configJson)
                    }

                    val exportData = JSONObject()
                    exportData.put("tenants", tenantsArray)
                    exportData.put("bills", billsArray)
                    exportData.put("price", priceJson)
                    exportData.put("meterConfigs", configsArray)
                    exportData.put("exportDate", System.currentTimeMillis())
                    exportData.put("appVersion", "1.0") // 可根据实际版本修改

                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(exportData.toString(4)) // 格式化JSON以便于阅读
                        }
                    }
                    
                    // 获取文件路径显示名称
                    val filePath = getRealFilePath(context, uri) ?: fileName
                    backupFilePath = filePath
                    
                    val successMessage = "数据备份成功\n\n文件已保存到: $fileName"
                    operationStatus = DataTransferStatus.Success(
                        message = successMessage, 
                        isExport = true,
                        filePath = filePath
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                operationStatus = DataTransferStatus.Error("备份失败: ${e.localizedMessage}")
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex("_display_name")
                if (displayNameIndex != -1) {
                    return it.getString(displayNameIndex)
                }
            }
        }
        return uri.lastPathSegment ?: uri.path ?: "未知位置"
    }
    
    // 获取真实文件路径
    private fun getRealFilePath(context: Context, uri: Uri): String? {
        try {
            // 尝试获取文件的真实路径
            val cursor = context.contentResolver.query(uri, arrayOf("_data"), null, null, null)
            return cursor?.use {
                val columnIndex = it.getColumnIndexOrThrow("_data")
                if (it.moveToFirst()) {
                    it.getString(columnIndex)
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun importData(context: Context, uri: Uri) {
        isExportOperation = false
        backupFilePath = null
        backupUri = null
        backupFileName = null
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            reader.readText()
                        }
                    } ?: throw Exception("无法读取文件")

                    val jsonObject = JSONObject(jsonString)
                    Log.d("DataTransferViewModel", "备份文件内容: ${jsonObject.toString().substring(0, minOf(200, jsonObject.toString().length))}")
                    
                    try {
                        // 检查租户数据是否存在
                        if (!jsonObject.has("tenants")) {
                            throw Exception("备份文件缺少租户数据")
                        }
                        val tenantsArray = jsonObject.getJSONArray("tenants")
                        
                        // 检查账单数据是否存在
                        if (!jsonObject.has("bills")) {
                            throw Exception("备份文件缺少账单数据") 
                        }
                        
                        // 账单数据可能是数组或嵌套对象
                        val bills = jsonObject.get("bills")
                        val billList = mutableListOf<JSONObject>()
                        
                        // 根据类型处理bills数据
                        if (bills is JSONArray) {
                            // bills是数组格式
                            for (i in 0 until bills.length()) {
                                billList.add(bills.getJSONObject(i))
                            }
                            Log.d("DataTransferViewModel", "常规数组格式的账单数据，数量: ${billList.size}")
                        } else if (bills.toString().startsWith("{")) {
                            // bills是嵌套对象格式 {roomNumber: {month: {bill}}}
                            Log.d("DataTransferViewModel", "检测到复杂嵌套账单数据结构")
                            
                            // 提取每个租户的账单
                            val billsObject = JSONObject(bills.toString())
                            val roomNumbers = billsObject.keys()
                            
                            while (roomNumbers.hasNext()) {
                                val roomNumber = roomNumbers.next()
                                val tenantBills = billsObject.getJSONObject(roomNumber)
                                val months = tenantBills.keys()
                                
                                while (months.hasNext()) {
                                    val month = months.next()
                                    val billData = tenantBills.getJSONObject(month)
                                    
                                    // 构造标准格式的账单对象
                                    val standardBill = JSONObject()
                                    standardBill.put("billId", billData.optInt("billId", 0))
                                    standardBill.put("tenantRoomNumber", roomNumber)
                                    standardBill.put("month", month)
                                    standardBill.put("totalAmount", billData.optDouble("totalAmount", 0.0))
                                    standardBill.put("createdDate", billData.optLong("createdDate", System.currentTimeMillis()))
                                    
                                    // 处理水电费明细
                                    val detailsArray = JSONArray()
                                    
                                    // 处理水费
                                    if (billData.has("water")) {
                                        val waterDetail = JSONObject()
                                        val waterData = billData.getJSONObject("water")
                                        waterDetail.put("detailId", 0)
                                        waterDetail.put("parentBillId", 0)
                                        waterDetail.put("type", "water")
                                        waterDetail.put("name", "水费")
                                        waterDetail.put("amount", waterData.optDouble("amount", 0.0))
                                        waterDetail.put("pricePerUnit", waterData.optDouble("pricePerUnit", 0.0))
                                        waterDetail.put("previousReading", waterData.optDouble("previous", 0.0))
                                        waterDetail.put("currentReading", waterData.optDouble("current", 0.0))
                                        waterDetail.put("usage", waterData.optDouble("usage", 0.0))
                                        detailsArray.put(waterDetail)
                                    }
                                    
                                    // 处理电费
                                    if (billData.has("electricity")) {
                                        val electricityDetail = JSONObject()
                                        val electricityData = billData.getJSONObject("electricity")
                                        electricityDetail.put("detailId", 0)
                                        electricityDetail.put("parentBillId", 0)
                                        electricityDetail.put("type", "electricity")
                                        electricityDetail.put("name", "电费")
                                        electricityDetail.put("amount", electricityData.optDouble("amount", 0.0))
                                        electricityDetail.put("pricePerUnit", electricityData.optDouble("pricePerUnit", 0.0))
                                        electricityDetail.put("previousReading", electricityData.optDouble("previous", 0.0))
                                        electricityDetail.put("currentReading", electricityData.optDouble("current", 0.0))
                                        electricityDetail.put("usage", electricityData.optDouble("usage", 0.0))
                                        detailsArray.put(electricityDetail)
                                    }
                                    
                                    // 处理额外费用
                                    if (billData.has("extraFees")) {
                                        val extraFeesArray = billData.getJSONArray("extraFees")
                                        for (j in 0 until extraFeesArray.length()) {
                                            val extraFee = extraFeesArray.getJSONObject(j)
                                            val extraDetail = JSONObject()
                                            extraDetail.put("detailId", extraFee.optInt("detailId", 0))
                                            extraDetail.put("parentBillId", 0)
                                            extraDetail.put("type", "extra")
                                            extraDetail.put("name", extraFee.getString("name"))
                                            extraDetail.put("amount", extraFee.optDouble("amount", 0.0))
                                            detailsArray.put(extraDetail)
                                        }
                                    }
                                    
                                    standardBill.put("details", detailsArray)
                                    billList.add(standardBill)
                                }
                            }
                            Log.d("DataTransferViewModel", "从嵌套结构中提取的账单数量: ${billList.size}")
                        } else {
                            throw Exception("无法识别账单数据格式")
                        }
                        
                        // 获取价格和隐私设置（如果存在）
                        val priceData = if (jsonObject.has("price")) {
                            jsonObject.getJSONObject("price")
                        } else if (jsonObject.has("prices")) {
                            jsonObject.getJSONObject("prices")
                        } else {
                            null
                        }
                        
                        // 获取表名配置（如果存在）
                        val meterConfigsArray = if (jsonObject.has("meterConfigs")) {
                            jsonObject.getJSONArray("meterConfigs")
                        } else {
                            null
                        }
                        
                        // 先删除所有现有数据
                        val allTenants = repository.allTenants.first()
                        allTenants.forEach { tenant ->
                            repository.delete(tenant)
                        }
                        
                        // 恢复租户数据
                        for (i in 0 until tenantsArray.length()) {
                            val tenantJson = tenantsArray.getJSONObject(i)
                            val tenant = Tenant(
                                roomNumber = tenantJson.getString("roomNumber"),
                                name = tenantJson.getString("name"),
                                rent = tenantJson.getDouble("rent")
                            )
                            repository.insert(tenant)
                        }
                        
                        // 恢复账单数据
                        for (billJson in billList) {
                            try {
                                // 创建账单对象
                                val bill = Bill(
                                    billId = 0, // 使用自动生成的ID
                                    tenantRoomNumber = billJson.getString("tenantRoomNumber"),
                                    month = billJson.getString("month"),
                                    totalAmount = billJson.getDouble("totalAmount")
                                )
                                
                                // 创建账单明细列表
                                val details = mutableListOf<BillDetail>()
                                val detailsArray = billJson.getJSONArray("details")
                                
                                for (j in 0 until detailsArray.length()) {
                                    val detailJson = detailsArray.getJSONObject(j)
                                    val detail = BillDetail(
                                        detailId = 0, // 使用自动生成的ID
                                        parentBillId = 0, // 稍后会被saveBill方法更新
                                        type = detailJson.getString("type"),
                                        name = detailJson.getString("name"),
                                        amount = detailJson.getDouble("amount"),
                                        pricePerUnit = if (detailJson.has("pricePerUnit") && !detailJson.isNull("pricePerUnit")) 
                                                    detailJson.getDouble("pricePerUnit") else null,
                                        previousReading = if (detailJson.has("previousReading") && !detailJson.isNull("previousReading")) 
                                                      detailJson.getDouble("previousReading") else null,
                                        currentReading = if (detailJson.has("currentReading") && !detailJson.isNull("currentReading")) 
                                                     detailJson.getDouble("currentReading") else null,
                                        usage = if (detailJson.has("usage") && !detailJson.isNull("usage")) 
                                             detailJson.getDouble("usage") else null
                                    )
                                    details.add(detail)
                                }
                                
                                // 保存账单和明细
                                repository.saveBill(bill, details)
                            } catch (e: Exception) {
                                Log.e("DataTransferViewModel", "导入单个账单失败: ${e.message}", e)
                                // 继续处理其他账单
                            }
                        }
                        
                        // 恢复价格和隐私设置（如果存在）
                        if (priceData != null) {
                            try {
                                val water = priceData.getDouble("water")
                                val electricity = priceData.getDouble("electricity")
                                val privacyKeywords = if (priceData.has("privacyKeywords")) 
                                                    priceData.getString("privacyKeywords") 
                                                    else "[]"
                                
                                // 保存水电价格
                                repository.savePrice(water, electricity)
                                
                                // 解析并保存隐私关键字
                                try {
                                    val keywordsArray = JSONArray(privacyKeywords)
                                    val keywordsList = mutableListOf<String>()
                                    for (i in 0 until keywordsArray.length()) {
                                        keywordsList.add(keywordsArray.getString(i))
                                    }
                                    repository.savePrivacyKeywords(keywordsList)
                                } catch (e: Exception) {
                                    Log.e("DataTransferViewModel", "解析隐私关键字失败", e)
                                }
                            } catch (e: Exception) {
                                Log.e("DataTransferViewModel", "保存价格设置失败", e)
                            }
                        }
                        
                        // 恢复表名配置（如果存在）
                        if (meterConfigsArray != null) {
                            // 先打印日志，帮助调试
                            Log.d("DataTransferViewModel", "找到 ${meterConfigsArray.length()} 个表名配置，开始恢复")
                            
                            for (i in 0 until meterConfigsArray.length()) {
                                try {
                                    val configJson = meterConfigsArray.getJSONObject(i)
                                    val meterType = configJson.getString("meterType")
                                    val defaultName = configJson.getString("defaultName")
                                    val customName = configJson.getString("customName")
                                    val tenantRoomNumber = configJson.getString("tenantRoomNumber")
                                    
                                    // 添加调试日志，检查表名配置
                                    Log.d("DataTransferViewModel", "恢复表名配置: $defaultName -> $customName (type: $meterType, tenant: $tenantRoomNumber)")
                                    
                                    // 判断表名是否符合可自定义规则（包含"水表"或"电表"，且不是主表）
                                    val containsWaterMeter = defaultName.contains("水表") 
                                    val containsElectricityMeter = defaultName.contains("电表")
                                    val isMainMeter = defaultName == "主水表" || defaultName == "主电表"
                                    val isExtraMeter = (containsWaterMeter || containsElectricityMeter) && !isMainMeter
                                    
                                    if (isExtraMeter) {
                                        Log.d("DataTransferViewModel", "表名配置符合自定义规则: $defaultName -> $customName")
                                        
                                        // 保存表名配置，使用TenantRepository中的方法
                                        repository.saveMeterCustomName(
                                            defaultName = defaultName,
                                            customName = customName,
                                            meterType = meterType,
                                            tenantRoomNumber = tenantRoomNumber
                                        )
                                    } else {
                                        Log.w("DataTransferViewModel", "表名不符合自定义规则，跳过: $defaultName")
                                    }
                                } catch (e: Exception) {
                                    Log.e("DataTransferViewModel", "导入单个表名配置失败: ${e.message}", e)
                                    // 继续处理其他配置
                                }
                            }
                        } else {
                            Log.d("DataTransferViewModel", "备份文件中没有找到表名配置")
                        }
                        
                        val fileName = getFileName(context, uri)
                        operationStatus = DataTransferStatus.Success(
                            message = "数据恢复成功\n\n已从文件恢复: $fileName",
                            isExport = false,
                            filePath = fileName
                        )
                        
                        // 数据恢复后执行一次自动备份
                        try {
                            com.morgen.rentalmanager.utils.AutoBackupManager.performAutoBackup(context, repository)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // 自动备份失败不影响恢复流程
                        }
                    } catch (e: Exception) {
                        Log.e("DataTransferViewModel", "处理备份数据失败: ${e.message}", e)
                        throw Exception("解析备份文件失败: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                operationStatus = DataTransferStatus.Error("恢复失败: ${e.localizedMessage}")
            }
        }
    }

    /**
     * 从自动备份文件导入数据
     */
    fun importAutoBackup(context: Context) {
        isExportOperation = false
        backupFilePath = null
        backupUri = null
        backupFileName = null
        
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 获取自动备份文件
                    val backupFile = com.morgen.rentalmanager.utils.AutoBackupManager.getLatestAutoBackup(context)
                    
                    if (backupFile == null) {
                        // 获取期望的自动备份文件路径，用于显示给用户
                        val expectedPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                            "${docsDir.absolutePath}/租房管家备份/租房管家自动备份.json"
                        } else {
                            val externalDir = ContextCompat.getExternalFilesDirs(context, null)[0]
                            "${externalDir.absolutePath}/租房管家备份/租房管家自动备份.json"
                        }
                        
                        throw Exception("未找到自动备份文件，请先保存账单以创建自动备份\n\n期望路径: $expectedPath")
                    }
                    
                    if (!backupFile.exists()) {
                        throw Exception("找到的自动备份文件不存在: ${backupFile.absolutePath}")
                    }
                    
                    // 读取备份文件内容
                    val jsonString = backupFile.readText()
                    val jsonObject = JSONObject(jsonString)
                    
                    Log.d("DataTransferViewModel", "自动备份文件内容: ${jsonObject.toString().substring(0, minOf(200, jsonObject.toString().length))}")
                    
                    try {
                        // 尝试识别和解析备份数据
                        // 由于自动备份现在与手动备份格式完全相同，直接检查必要字段即可
                        if (!jsonObject.has("tenants") || !jsonObject.has("bills")) {
                            throw Exception("备份文件缺少必要数据")
                        }
                        
                        // 获取租户数据
                        val tenantsArray = jsonObject.getJSONArray("tenants")
                        
                        // 获取账单数据(可能是Array或Object)
                        val bills = jsonObject.get("bills")
                        val billList = mutableListOf<JSONObject>()
                        
                        // 根据类型处理bills数据
                        if (bills is JSONArray) {
                            // bills是数组格式
                            Log.d("DataTransferViewModel", "发现数组格式账单数据")
                            for (i in 0 until bills.length()) {
                                billList.add(bills.getJSONObject(i))
                            }
                        } else if (bills.toString().startsWith("{")) {
                            // bills是嵌套对象格式 {roomNumber: {month: {bill}}}
                            Log.d("DataTransferViewModel", "发现嵌套对象格式账单数据")
                            
                            // 提取每个租户的账单
                            val billsObject = JSONObject(bills.toString())
                            val roomNumbers = billsObject.keys()
                            
                            while (roomNumbers.hasNext()) {
                                val roomNumber = roomNumbers.next()
                                val tenantBills = billsObject.getJSONObject(roomNumber)
                                val months = tenantBills.keys()
                                
                                while (months.hasNext()) {
                                    val month = months.next()
                                    val billData = tenantBills.getJSONObject(month)
                                    
                                    // 构造标准格式的账单对象
                                    val standardBill = JSONObject()
                                    standardBill.put("billId", billData.optInt("billId", 0))
                                    standardBill.put("tenantRoomNumber", roomNumber)
                                    standardBill.put("month", month)
                                    standardBill.put("totalAmount", billData.optDouble("totalAmount", 0.0))
                                    standardBill.put("createdDate", billData.optLong("createdDate", System.currentTimeMillis()))
                                    
                                    // 处理水电费明细
                                    val detailsArray = JSONArray()
                                    
                                    // 处理水费
                                    if (billData.has("water")) {
                                        val waterDetail = JSONObject()
                                        val waterData = billData.getJSONObject("water")
                                        waterDetail.put("detailId", 0)
                                        waterDetail.put("parentBillId", 0)
                                        waterDetail.put("type", "water")
                                        waterDetail.put("name", "水费")
                                        waterDetail.put("amount", waterData.optDouble("amount", 0.0))
                                        waterDetail.put("pricePerUnit", waterData.optDouble("pricePerUnit", 0.0))
                                        waterDetail.put("previousReading", waterData.optDouble("previous", 0.0))
                                        waterDetail.put("currentReading", waterData.optDouble("current", 0.0))
                                        waterDetail.put("usage", waterData.optDouble("usage", 0.0))
                                        detailsArray.put(waterDetail)
                                    }
                                    
                                    // 处理电费
                                    if (billData.has("electricity")) {
                                        val electricityDetail = JSONObject()
                                        val electricityData = billData.getJSONObject("electricity")
                                        electricityDetail.put("detailId", 0)
                                        electricityDetail.put("parentBillId", 0)
                                        electricityDetail.put("type", "electricity")
                                        electricityDetail.put("name", "电费")
                                        electricityDetail.put("amount", electricityData.optDouble("amount", 0.0))
                                        electricityDetail.put("pricePerUnit", electricityData.optDouble("pricePerUnit", 0.0))
                                        electricityDetail.put("previousReading", electricityData.optDouble("previous", 0.0))
                                        electricityDetail.put("currentReading", electricityData.optDouble("current", 0.0))
                                        electricityDetail.put("usage", electricityData.optDouble("usage", 0.0))
                                        detailsArray.put(electricityDetail)
                                    }
                                    
                                    // 处理额外费用
                                    if (billData.has("extraFees")) {
                                        val extraFeesArray = billData.getJSONArray("extraFees")
                                        for (j in 0 until extraFeesArray.length()) {
                                            val extraFee = extraFeesArray.getJSONObject(j)
                                            val extraDetail = JSONObject()
                                            extraDetail.put("detailId", extraFee.optInt("detailId", 0))
                                            extraDetail.put("parentBillId", 0)
                                            extraDetail.put("type", "extra")
                                            extraDetail.put("name", extraFee.getString("name"))
                                            extraDetail.put("amount", extraFee.optDouble("amount", 0.0))
                                            detailsArray.put(extraDetail)
                                        }
                                    }
                                    
                                    standardBill.put("details", detailsArray)
                                    billList.add(standardBill)
                                }
                            }
                        } else {
                            throw Exception("无法识别账单数据格式")
                        }
                        
                        // 获取价格配置
                        val priceData = if (jsonObject.has("prices")) {
                            jsonObject.getJSONObject("prices")
                        } else if (jsonObject.has("price")) {
                            jsonObject.getJSONObject("price")
                        } else {
                            null
                        }
                        
                        // 获取表名配置
                        val meterConfigsArray = if (jsonObject.has("meterConfigs")) {
                            jsonObject.getJSONArray("meterConfigs")
                        } else {
                            null
                        }
                        
                        // 开始导入数据
                        // 先删除所有现有数据
                        val allTenants = repository.allTenants.first()
                        allTenants.forEach { tenant ->
                            repository.delete(tenant)
                        }
                        
                        // 导入租户数据
                        for (i in 0 until tenantsArray.length()) {
                            val tenantJson = tenantsArray.getJSONObject(i)
                            val tenant = Tenant(
                                roomNumber = tenantJson.getString("roomNumber"),
                                name = tenantJson.getString("name"),
                                rent = tenantJson.getDouble("rent")
                            )
                            repository.insert(tenant)
                        }
                        
                        // 导入账单数据
                        for (billJson in billList) {
                            try {
                                // 创建账单对象
                                val bill = Bill(
                                    billId = 0, // 使用自动生成的ID
                                    tenantRoomNumber = billJson.getString("tenantRoomNumber"),
                                    month = billJson.getString("month"),
                                    totalAmount = billJson.getDouble("totalAmount")
                                )
                                
                                // 创建账单明细列表
                                val details = mutableListOf<BillDetail>()
                                val detailsArray = billJson.getJSONArray("details")
                                
                                for (j in 0 until detailsArray.length()) {
                                    val detailJson = detailsArray.getJSONObject(j)
                                    val detail = BillDetail(
                                        detailId = 0, // 使用自动生成的ID
                                        parentBillId = 0, // 稍后会被saveBill方法更新
                                        type = detailJson.getString("type"),
                                        name = detailJson.getString("name"),
                                        amount = detailJson.getDouble("amount"),
                                        pricePerUnit = if (detailJson.has("pricePerUnit") && !detailJson.isNull("pricePerUnit")) 
                                                        detailJson.getDouble("pricePerUnit") else null,
                                        previousReading = if (detailJson.has("previousReading") && !detailJson.isNull("previousReading")) 
                                                          detailJson.getDouble("previousReading") else null,
                                        currentReading = if (detailJson.has("currentReading") && !detailJson.isNull("currentReading")) 
                                                         detailJson.getDouble("currentReading") else null,
                                        usage = if (detailJson.has("usage") && !detailJson.isNull("usage")) 
                                                 detailJson.getDouble("usage") else null
                                    )
                                    details.add(detail)
                                }
                                
                                // 保存账单和明细
                                repository.saveBill(bill, details)
                            } catch (e: Exception) {
                                Log.e("DataTransferViewModel", "导入账单失败: ${e.message}", e)
                                // 继续处理其他账单
                            }
                        }
                        
                        // 保存价格设置
                        if (priceData != null) {
                            try {
                                val water = priceData.getDouble("water")
                                val electricity = priceData.getDouble("electricity")
                                val privacyKeywords = if (priceData.has("privacyKeywords")) 
                                                    priceData.getString("privacyKeywords") 
                                                    else "[]"
                                
                                // 保存水电价格
                                repository.savePrice(water, electricity)
                                
                                // 解析并保存隐私关键字
                                try {
                                    val keywordsArray = JSONArray(privacyKeywords)
                                    val keywordsList = mutableListOf<String>()
                                    for (i in 0 until keywordsArray.length()) {
                                        keywordsList.add(keywordsArray.getString(i))
                                    }
                                    repository.savePrivacyKeywords(keywordsList)
                                } catch (e: Exception) {
                                    Log.e("DataTransferViewModel", "解析隐私关键字失败", e)
                                }
                            } catch (e: Exception) {
                                Log.e("DataTransferViewModel", "保存价格设置失败", e)
                            }
                        }
                        
                        // 保存表名配置
                        if (meterConfigsArray != null) {
                            // 先打印日志，帮助调试
                            Log.d("DataTransferViewModel", "自动备份中找到 ${meterConfigsArray.length()} 个表名配置，开始恢复")
                            
                            for (i in 0 until meterConfigsArray.length()) {
                                try {
                                    val configJson = meterConfigsArray.getJSONObject(i)
                                    val meterType = configJson.getString("meterType")
                                    val defaultName = configJson.getString("defaultName")
                                    val customName = configJson.getString("customName")
                                    val tenantRoomNumber = configJson.getString("tenantRoomNumber")
                                    
                                    // 添加调试日志，检查表名配置
                                    Log.d("DataTransferViewModel", "恢复表名配置: $defaultName -> $customName (type: $meterType, tenant: $tenantRoomNumber)")
                                    
                                    // 判断表名是否符合可自定义规则（包含"水表"或"电表"，且不是主表）
                                    val containsWaterMeter = defaultName.contains("水表") 
                                    val containsElectricityMeter = defaultName.contains("电表")
                                    val isMainMeter = defaultName == "主水表" || defaultName == "主电表"
                                    val isExtraMeter = (containsWaterMeter || containsElectricityMeter) && !isMainMeter
                                    
                                    if (isExtraMeter) {
                                        Log.d("DataTransferViewModel", "表名配置符合自定义规则: $defaultName -> $customName")
                                        
                                        // 保存表名配置，使用TenantRepository中的方法
                                        repository.saveMeterCustomName(
                                            defaultName = defaultName,
                                            customName = customName,
                                            meterType = meterType,
                                            tenantRoomNumber = tenantRoomNumber
                                        )
                                    } else {
                                        Log.w("DataTransferViewModel", "表名不符合自定义规则，跳过: $defaultName")
                                    }
                                } catch (e: Exception) {
                                    Log.e("DataTransferViewModel", "导入单个表名配置失败: ${e.message}", e)
                                    // 继续处理其他配置
                                }
                            }
                        } else {
                            Log.d("DataTransferViewModel", "自动备份中没有找到表名配置")
                        }
                        
                        operationStatus = DataTransferStatus.Success(
                            message = "已成功从自动备份文件恢复数据\n\n文件位置: ${backupFile.absolutePath}",
                            isExport = false,
                            filePath = backupFile.absolutePath
                        )
                    } catch (e: Exception) {
                        Log.e("DataTransferViewModel", "处理备份数据失败", e)
                        throw Exception("解析备份文件失败: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                operationStatus = DataTransferStatus.Error("从自动备份恢复失败: ${e.localizedMessage}")
            }
        }
    }

    // 重置状态
    fun resetStatus() {
        operationStatus = DataTransferStatus.Idle
        // 不重置isExportOperation、backupFilePath、backupUri和backupFileName，
        // 这样在对话框消失后仍能保留这些信息
    }
}

class DataTransferViewModelFactory(private val repository: TenantRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DataTransferViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DataTransferViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 