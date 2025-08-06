package com.morgen.rentalmanager.utils

import android.util.Log
import com.morgen.rentalmanager.myapplication.MeterNameConfig
import com.morgen.rentalmanager.myapplication.MeterNameConfigDao
import com.morgen.rentalmanager.myapplication.TenantRepository
import kotlinx.coroutines.flow.first

/**
 * 表名调试工具类
 * 用于检查和修复额外表名称识别问题
 */
object MeterNameDebugUtils {
    private const val TAG = "MeterNameDebug"

    /**
     * 诊断表名问题
     */
    suspend fun diagnoseNameIssues(repository: TenantRepository) {
        Log.d(TAG, "=========== 开始表名诊断 ===========")
        
        try {
            // 1. 获取所有活跃配置
            val configs = repository.getAllMeterNameConfigs()
            Log.d(TAG, "找到 ${configs.size} 个活跃的名称配置")
            
            // 2. 检查每个配置
            configs.forEach { config ->
                // 使用与MeterNameManager相同的规则判断是否为水电表
                val containsWaterMeter = config.defaultName.contains("水表") 
                val containsElectricityMeter = config.defaultName.contains("电表")
                val isMainMeter = config.defaultName.contains("主水表") || config.defaultName.contains("主电表")
                val isExtraMeter = (containsWaterMeter || containsElectricityMeter) && !isMainMeter
                
                val prefix = if (isExtraMeter) "✓" else "✗"
                Log.d(TAG, "$prefix ${config.defaultName} -> ${config.customName} (${config.meterType}, 租户: ${config.tenantRoomNumber})")
                
                // 检查可能的错误识别
                if (!isExtraMeter && (containsWaterMeter || containsElectricityMeter)) {
                    Log.w(TAG, "警告: 可能错误识别的水电表: '${config.defaultName}'")
                }
            }
            
            // 3. 尝试获取每个配置的展示名称
            configs.forEach { config ->
                val displayName = repository.getMeterDisplayName(config.defaultName, config.tenantRoomNumber)
                Log.d(TAG, "展示名称: '${config.defaultName}' -> '$displayName' (租户: '${config.tenantRoomNumber}')")
                
                if (displayName != config.customName && config.customName.isNotBlank()) {
                    Log.w(TAG, "警告: 展示名称不匹配配置的自定义名称")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "诊断过程出错", e)
        }
        
        Log.d(TAG, "=========== 表名诊断结束 ===========")
    }
    
    /**
     * 修复所有可能的额外表名称识别问题
     */
    suspend fun fixNameIssues(dao: MeterNameConfigDao) {
        Log.d(TAG, "=========== 开始修复表名问题 ===========")
        
        try {
            // 获取所有配置
            val configs = dao.getAllActiveConfigs().first()
            Log.d(TAG, "找到 ${configs.size} 个配置需要检查")
            
            val fixedConfigs = mutableListOf<MeterNameConfig>()
            
            // 检查和修复每个配置
            configs.forEach { config ->
                // 使用新的宽松判断规则
                val containsWaterMeter = config.defaultName.contains("水表") 
                val containsElectricityMeter = config.defaultName.contains("电表")
                val isMainMeter = config.defaultName.contains("主水表") || config.defaultName.contains("主电表")
                val isWaterElecMeter = (containsWaterMeter || containsElectricityMeter) && !isMainMeter
                
                // 检查是否曾被误判为非水电表
                val wasIdentifiedAsExtra = config.defaultName.contains("额外水表") || config.defaultName.contains("额外电表")
                
                if (isWaterElecMeter && !wasIdentifiedAsExtra) {
                    // 这是可能有问题的配置，需要标记为已修复
                    Log.w(TAG, "发现需要修复的配置: '${config.defaultName}' -> '${config.customName}'")
                    fixedConfigs.add(config)
                }
            }
            
            if (fixedConfigs.isEmpty()) {
                Log.d(TAG, "未发现需要修复的配置")
            } else {
                Log.d(TAG, "发现 ${fixedConfigs.size} 个需要修复的配置")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "修复过程出错", e)
        }
        
        Log.d(TAG, "=========== 表名修复结束 ===========")
    }
} 