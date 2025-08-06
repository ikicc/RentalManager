package com.morgen.rentalmanager.myapplication

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 数据同步管理器
 * 用于在不同组件之间传递数据变更事件
 */
object DataSyncManager {

    // 租户数据变更事件
    private val _tenantDataChanged = MutableSharedFlow<String>(replay = 0)
    val tenantDataChanged = _tenantDataChanged.asSharedFlow()

    // 价格数据变更事件
    private val _priceDataChanged = MutableSharedFlow<Unit>(replay = 0)
    val priceDataChanged = _priceDataChanged.asSharedFlow()

    // 隐私关键字变更事件
    private val _privacyKeywordsChanged = MutableSharedFlow<List<String>>(replay = 0)
    val privacyKeywordsChanged = _privacyKeywordsChanged.asSharedFlow()
    
    // 表名变更事件（表默认名称，自定义名称，租户房间号）
    private val _meterNameChanged = MutableSharedFlow<Triple<String, String, String>>(replay = 0)
    val meterNameChanged = _meterNameChanged.asSharedFlow()
    
    // 账单数据变更事件（租户房间号，月份）
    private val _billDataChanged = MutableSharedFlow<Pair<String, String>>(replay = 0)
    val billDataChanged = _billDataChanged.asSharedFlow()

    // 通知租户数据变更
    suspend fun notifyTenantDataChanged(tenantRoomNumber: String) {
        _tenantDataChanged.emit(tenantRoomNumber)
    }

    // 通知价格数据变更
    suspend fun notifyPriceDataChanged() {
        _priceDataChanged.emit(Unit)
    }
    
    // 通知隐私关键字变更
    suspend fun notifyPrivacyKeywordsChanged(keywords: List<String>) {
        _privacyKeywordsChanged.emit(keywords)
    }
    
    // 通知表名变更
    suspend fun notifyMeterNameChanged(defaultName: String, customName: String, tenantRoomNumber: String = "") {
        _meterNameChanged.emit(Triple(defaultName, customName, tenantRoomNumber))
    }
    
    // 通知账单数据变更
    suspend fun notifyBillDataChanged(tenantRoomNumber: String, month: String) {
        _billDataChanged.emit(Pair(tenantRoomNumber, month))
    }
}