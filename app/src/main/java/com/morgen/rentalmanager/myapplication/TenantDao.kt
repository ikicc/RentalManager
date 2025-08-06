package com.morgen.rentalmanager.myapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface TenantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenant(tenant: Tenant)

    @Update
    suspend fun updateTenant(tenant: Tenant)

    @Delete
    suspend fun deleteTenant(tenant: Tenant)

    @Query("SELECT * FROM tenants ORDER BY room_number ASC")
    fun getAllTenants(): Flow<List<Tenant>>

    @Query("SELECT * FROM tenants WHERE room_number = :roomNumber")
    fun getTenantById(roomNumber: String): Flow<Tenant?>
    
    @Query("SELECT COUNT(*) FROM tenants")
    suspend fun getTenantCount(): Int
    
    // 一次性获取所有租户（非Flow）
    @Query("SELECT * FROM tenants ORDER BY room_number ASC")
    suspend fun getAllTenantsOnce(): List<Tenant>
    
    // 获取最近的N个租户
    @Query("SELECT * FROM tenants ORDER BY room_number ASC LIMIT :limit")
    suspend fun getRecentTenants(limit: Int): List<Tenant>
} 