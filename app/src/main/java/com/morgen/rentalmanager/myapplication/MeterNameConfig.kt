package com.morgen.rentalmanager.myapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 水表/电表自定义名称配置实体
 * 用于存储用户自定义的水表和电表名称
 */
@Entity(tableName = "meter_name_configs")
data class MeterNameConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    @ColumnInfo(name = "meter_type")
    val meterType: String, // "water" or "electricity"
    
    @ColumnInfo(name = "default_name")
    val defaultName: String, // "额外水表1", "额外电表1"
    
    @ColumnInfo(name = "custom_name")
    val customName: String, // 用户自定义名称
    
    @ColumnInfo(name = "tenant_room_number")
    val tenantRoomNumber: String = "", // 关联的租户房间号，空字符串表示全局配置
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "created_date")
    val createdDate: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_date")
    val updatedDate: Long = System.currentTimeMillis()
)