package com.morgen.rentalmanager

import android.app.Application
import android.util.Log
import com.morgen.rentalmanager.myapplication.AppDatabase
import com.morgen.rentalmanager.myapplication.TenantRepository
import com.morgen.rentalmanager.utils.DevicePerformanceTier
import com.morgen.rentalmanager.utils.PerformanceUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TenantApplication : Application() {
    // 应用级协程作用域
    private val applicationScope = CoroutineScope(SupervisorJob())
    
    // 异步加载状态
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady
    
    // 设备性能等级
    private lateinit var _devicePerformanceTier: DevicePerformanceTier
    val devicePerformanceTier: DevicePerformanceTier 
        get() = _devicePerformanceTier
    
    // 懒加载数据库
    val database: AppDatabase by lazy { 
        // 始终返回一个实例，即使异步初始化没完成也可访问
        AppDatabase.getDatabase(this)
    }
    
    // 懒加载仓库
    val repository: TenantRepository by lazy { 
        TenantRepository(database.tenantDao(), database.billDao(), database.priceDao(), database.meterNameConfigDao())
    }

    /**
     * 预热关键数据库操作
     * 提前初始化一些慢速操作，以加快后续交互
     */
    private fun prewarmDatabaseOperations() {
        kotlinx.coroutines.MainScope().launch {
            try {
                // 检查自定义表名配置
                repository.getAllMeterNameConfigs()
                android.util.Log.d("TenantApplication", "预热数据库操作完成")
            } catch (e: Exception) {
                android.util.Log.e("TenantApplication", "预热数据库操作失败", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // 第一步：立即检测设备性能等级，并确保即使失败也有默认值
        try {
            _devicePerformanceTier = PerformanceUtils.detectDevicePerformanceTier(this)
            Log.d("TenantApplication", "设备性能等级检测完成: $_devicePerformanceTier")
        } catch (e: Exception) {
            // 如果性能检测失败，使用中等级别作为默认值
            _devicePerformanceTier = DevicePerformanceTier.MEDIUM
            Log.e("TenantApplication", "设备性能等级检测失败，使用默认值: $_devicePerformanceTier", e)
        }
        
        // 记录启动性能信息
        Log.d("TenantApplication", "应用启动中，设备性能等级: $_devicePerformanceTier")
        
        // 第二步：使用协程异步初始化数据库
        initDatabaseAsync()
        
        // 初始化自定义表名测试数据
        initializeTestMeterNames()
        
        // 预热数据库操作
        prewarmDatabaseOperations()
    }
    
    private fun initDatabaseAsync() {
        AppDatabase.createDatabaseAsync(this, applicationScope) { db ->
            // 数据库初始化完成后的回调
            _isReady.value = true
            Log.d("TenantApplication", "数据库初始化完成")
        }
    }
    
    // 测试用：初始化一些自定义表名（仅用于调试）
    private fun initializeTestMeterNames() {
        // 使用协程在后台线程中执行
        kotlinx.coroutines.MainScope().launch {
            try {
                android.util.Log.d("TenantApplication", "开始初始化测试表名...")
                
                // 添加几个测试用的自定义表名（仅针对额外表）
                try {
                    repository.saveMeterCustomName("额外水表1", "厨房水表", "water")
                    repository.saveMeterCustomName("额外水表2", "卫生间水表", "water")
                    repository.saveMeterCustomName("额外电表1", "空调电表", "electricity")
                    android.util.Log.d("TenantApplication", "成功添加测试自定义表名")
                } catch (e: Exception) {
                    android.util.Log.e("TenantApplication", "添加自定义表名失败，但不影响程序运行", e)
                }
                
                // 打印所有配置
                val configs = repository.getAllMeterNameConfigs()
                android.util.Log.d("TenantApplication", "当前有 ${configs.size} 个自定义表名配置:")
                configs.forEach { config ->
                    android.util.Log.d("TenantApplication", "- ${config.defaultName} -> ${config.customName} (${config.meterType})")
                }
                
                android.util.Log.d("TenantApplication", "测试表名初始化完成")
            } catch (e: Exception) {
                android.util.Log.e("TenantApplication", "初始化测试表名失败", e)
            }
        }
    }
} 