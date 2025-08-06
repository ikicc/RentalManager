package com.morgen.rentalmanager.utils

import com.morgen.rentalmanager.myapplication.MeterNameConfig
import com.morgen.rentalmanager.myapplication.MeterNameConfigDao
import com.morgen.rentalmanager.myapplication.DataSyncManager
import kotlinx.coroutines.flow.first

/**
 * 水表/电表名称管理器
 * 处理自定义名称的映射和验证
 */
class MeterNameManager(
    private val meterNameConfigDao: MeterNameConfigDao
) {
    /**
     * 获取显示名称
     * 如果有自定义名称则返回自定义名称，否则返回默认名称
     * 主水表和主电表名称不允许自定义
     */
    suspend fun getDisplayName(defaultName: String, tenantRoomNumber: String = ""): String {
        if (defaultName.isBlank()) {
            android.util.Log.w("MeterNameManager", "默认名称为空，直接返回")
            return defaultName
        }
        
        // 详细记录输入参数
        android.util.Log.d("MeterNameManager", "获取显示名称 - 输入参数: defaultName='$defaultName', tenantRoomNumber='$tenantRoomNumber'")
        
        // 主水表和主电表不允许自定义名称
        if (defaultName == "主水表" || defaultName == "主电表") {
            android.util.Log.d("MeterNameManager", "主表名称不允许自定义，直接返回: '$defaultName'")
            return defaultName
        }
        
        // 使用更宽松的表名识别规则：
        // 如果包含"水表"或"电表"，且不是"主水表"或"主电表"，则允许自定义
        val containsWaterMeter = defaultName.contains("水表") 
        val containsElectricityMeter = defaultName.contains("电表")
        val isMainMeter = defaultName == "主水表" || defaultName == "主电表"
        val isExtraMeter = (containsWaterMeter || containsElectricityMeter) && !isMainMeter
        
        // 如果不符合最初的规则，使用更宽松的规则再检查一次
        if (!isExtraMeter) {
            // 更宽松的规则：包含"水表"或"电表"且不是"主水表"或"主电表"
            val moreRelaxedCheck = (defaultName.contains("水表") || defaultName.contains("电表")) && 
                                  defaultName != "主水表" && defaultName != "主电表"
            
            if (moreRelaxedCheck) {
                android.util.Log.d("MeterNameManager", "使用宽松规则判断，允许自定义表名: '$defaultName'")
                // 继续执行，查询自定义名称
            } else {
                android.util.Log.d("MeterNameManager", "非水电表名称不允许自定义，直接返回: '$defaultName'")
            return defaultName
            }
        } else {
            android.util.Log.d("MeterNameManager", "识别到水电表: '$defaultName'，可以自定义")
        }
        
        return try {
            android.util.Log.d("MeterNameManager", "查询自定义名称: '$defaultName' (租户: '$tenantRoomNumber')")
            
            // 查询前确保DAO实例有效
            if (meterNameConfigDao == null) {
                android.util.Log.e("MeterNameManager", "meterNameConfigDao为null，无法查询")
                return defaultName
            }
            
            // 只查找该租户的自定义名称，不使用全局配置
            var customName: String? = null
            if (tenantRoomNumber.isNotBlank()) {
                customName = meterNameConfigDao.getCustomNameByDefaultAndTenant(defaultName, tenantRoomNumber)
                android.util.Log.d("MeterNameManager", "租户特定查询结果: '$defaultName' -> ${customName ?: "null"}")
            } else {
                android.util.Log.w("MeterNameManager", "租户房间号为空，无法查询自定义名称")
            }
            
            if (customName != null && customName.isNotBlank()) {
                android.util.Log.d("MeterNameManager", "使用自定义名称: '$defaultName' -> '$customName'")
                customName
            } else {
                android.util.Log.d("MeterNameManager", "使用默认名称: '$defaultName'")
                defaultName
            }
        } catch (e: Exception) {
            // 满足需求 5.1 - 数据库配置损坏时使用默认名称并记录错误
            android.util.Log.e("MeterNameManager", "获取自定义名称失败，使用默认名称: '$defaultName'", e)
            defaultName
        }
    }
    
    /**
     * 保存自定义名称
     * 只允许额外水表和额外电表自定义名称
     */
    suspend fun saveCustomName(defaultName: String, customName: String, meterType: String, tenantRoomNumber: String = ""): Result<Unit> {
        // 详细记录输入参数
        android.util.Log.d("MeterNameManager", "保存自定义名称 - 输入参数: defaultName='$defaultName', customName='$customName', meterType='$meterType', tenantRoomNumber='$tenantRoomNumber'")
        
        // 验证：只允许额外水表和额外电表自定义名称
        if (defaultName == "主水表" || defaultName == "主电表") {
            android.util.Log.w("MeterNameManager", "不允许修改主表名称: $defaultName")
            return Result.failure(IllegalArgumentException("不允许修改主表名称"))
        }
        
        // 使用与getDisplayName相同的宽松判断规则
        val containsWaterMeter = defaultName.contains("水表") 
        val containsElectricityMeter = defaultName.contains("电表")
        val isMainMeter = defaultName == "主水表" || defaultName == "主电表"
        val isExtraMeter = (containsWaterMeter || containsElectricityMeter) && !isMainMeter
        
        // 为了解决恢复备份时的问题，我们增加额外的宽松规则检查
        if (!isExtraMeter) {
            // 对于导入备份场景，我们使用更宽松的规则：如果包含"水表"或"电表"，且不是"主水表"或"主电表"，则允许自定义
            val moreRelaxedCheck = (defaultName.contains("水表") || defaultName.contains("电表")) && 
                                   defaultName != "主水表" && defaultName != "主电表"
            
            if (moreRelaxedCheck) {
                android.util.Log.d("MeterNameManager", "使用宽松规则判断，允许修改表名: '$defaultName'")
                // 继续执行，允许保存
            } else {
                android.util.Log.w("MeterNameManager", "只允许修改水电表名称: $defaultName")
                return Result.failure(IllegalArgumentException("只允许修改水电表名称"))
            }
        } else {
            android.util.Log.d("MeterNameManager", "确认是水电表，可以修改名称: '$defaultName'")
        }
        
        // 验证自定义名称是否合法
        if (!validateCustomName(customName)) {
            android.util.Log.w("MeterNameManager", "自定义名称不合法: $customName")
            return Result.failure(IllegalArgumentException("自定义名称不合法"))
            }
            
        // 清理和转义特殊字符
        val sanitizedCustomName = sanitizeCustomName(customName)
        
        // 处理自定义名称与默认名称相同的情况
        if (sanitizedCustomName == defaultName) {
            // 如果设置为与默认名称相同，相当于重置为默认名称
            return resetToDefault(defaultName, tenantRoomNumber)
        }
        
        return try {
            val config = MeterNameConfig(
                meterType = meterType,
                defaultName = defaultName,
                customName = sanitizedCustomName,
                tenantRoomNumber = tenantRoomNumber,
                updatedDate = System.currentTimeMillis()
            )
            
            // 通知表名称变更事件
            DataSyncManager.notifyMeterNameChanged(defaultName, sanitizedCustomName, tenantRoomNumber)
            
            // 保存到数据库，确保每个租户的每个表名只有一个配置
            meterNameConfigDao.upsertMeterNameConfig(config)
            
            android.util.Log.d("MeterNameManager", "自定义名称保存成功: '$defaultName' -> '$sanitizedCustomName'")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("MeterNameManager", "保存自定义名称失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 重置为默认名称
     */
    suspend fun resetToDefault(defaultName: String, tenantRoomNumber: String = ""): Result<Unit> {
        return try {
            if (tenantRoomNumber.isBlank()) {
                android.util.Log.w("MeterNameManager", "租户房间号为空，无法重置配置")
                return Result.failure(IllegalArgumentException("租户房间号不能为空"))
            }
            
            // 只重置特定租户的配置
            meterNameConfigDao.deactivateConfigForTenant(defaultName, tenantRoomNumber)
            
            // 通知表名称变更事件
            DataSyncManager.notifyMeterNameChanged(defaultName, defaultName, tenantRoomNumber)
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("MeterNameManager", "重置默认名称失败: $defaultName", e)
            Result.failure(e)
        }
    }
    
    /**
     * 验证自定义名称
     * 满足需求 1.3 - 验证名称不为空且长度在合理范围内（1-20个字符）
     */
    fun validateCustomName(name: String): Boolean {
        return name.isNotBlank() && name.length in 1..20
    }
    
    /**
     * 获取所有活跃的配置
     * 用于设置界面显示当前配置
     */
    suspend fun getAllActiveConfigs(): List<MeterNameConfig> {
        return try {
            meterNameConfigDao.getAllActiveConfigs().first()
        } catch (e: Exception) {
            android.util.Log.e("MeterNameManager", "获取配置列表失败", e)
            emptyList()
        }
    }

    /**
     * 获取某租户的所有自定义配置
     */
    suspend fun getConfigsByTenant(tenantRoomNumber: String): List<MeterNameConfig> {
        return try {
            meterNameConfigDao.getActiveConfigsByTenant(tenantRoomNumber).first()
        } catch (e: Exception) {
            android.util.Log.e("MeterNameManager", "获取租户配置失败: $tenantRoomNumber", e)
            emptyList()
        }
    }
    
    /**
     * 根据类型获取配置
     */
    suspend fun getConfigsByType(meterType: String): List<MeterNameConfig> {
        return try {
            meterNameConfigDao.getConfigsByType(meterType)
        } catch (e: Exception) {
            android.util.Log.e("MeterNameManager", "获取类型配置失败: $meterType", e)
            emptyList()
        }
    }

    /**
     * 根据类型和租户获取配置
     */
    suspend fun getConfigsByTypeAndTenant(meterType: String, tenantRoomNumber: String): List<MeterNameConfig> {
        return try {
            meterNameConfigDao.getConfigsByTypeAndTenant(meterType, tenantRoomNumber)
        } catch (e: Exception) {
            android.util.Log.e("MeterNameManager", "获取类型租户配置失败: $meterType, $tenantRoomNumber", e)
            emptyList()
        }
    }
    
    /**
     * 清理和转义特殊字符
     * 满足需求 5.2 - 处理特殊字符
     */
    private fun sanitizeCustomName(name: String): String {
        // 移除或替换可能导致问题的特殊字符
        return name
            .replace(Regex("[<>\"'&]"), "") // 移除HTML特殊字符
            .replace(Regex("\\s+"), " ") // 将多个空格替换为单个空格
            .trim()
    }

    /**
     * 列出所有活跃的自定义表名配置，用于调试
     */
    suspend fun dumpAllConfigurations() {
        android.util.Log.d("MeterNameManager", "==== 开始打印所有自定义表名配置 ====")
        try {
            val configs = meterNameConfigDao.getAllActiveConfigs().first()
            if (configs.isEmpty()) {
                android.util.Log.w("MeterNameManager", "没有发现任何自定义表名配置")
            } else {
                android.util.Log.d("MeterNameManager", "找到 ${configs.size} 个自定义表名配置:")
                configs.forEach { config ->
                    val isExtra = config.defaultName.startsWith("额外水表") || config.defaultName.startsWith("额外电表")
                    val prefix = if (isExtra) "✓ " else "✗ "
                    android.util.Log.d("MeterNameManager", "$prefix ${config.defaultName} -> ${config.customName} (${config.meterType})")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MeterNameManager", "打印配置异常", e)
        }
        android.util.Log.d("MeterNameManager", "==== 打印自定义表名配置结束 ====")
    }
}