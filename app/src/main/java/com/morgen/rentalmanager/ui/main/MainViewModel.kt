package com.morgen.rentalmanager.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.morgen.rentalmanager.myapplication.Tenant
import com.morgen.rentalmanager.myapplication.TenantRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import com.morgen.rentalmanager.myapplication.BillWithDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log

class MainViewModel(private val repository: TenantRepository) : ViewModel() {
    // 优化StateFlow配置，减少停止/启动开销
    val allTenants: StateFlow<List<Tenant>> = repository.allTenants
        .stateIn(
            scope = viewModelScope,
            // 使用更长的超时以防止频繁重订阅
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // 移除预加载相关状态
    
    // 当前月份
    private val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    

    
    // 移除预加载相关方法
    
    // 移除缓存相关方法

    suspend fun getBillForTenantByMonth(roomNumber: String, month: String): BillWithDetails? {
        try {
            // 直接从数据库获取，不使用缓存，确保数据是最新的
            val bill = repository.getBillWithDetailsByTenantAndMonth(roomNumber, month)
            return bill
        } catch (e: Exception) {
            Log.e("MainViewModel", "获取账单失败: ${e.message}")
            return null
        }
    }

    suspend fun getBillWithDetailsForTenantByMonth(roomNumber: String, month: String): BillWithDetails? {
        return getBillForTenantByMonth(roomNumber, month)
    }
    
    // 移除初始化预加载
}

class MainViewModelFactory(private val repository: TenantRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 