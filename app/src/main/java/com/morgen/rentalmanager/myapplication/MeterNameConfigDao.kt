package com.morgen.rentalmanager.myapplication

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 水表/电表自定义名称配置数据访问对象
 */
@Dao
interface MeterNameConfigDao {
    
    /**
     * 获取所有活跃的配置
     */
    @Query("SELECT * FROM meter_name_configs WHERE is_active = 1")
    fun getAllActiveConfigs(): Flow<List<MeterNameConfig>>
    
    /**
     * 根据租户获取所有活跃的配置
     */
    @Query("SELECT * FROM meter_name_configs WHERE tenant_room_number = :tenantRoomNumber AND is_active = 1")
    fun getActiveConfigsByTenant(tenantRoomNumber: String): Flow<List<MeterNameConfig>>
    
    /**
     * 根据类型获取配置
     */
    @Query("SELECT * FROM meter_name_configs WHERE meter_type = :type AND is_active = 1")
    suspend fun getConfigsByType(type: String): List<MeterNameConfig>
    
    /**
     * 根据类型和租户获取配置
     */
    @Query("SELECT * FROM meter_name_configs WHERE meter_type = :type AND tenant_room_number = :tenantRoomNumber AND is_active = 1")
    suspend fun getConfigsByTypeAndTenant(type: String, tenantRoomNumber: String): List<MeterNameConfig>
    
    /**
     * 根据默认名称获取自定义名称
     * 优先查询指定租户的配置，如果没有再查询全局配置
     */
    @Query("SELECT custom_name FROM meter_name_configs WHERE default_name = :defaultName AND tenant_room_number = :tenantRoomNumber AND is_active = 1 ORDER BY updated_date DESC LIMIT 1")
    suspend fun getCustomNameByDefaultAndTenant(defaultName: String, tenantRoomNumber: String): String?
    
    /**
     * 根据默认名称获取全局自定义名称
     */
    @Query("SELECT custom_name FROM meter_name_configs WHERE default_name = :defaultName AND tenant_room_number = '' AND is_active = 1 ORDER BY updated_date DESC LIMIT 1")
    suspend fun getCustomNameByDefault(defaultName: String): String?
    
    /**
     * 插入或更新配置
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: MeterNameConfig)
    
    /**
     * 更新或插入特定租户的表名配置
     */
    @Transaction
    suspend fun upsertMeterNameConfig(config: MeterNameConfig) {
        // 先停用现有的配置
        deactivateConfigForTenant(config.defaultName, config.tenantRoomNumber)
        // 然后插入新配置
        insertOrUpdate(config)
    }
    
    /**
     * 停用配置（软删除）
     */
    @Query("UPDATE meter_name_configs SET is_active = 0 WHERE default_name = :defaultName AND tenant_room_number = :tenantRoomNumber")
    suspend fun deactivateConfigForTenant(defaultName: String, tenantRoomNumber: String)
    
    /**
     * 停用全局配置（软删除）
     */
    @Query("UPDATE meter_name_configs SET is_active = 0 WHERE default_name = :defaultName AND tenant_room_number = ''")
    suspend fun deactivateConfig(defaultName: String)
    
    /**
     * 根据类型删除所有配置
     */
    @Query("DELETE FROM meter_name_configs WHERE meter_type = :type")
    suspend fun deleteConfigsByType(type: String)
    
    /**
     * 检查配置是否存在
     */
    @Query("SELECT COUNT(*) FROM meter_name_configs WHERE default_name = :defaultName AND tenant_room_number = :tenantRoomNumber AND is_active = 1")
    suspend fun configExistsForTenant(defaultName: String, tenantRoomNumber: String): Int
    
    /**
     * 检查全局配置是否存在
     */
    @Query("SELECT COUNT(*) FROM meter_name_configs WHERE default_name = :defaultName AND tenant_room_number = '' AND is_active = 1")
    suspend fun configExists(defaultName: String): Int
}