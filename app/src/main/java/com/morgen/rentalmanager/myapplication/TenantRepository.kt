package com.morgen.rentalmanager.myapplication

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray

class TenantRepository(
    private val tenantDao: TenantDao,
    private val billDao: BillDao,
    private val priceDao: PriceDao,
    private val meterNameConfigDao: MeterNameConfigDao
) {
    
    // 名称管理器
    private val meterNameManager = com.morgen.rentalmanager.utils.MeterNameManager(meterNameConfigDao)
    // 移除内存缓存，直接使用Flow提供实时数据

    // Tenant methods
    val allTenants: Flow<List<Tenant>> = tenantDao.getAllTenants()

    fun getTenantById(roomNumber: String): Flow<Tenant?> = tenantDao.getTenantById(roomNumber)

    suspend fun insert(tenant: Tenant) {
        tenantDao.insertTenant(tenant)
    }

    suspend fun update(tenant: Tenant) {
        tenantDao.updateTenant(tenant)
        // 通知租户数据变更
        DataSyncManager.notifyTenantDataChanged(tenant.roomNumber)
    }
    
    /**
     * 重新计算指定租户的所有账单
     * 当租金发生变化时调用，确保所有账单数据同步更新
     */
    suspend fun recalculateAllBillsForTenant(roomNumber: String) = withContext(Dispatchers.IO) {
        android.util.Log.d("TenantRepository", "开始重新计算租户 $roomNumber 的所有账单")
        
        try {
            // 获取该租户的所有账单
            val allBills = billDao.getAllBillsWithDetails().first()
            val tenantBills = allBills.filter { it.bill.tenantRoomNumber == roomNumber }
            
            android.util.Log.d("TenantRepository", "找到 ${tenantBills.size} 个账单需要重新计算")
            
            // 获取当前价格，确保使用数据库中的最新价格
            var currentPrice = priceDao.getPrice()
            if (currentPrice == null) {
                // 如果数据库中没有价格记录，创建并保存默认价格
                currentPrice = Price(water = 4.0, electricity = 1.0)
                priceDao.save(currentPrice)
                android.util.Log.d("TenantRepository", "recalculateAllBillsForTenant - 已保存默认价格到数据库: $currentPrice")
            }
            android.util.Log.d("TenantRepository", "使用价格进行重新计算: 水价=${currentPrice.water}, 电价=${currentPrice.electricity}")
            
            // 重新计算每个账单
            tenantBills.forEach { billWithDetails ->
                val updatedDetails = billWithDetails.details.map { detail ->
                    when (detail.type) {
                        "water" -> {
                            val usage = detail.usage ?: 0.0
                            val newAmount = usage * currentPrice.water
                            detail.copy(
                                pricePerUnit = currentPrice.water,
                                amount = newAmount
                            )
                        }
                        "electricity" -> {
                            val usage = detail.usage ?: 0.0
                            val newAmount = usage * currentPrice.electricity
                            detail.copy(
                                pricePerUnit = currentPrice.electricity,
                                amount = newAmount
                            )
                        }
                        else -> detail // 其他费用保持不变
                    }
                }
                
                // 重新计算总金额
                val newTotalAmount = updatedDetails.sumOf { it.amount }
                val updatedBill = billWithDetails.bill.copy(totalAmount = newTotalAmount)
                
                // 保存更新后的账单
                billDao.upsertBillTransaction(updatedBill, updatedDetails)
                
                android.util.Log.d("TenantRepository", "已更新账单: ${updatedBill.month}, 新总金额: $newTotalAmount")
            }
            
            // 通知数据变更
            DataSyncManager.notifyTenantDataChanged(roomNumber)
            DataSyncManager.notifyPriceDataChanged()
            
            android.util.Log.d("TenantRepository", "租户 $roomNumber 的所有账单重新计算完成")
            
        } catch (e: Exception) {
            android.util.Log.e("TenantRepository", "重新计算账单失败", e)
        }
    }

    suspend fun delete(tenant: Tenant) {
        billDao.deleteBillsByTenant(tenant.roomNumber)
        tenantDao.deleteTenant(tenant)
    }

    // Price methods
    val priceFlow: Flow<Price> = priceDao.getPriceFlow()
        .map { price ->
            if (price == null) {
                // 如果数据库中没有价格记录，返回默认价格
                // 注意：在Flow的map中不应该执行异步操作
                val defaultPrice = Price(water = 4.0, electricity = 1.0)
                android.util.Log.d("TenantRepository", "使用默认价格: $defaultPrice")
                defaultPrice
            } else {
                price
            }
        }
    
    // Privacy keywords flow - 实时监听隐私关键字变化
    val privacyKeywordsFlow: Flow<List<String>> = priceDao.getPriceFlow()
        .map { price ->
            android.util.Log.d("TenantRepository", "privacyKeywordsFlow - price updated: $price")
            if (price == null) {
                emptyList()
            } else {
                try {
                    val jsonArray = JSONArray(price.privacyKeywords)
                    val keywords = mutableListOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        keywords.add(jsonArray.getString(i))
                    }
                    android.util.Log.d("TenantRepository", "privacyKeywordsFlow - parsed keywords: $keywords")
                    keywords
                } catch (e: Exception) {
                    android.util.Log.e("TenantRepository", "privacyKeywordsFlow - parsing failed", e)
                    emptyList()
                }
            }
        }
        .distinctUntilChanged() // 只有当关键字真正变化时才发出新值

    suspend fun getCurrentPrice(): Price? = priceDao.getPrice()
    
    // 获取所有价格数据（用于预热）
    suspend fun getAllPrices(): Price = withContext(Dispatchers.IO) {
        var price = priceDao.getPrice()
        if (price == null) {
            // 如果数据库中没有价格记录，创建并保存默认价格
            price = Price(water = 4.0, electricity = 1.0)
            priceDao.save(price)
            android.util.Log.d("TenantRepository", "getAllPrices - 已保存默认价格到数据库: $price")
        }
        price
    }
    
    // 确保默认价格存在的方法
    suspend fun ensureDefaultPriceExists() = withContext(Dispatchers.IO) {
        val price = priceDao.getPrice()
        if (price == null) {
            val defaultPrice = Price(water = 4.0, electricity = 1.0)
            priceDao.save(defaultPrice)
            android.util.Log.d("TenantRepository", "已保存默认价格到数据库: $defaultPrice")
        }
    }

    suspend fun savePrice(water: Double, electricity: Double) = withContext(Dispatchers.IO) {
        android.util.Log.d("TenantRepository", "保存价格: 水价=$water, 电价=$electricity")
        
        // 获取当前价格记录，保留隐私关键字
        val currentPrice = priceDao.getPrice()
        val privacyKeywords = currentPrice?.privacyKeywords ?: "[]"
        
        // 检查价格是否发生变化
        val priceChanged = currentPrice == null || 
                          currentPrice.water != water || 
                          currentPrice.electricity != electricity
        
        val newPrice = Price(water = water, electricity = electricity, privacyKeywords = privacyKeywords)
        priceDao.save(newPrice)
        
        android.util.Log.d("TenantRepository", "价格保存完成: $newPrice")
        
        // 如果价格发生变化，重新计算所有租户的所有账单
        if (priceChanged) {
            android.util.Log.d("TenantRepository", "价格发生变化，开始重新计算所有账单")
            recalculateAllBills()
        }
        
        // 小延迟确保数据库事务完成
        kotlinx.coroutines.delay(100)
        
        // 通知价格数据变更
        DataSyncManager.notifyPriceDataChanged()
        android.util.Log.d("TenantRepository", "已通知价格数据变更")
    }
    
    /**
     * 重新计算所有租户的所有账单
     * 当水电价格发生变化时调用
     */
    private suspend fun recalculateAllBills() = withContext(Dispatchers.IO) {
        android.util.Log.d("TenantRepository", "开始重新计算所有租户的所有账单")
        
        try {
            // 获取所有租户
            val allTenants = tenantDao.getAllTenants().first()
            
            // 为每个租户重新计算账单
            allTenants.forEach { tenant ->
                recalculateAllBillsForTenant(tenant.roomNumber)
            }
            
            android.util.Log.d("TenantRepository", "所有租户的账单重新计算完成")
            
        } catch (e: Exception) {
            android.util.Log.e("TenantRepository", "重新计算所有账单失败", e)
        }
    }

    // Bill methods
    fun getAllBillsWithDetails(): Flow<List<BillWithDetails>> = billDao.getAllBillsWithDetails()

    fun getBillWithDetailsByTenantAndMonthFlow(roomNumber: String, month: String): Flow<BillWithDetails?> =
        billDao.getBillWithDetailsByTenantAndMonthFlow(roomNumber, month)

    suspend fun getBillWithDetailsByTenantAndMonth(roomNumber: String, month: String): BillWithDetails? =
        billDao.getBillWithDetailsByTenantAndMonth(roomNumber, month)

    suspend fun saveBill(bill: Bill, details: List<BillDetail>) {
        billDao.upsertBillTransaction(bill, details)
        
        // 通知数据变更
        DataSyncManager.notifyBillDataChanged(bill.tenantRoomNumber, bill.month)
    }
    
    /**
     * 保存账单并自动备份
     * @param context 应用上下文，用于执行自动备份
     * @param bill 账单对象
     * @param details 账单明细列表
     */
    suspend fun saveBillWithAutoBackup(context: android.content.Context, bill: Bill, details: List<BillDetail>) {
        // 先保存账单
        billDao.upsertBillTransaction(bill, details)
        
        // 通知数据变更
        DataSyncManager.notifyBillDataChanged(bill.tenantRoomNumber, bill.month)
        
        // 执行自动备份
        try {
            com.morgen.rentalmanager.utils.AutoBackupManager.performAutoBackup(context, this)
        } catch (e: Exception) {
            android.util.Log.e("TenantRepository", "自动备份失败", e)
            // 自动备份失败不影响正常流程
        }
    }
    
    // 新增方法用于账单详情页面
    suspend fun getTenantByRoomNumber(roomNumber: String): Tenant? = 
        withContext(Dispatchers.IO) {
            tenantDao.getTenantById(roomNumber).first()
        }
    
    suspend fun getBillByRoomAndMonth(roomNumber: String, month: String): Bill? = 
        withContext(Dispatchers.IO) {
            billDao.getBillByTenantAndMonth(roomNumber, month)
        }
    
    suspend fun getBillDetailsByBillId(billId: Int): List<BillDetail> = 
        withContext(Dispatchers.IO) {
            billDao.getBillDetailsByBillId(billId)
        }
    
    // Privacy protection methods
    suspend fun getPrivacyKeywords(): List<String> = withContext(Dispatchers.IO) {
        // 每次都从数据库重新查询，确保获取最新数据
        val queryTime = System.currentTimeMillis()
        android.util.Log.d("TenantRepository", "getPrivacyKeywords - 开始新查询，时间戳: $queryTime")
        
        // 强制重新查询数据库
        var price = priceDao.getPrice()
        if (price == null) {
            // 如果Price表为空，创建默认记录
            price = Price(water = 4.0, electricity = 1.0, privacyKeywords = "[]")
            priceDao.save(price)
            android.util.Log.d("TenantRepository", "getPrivacyKeywords - 已保存默认价格记录到数据库: $price")
        }
        
        android.util.Log.d("TenantRepository", "getPrivacyKeywords - 查询结果: $price")
        android.util.Log.d("TenantRepository", "getPrivacyKeywords - privacyKeywords string: '${price.privacyKeywords}'")
        
        try {
            val jsonArray = JSONArray(price.privacyKeywords)
            val keywords = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                keywords.add(jsonArray.getString(i))
            }
            android.util.Log.d("TenantRepository", "getPrivacyKeywords - 解析结果: $keywords (查询时间: $queryTime)")
            keywords
        } catch (e: Exception) {
            android.util.Log.e("TenantRepository", "getPrivacyKeywords - parsing failed", e)
            emptyList()
        }
    }

    suspend fun savePrivacyKeywords(keywords: List<String>) = withContext(Dispatchers.IO) {
        var currentPrice = priceDao.getPrice()
        if (currentPrice == null) {
            // 如果Price表为空，创建默认记录
            currentPrice = Price(water = 4.0, electricity = 1.0, privacyKeywords = "[]")
            priceDao.save(currentPrice)
            android.util.Log.d("TenantRepository", "savePrivacyKeywords - 已保存默认价格记录到数据库: $currentPrice")
        }
        android.util.Log.d("TenantRepository", "savePrivacyKeywords - input keywords: $keywords")
        val jsonArray = JSONArray()
        keywords.forEach { jsonArray.put(it) }
        val jsonString = jsonArray.toString()
        android.util.Log.d("TenantRepository", "savePrivacyKeywords - JSON string: '$jsonString'")
        val updatedPrice = currentPrice.copy(privacyKeywords = jsonString)
        android.util.Log.d("TenantRepository", "savePrivacyKeywords - updated price: $updatedPrice")
        priceDao.save(updatedPrice)
        android.util.Log.d("TenantRepository", "savePrivacyKeywords - saved to database")
        
        // 强制等待一小段时间，确保数据库事务完成
        kotlinx.coroutines.delay(100)
        
        // 验证数据是否真的保存成功
        val verifyPrice = priceDao.getPrice()
        android.util.Log.d("TenantRepository", "savePrivacyKeywords - verify saved: ${verifyPrice?.privacyKeywords}")
        
        // 通知数据变更
        DataSyncManager.notifyPrivacyKeywordsChanged(keywords)
    }

    fun applyPrivacyProtection(roomNumber: String, keywords: List<String>): String {
        return com.morgen.rentalmanager.utils.PrivacyProtectionUtils.applyPrivacyProtection(roomNumber, keywords)
    }
    
    /**
     * 专门用于收据生成的数据获取方法
     * 确保每次都获取最新的租户信息
     */
    suspend fun getTenantForReceipt(roomNumber: String): Tenant? = withContext(Dispatchers.IO) {
        val queryTime = System.currentTimeMillis()
        android.util.Log.d("TenantRepository", "getTenantForReceipt - 开始查询租户: $roomNumber, 时间戳: $queryTime")
        
        val tenant = getTenantById(roomNumber).first()
        android.util.Log.d("TenantRepository", "getTenantForReceipt - 查询结果: $tenant (查询时间: $queryTime)")
        
        tenant
    }
    
    // Meter name management methods
    suspend fun getMeterDisplayName(defaultName: String, tenantRoomNumber: String = ""): String {
        return meterNameManager.getDisplayName(defaultName, tenantRoomNumber)
    }

    suspend fun saveMeterCustomName(defaultName: String, customName: String, meterType: String, tenantRoomNumber: String = ""): Result<Unit> {
        android.util.Log.d("TenantRepository", "开始保存自定义表名: defaultName='$defaultName', customName='$customName', meterType='$meterType', tenantRoomNumber='$tenantRoomNumber'")
        
        // 判断表名是否符合可自定义规则（包含"水表"或"电表"，且不是主表）
        val containsWaterMeter = defaultName.contains("水表") 
        val containsElectricityMeter = defaultName.contains("电表")
        val isMainMeter = defaultName == "主水表" || defaultName == "主电表"
        val isExtraMeter = (containsWaterMeter || containsElectricityMeter) && !isMainMeter
        
        if (!isExtraMeter) {
            android.util.Log.w("TenantRepository", "表名不符合自定义规则，跳过: $defaultName")
            // 尝试用宽松的规则保存
            android.util.Log.d("TenantRepository", "尝试用宽松的规则保存表名配置")
        }
        
        // 使用MeterNameManager保存配置
        val result = meterNameManager.saveCustomName(defaultName, customName, meterType, tenantRoomNumber)
        
        // 记录结果
        if (result.isSuccess) {
            android.util.Log.d("TenantRepository", "表名配置保存成功: $defaultName -> $customName")
        } else {
            android.util.Log.e("TenantRepository", "表名配置保存失败: ${result.exceptionOrNull()?.message}")
        }
        
        return result
    }

    suspend fun resetMeterNameToDefault(defaultName: String, tenantRoomNumber: String = ""): Result<Unit> {
        return meterNameManager.resetToDefault(defaultName, tenantRoomNumber)
    }

    fun validateMeterCustomName(name: String): Boolean {
        return meterNameManager.validateCustomName(name)
    }

    suspend fun getAllMeterNameConfigs(): List<MeterNameConfig> {
        return meterNameManager.getAllActiveConfigs()
    }

    suspend fun getMeterConfigsByTenant(tenantRoomNumber: String): List<MeterNameConfig> {
        return meterNameManager.getConfigsByTenant(tenantRoomNumber)
    }

    suspend fun getMeterConfigsByType(meterType: String): List<MeterNameConfig> {
        return meterNameManager.getConfigsByType(meterType)
    }

    suspend fun getMeterConfigsByTypeAndTenant(meterType: String, tenantRoomNumber: String): List<MeterNameConfig> {
        return meterNameManager.getConfigsByTypeAndTenant(meterType, tenantRoomNumber)
    }
} 