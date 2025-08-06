package com.morgen.rentalmanager.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
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
 * 自动备份管理器
 * 提供自动备份功能，在保存账单后自动执行备份
 */
object AutoBackupManager {
    private const val TAG = "AutoBackupManager"
    private const val AUTO_BACKUP_FILENAME = "租房管家自动备份.json"
    private const val BACKUP_FOLDER_NAME = "租房管家备份"

    /**
     * 执行自动备份
     * @param context 应用上下文
     * @param repository 数据仓库
     */
    suspend fun performAutoBackup(context: Context, repository: TenantRepository) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始执行自动备份")

            // 使用与手动备份完全相同的方式创建备份数据
            val jsonObject = createBackupData(repository)
            
            // 始终使用外部存储的公共文档目录，确保应用数据清除后仍能访问
            val documentsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), BACKUP_FOLDER_NAME)
            
            // 确保备份目录存在
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
                Log.d(TAG, "创建备份目录: ${documentsDir.absolutePath}")
            }
            
            // 使用固定文件名(覆盖式备份)
            val backupFile = File(documentsDir, AUTO_BACKUP_FILENAME)
            
            // 先检查文件是否存在，如存在则先删除再创建，确保覆盖成功
            if (backupFile.exists()) {
                backupFile.delete()
                Log.d(TAG, "删除已存在的自动备份文件")
            }
            
            FileOutputStream(backupFile).use { 
                // 使用与手动备份相同的格式化方式（缩进4个空格）
                it.write(jsonObject.toString(4).toByteArray())
            }
            
            Log.d(TAG, "自动备份完成: ${backupFile.absolutePath}")
            
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "自动备份失败", e)
            return@withContext false
        }
    }
    
    /**
     * 创建备份数据，与手动备份功能完全一致
     */
    private suspend fun createBackupData(repository: TenantRepository): JSONObject = withContext(Dispatchers.IO) {
        // 获取所有租户
        val tenants = repository.allTenants.first()
        val tenantsArray = JSONArray()
        for (tenant in tenants) {
            val tenantJson = JSONObject()
            tenantJson.put("roomNumber", tenant.roomNumber)
            tenantJson.put("name", tenant.name)
            tenantJson.put("rent", tenant.rent)
            tenantsArray.put(tenantJson)
        }
        
        // 获取所有账单
        val billsWithDetails = repository.getAllBillsWithDetails().first()
        val billsArray = JSONArray()
        for (billWithDetails in billsWithDetails) {
            val billJson = JSONObject()
            billJson.put("billId", billWithDetails.bill.billId)
            billJson.put("tenantRoomNumber", billWithDetails.bill.tenantRoomNumber)
            billJson.put("month", billWithDetails.bill.month)
            billJson.put("totalAmount", billWithDetails.bill.totalAmount)
            billJson.put("createdDate", billWithDetails.bill.createdDate)
            
            // 账单明细
            val detailsArray = JSONArray()
            for (detail in billWithDetails.details) {
                val detailJson = JSONObject()
                detailJson.put("detailId", detail.detailId)
                detailJson.put("parentBillId", detail.parentBillId)
                detailJson.put("type", detail.type)
                detailJson.put("name", detail.name)
                detailJson.put("amount", detail.amount)
                detailJson.put("pricePerUnit", detail.pricePerUnit)
                detailJson.put("previousReading", detail.previousReading)
                detailJson.put("currentReading", detail.currentReading)
                detailJson.put("usage", detail.usage)
                detailsArray.put(detailJson)
            }
            billJson.put("details", detailsArray)
            billsArray.put(billJson)
        }

        // 获取价格和隐私设置
        val price = repository.getAllPrices()
        val priceJson = JSONObject()
        priceJson.put("water", price.water)
        priceJson.put("electricity", price.electricity)
        priceJson.put("privacyKeywords", price.privacyKeywords)

        // 获取表名配置
        val meterConfigs = repository.getAllMeterNameConfigs()
        val configsArray = JSONArray()
        for (config in meterConfigs) {
            val configJson = JSONObject()
            configJson.put("meterType", config.meterType)
            configJson.put("defaultName", config.defaultName)
            configJson.put("customName", config.customName)
            configJson.put("tenantRoomNumber", config.tenantRoomNumber)
            configJson.put("isActive", config.isActive)
            configJson.put("createdDate", config.createdDate)
            configJson.put("updatedDate", config.updatedDate)
            configsArray.put(configJson)
        }

        // 创建与手动备份完全一致的导出数据结构
        val exportData = JSONObject()
        exportData.put("tenants", tenantsArray)
        exportData.put("bills", billsArray)
        exportData.put("price", priceJson)
        exportData.put("meterConfigs", configsArray)
        exportData.put("exportDate", System.currentTimeMillis())
        exportData.put("appVersion", "1.0") // 与手动备份保持一致
        
        return@withContext exportData
    }
    
    /**
     * 获取自动备份文件
     * @param context 应用上下文
     * @return 备份文件，如不存在则返回null
     */
    fun getLatestAutoBackup(context: Context): File? {
        try {
            // 始终使用外部存储的公共文档目录，确保应用数据清除后仍能访问
            val documentsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), BACKUP_FOLDER_NAME)
            
            if (!documentsDir.exists()) {
                Log.d(TAG, "备份目录不存在: ${documentsDir.absolutePath}")
                return null
            }
            
            // 直接查找固定文件名的备份文件
            val backupFile = File(documentsDir, AUTO_BACKUP_FILENAME)
            
            if (!backupFile.exists()) {
                Log.d(TAG, "自动备份文件不存在: ${backupFile.absolutePath}")
                return null
            }
            
            Log.d(TAG, "找到自动备份文件: ${backupFile.absolutePath}")
            return backupFile
            
        } catch (e: Exception) {
            Log.e(TAG, "获取自动备份文件失败", e)
            return null
        }
    }
} 