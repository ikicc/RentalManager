package com.morgen.rentalmanager.ui.addbillall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.morgen.rentalmanager.myapplication.Bill
import com.morgen.rentalmanager.myapplication.BillDetail
import com.morgen.rentalmanager.myapplication.Tenant
import com.morgen.rentalmanager.myapplication.TenantRepository
import com.morgen.rentalmanager.utils.AmountFormatter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

// Prices are dynamic

// Represents a single meter (main or sub)
data class MeterInput(
    val id: String = UUID.randomUUID().toString(), // Unique ID for UI stability
    val type: String, // "water" or "electricity"
    val name: String, // e.g., "主水表", "厨房水表" (display name, may be custom)
    val originalName: String = name, // Original name for database storage
    val isPrimary: Boolean = false, // Is this the main meter?
    val previousReading: String = "",
    val currentReading: String = "",
    val usage: String = "", // String to allow empty input
    val amount: String = "", // String to allow empty input
    val isPrevEditable: Boolean = false,
    val isAmountManual: Boolean = false, // Flag to check if amount was manually entered
    val isUsageManual: Boolean = false // Flag to check if usage was manually entered
)

// Represents a single extra fee
data class ExtraFee(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val amount: String = ""
)

// Represents the complete bill state for one tenant
data class BillInputState(
    val roomNumber: String,
    val tenantName: String,
    val meters: List<MeterInput> = emptyList(),
    val extraFees: List<ExtraFee> = emptyList()
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AddBillAllViewModel(
    private val repository: TenantRepository
) : ViewModel() {

    // 添加Context属性，用于自动备份
    var context: android.content.Context? = null
        private set
    
    // 设置Context的方法
    fun setContext(ctx: android.content.Context) {
        context = ctx
    }

    private val _selectedMonth = MutableStateFlow(
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Calendar.getInstance().time)
    )
    val selectedMonth: StateFlow<String> = _selectedMonth

    private val _uiState = MutableStateFlow<List<BillInputState>>(emptyList())
    val uiState: StateFlow<List<BillInputState>> = _uiState

    private var waterPrice = 0.0 // 初始值设为0，等待从数据库加载
    private var elecPrice = 0.0 // 初始值设为0，等待从数据库加载

    init {
        viewModelScope.launch {
            repository.priceFlow.collect { price ->
                val firstLoad = _uiState.value.isEmpty()
                val oldWaterPrice = waterPrice
                val oldElecPrice = elecPrice
                
                waterPrice = price.water
                elecPrice = price.electricity
                
                android.util.Log.d("AddBillAllViewModel", "价格更新: 水价 $oldWaterPrice -> $waterPrice, 电价 $oldElecPrice -> $elecPrice")

                if (firstLoad) {
                    // 首次获取价格，加载所有数据
                    android.util.Log.d("AddBillAllViewModel", "首次加载数据")
                    loadData()
                } else {
                    // 价格更新，仅根据最新单价重新计算金额，保留用户输入
                    android.util.Log.d("AddBillAllViewModel", "价格变更，重新计算金额")
                    _uiState.update { currentStates ->
                        currentStates.map { state ->
                            val updatedMeters = state.meters.map { meter ->
                                calculateUsageAndAmount(meter)
                            }
                            state.copy(meters = updatedMeters)
                        }
                    }
                }
            }
        }
        
        // 额外监听价格变更事件，确保立即响应
        viewModelScope.launch {
            com.morgen.rentalmanager.myapplication.DataSyncManager.priceDataChanged.collect {
                android.util.Log.d("AddBillAllViewModel", "收到价格变更通知，强制刷新")
                // 强制重新获取最新价格
                val latestPrice = repository.priceFlow.first()
                waterPrice = latestPrice.water
                elecPrice = latestPrice.electricity
                
                // 重新计算所有金额
                _uiState.update { currentStates ->
                    currentStates.map { state ->
                        val updatedMeters = state.meters.map { meter ->
                            calculateUsageAndAmount(meter)
                        }
                        state.copy(meters = updatedMeters)
                    }
                }
            }
        }
        
        // 监听租户数据变更事件，确保租金变更后立即刷新
        viewModelScope.launch {
            com.morgen.rentalmanager.myapplication.DataSyncManager.tenantDataChanged.collect { roomNumber ->
                android.util.Log.d("AddBillAllViewModel", "收到租户数据变更通知: $roomNumber，重新加载数据")
                // 重新加载所有数据，确保显示最新的租户信息和账单数据
                loadData()
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            val tenants = repository.allTenants.first()
            val states = tenants.map { tenant ->
                createStateForTenant(tenant, _selectedMonth.value)
            }
            _uiState.value = states
        }
    }

    private suspend fun createStateForTenant(tenant: Tenant, month: String): BillInputState {
        val currentBill = repository.getBillWithDetailsByTenantAndMonth(tenant.roomNumber, month)
        if (currentBill != null) {
            // 即使本月账单已存在，也要取上月账单的 currentReading 作为"上月读数"，
            // 以便用户修改了上月后，这里能同步显示
            val prevMonth = getPreviousMonth(month)
            val prevBill = repository.getBillWithDetailsByTenantAndMonth(tenant.roomNumber, prevMonth)

            val meters = currentBill.details.filter { it.type in listOf("water", "electricity") }.map { d ->
                val prevReading = prevBill?.details
                    ?.firstOrNull { it.type == d.type && it.name == d.name }
                    ?.currentReading ?: d.previousReading ?: 0.0

                // 获取显示名称
                val displayName = try {
                    // 使用更宽松的表名识别规则
                    val containsWaterMeter = d.name.contains("水表") 
                    val containsElectricityMeter = d.name.contains("电表")
                    val isMainMeter = d.name == "主水表" || d.name == "主电表"
                    val isExtraMeter = (containsWaterMeter || containsElectricityMeter) && !isMainMeter
                    
                    if (isExtraMeter) {
                        val customName = repository.getMeterDisplayName(d.name, tenant.roomNumber)
                        if (customName.isBlank()) {
                            android.util.Log.w("AddBillAllViewModel", "加载已有账单，获取到的自定义名称为空，使用原名称: '${d.name}'")
                            d.name
                        } else {
                            android.util.Log.d("AddBillAllViewModel", "加载已有账单，获取到自定义名称: '${d.name}' -> '$customName'")
                            customName
                        }
                    } else {
                        android.util.Log.d("AddBillAllViewModel", "加载已有账单，主表不使用自定义名称: '${d.name}'")
                        d.name
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AddBillAllViewModel", "加载已有账单，获取自定义名称失败，使用原名称: '${d.name}'", e)
                    d.name
                }

                MeterInput(
                    type = d.type,
                    name = displayName,
                    originalName = d.name,
                    isPrimary = d.name.startsWith("主"),
                    previousReading = AmountFormatter.formatUsage(prevReading),
                    currentReading = d.currentReading?.let { AmountFormatter.formatUsage(it) } ?: "",
                    usage = d.usage?.let { AmountFormatter.formatUsage(it) } ?: "",
                    amount = AmountFormatter.formatAmount(d.amount)
                )
            }
            val fees = currentBill.details.filter { it.type == "extra" }.map { ExtraFee(name = it.name, amount = AmountFormatter.formatAmount(it.amount)) }
            return BillInputState(tenant.roomNumber, tenant.name, meters, fees)
        }

        val prevMonth = getPreviousMonth(month)
        val prevBill = repository.getBillWithDetailsByTenantAndMonth(tenant.roomNumber, prevMonth)
        val metersAndFees = if (prevBill != null) {
            val metersList = prevBill.details.filter { it.type in listOf("water", "electricity") }.map { d ->
                val lastReading = d.currentReading ?: d.previousReading ?: 0.0
                
                // 获取显示名称（如果有自定义名称则使用自定义名称）
                val displayName = try {
                    android.util.Log.d("AddBillAllViewModel", "获取自定义表名: ${d.name} (租户: ${tenant.roomNumber})")
                    repository.getMeterDisplayName(d.name, tenant.roomNumber)
                } catch (e: Exception) {
                    android.util.Log.w("AddBillAllViewModel", "获取自定义名称失败，使用原名称: ${d.name}", e)
                    d.name
                }
                
                MeterInput(
                    type = d.type,
                    name = displayName,
                    originalName = d.name,
                    isPrimary = d.name.startsWith("主"),
                    previousReading = AmountFormatter.formatUsage(lastReading)
                )
            }
            Pair(metersList, prevBill.details.filter { it.type == "extra" }.map { ExtraFee(name = it.name) })
        } else {
            Pair(
            listOf(
                    MeterInput(type = "water", name = "主水表", originalName = "主水表", isPrimary = true, previousReading = AmountFormatter.formatUsage(0.0)),
                    MeterInput(type = "electricity", name = "主电表", originalName = "主电表", isPrimary = true, previousReading = AmountFormatter.formatUsage(0.0))
                ),
                emptyList<ExtraFee>()
            )
        }
        return BillInputState(tenant.roomNumber, tenant.name, metersAndFees.first, metersAndFees.second)
    }

    private fun getPreviousMonth(month: String): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val calendar = Calendar.getInstance().apply {
            time = sdf.parse(month) ?: Date()
            add(Calendar.MONTH, -1)
        }
        return sdf.format(calendar.time)
    }

    fun onMonthSelected(year: Int, month: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
        }
        _selectedMonth.value = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
        loadData()
    }

    private fun calculateUsageAndAmount(meter: MeterInput): MeterInput {
        if (meter.isUsageManual || meter.isAmountManual) {
            return meter
        }
        val currentReading = meter.currentReading.toDoubleOrNull()
        val previousReading = meter.previousReading.toDoubleOrNull() ?: 0.0
        if (currentReading != null && currentReading >= previousReading) {
            val usage = currentReading - previousReading
            val price = if (meter.type == "water") waterPrice else elecPrice
            val amount = usage * price
            return meter.copy(
                usage = AmountFormatter.formatUsage(usage),
                amount = AmountFormatter.formatAmount(amount)
            )
        }
        return meter.copy(usage = "", amount = "")
    }

    fun onMeterReadingChange(roomNumber: String, meterId: String, newReading: String) {
        _uiState.update { currentStates ->
            currentStates.map { state ->
                if (state.roomNumber == roomNumber) {
                    val updatedMeters = state.meters.map { meter ->
                        if (meter.id == meterId) {
                            val updatedMeter = meter.copy(
                                currentReading = newReading,
                                isUsageManual = false,
                                isAmountManual = false
                            )
                            calculateUsageAndAmount(updatedMeter)
                        } else meter
                    }
                    state.copy(meters = updatedMeters)
                } else state
            }
        }
    }

    fun onMeterUsageChange(roomNumber: String, meterId: String, newUsage: String) {
        _uiState.update { currentStates ->
            currentStates.map { state ->
                if (state.roomNumber == roomNumber) {
                    val updatedMeters = state.meters.map { meter ->
                        if (meter.id == meterId) {
                            val usage = newUsage.toDoubleOrNull()
                            val price = if (meter.type == "water") waterPrice else elecPrice
                            val amount = if (usage != null) AmountFormatter.formatAmount(usage * price) else ""
                            meter.copy(usage = newUsage, amount = amount, isUsageManual = true, isAmountManual = false)
                        } else meter
                    }
                    state.copy(meters = updatedMeters)
                } else state
            }
        }
    }

    fun onMeterAmountChange(roomNumber: String, meterId: String, newAmount: String) {
        _uiState.update { currentStates ->
            currentStates.map { state ->
                if (state.roomNumber == roomNumber) {
                    val updatedMeters = state.meters.map { meter ->
                        if (meter.id == meterId) {
                            meter.copy(amount = newAmount, isAmountManual = true)
                        } else meter
                    }
                    state.copy(meters = updatedMeters)
                } else state
            }
        }
    }

    fun addMeter(roomNumber: String, type: String) {
        viewModelScope.launch {
            _uiState.update { currentStates ->
                currentStates.map { state ->
                    if (state.roomNumber == roomNumber) {
                        val base = if (type == "water") "额外水表" else "额外电表"
                        val index = state.meters.count { it.type == type && it.name.startsWith(base) } + 1
                        val defaultName = "$base$index"
                        
                        // 使用更宽松的表名识别规则
                        val containsWaterMeter = defaultName.contains("水表") 
                        val containsElectricityMeter = defaultName.contains("电表")
                        val isMainMeter = defaultName == "主水表" || defaultName == "主电表"
                        val isExtraMeter = (containsWaterMeter || containsElectricityMeter) && !isMainMeter
                        
                        // 只有额外表可以使用自定义名称
                        val displayName = if (isExtraMeter) {
                            try {
                                android.util.Log.d("AddBillAllViewModel", "准备获取水电表自定义名称: '${defaultName}'")
                                val customName = repository.getMeterDisplayName(defaultName, roomNumber)
                                
                                if (customName.isBlank()) {
                                    android.util.Log.w("AddBillAllViewModel", "获取到的自定义名称为空，使用默认名称: '$defaultName'")
                                    defaultName
                                } else {
                                    android.util.Log.d("AddBillAllViewModel", "获取到自定义名称: '${defaultName}' -> '$customName'")
                                    customName
                                }
                        } catch (e: Exception) {
                                android.util.Log.w("AddBillAllViewModel", "获取自定义名称失败，使用默认名称: '$defaultName'", e)
                                defaultName
                            }
                        } else {
                            android.util.Log.d("AddBillAllViewModel", "主表不使用自定义名称: '$defaultName'")
                            defaultName
                        }

                        val newMeter = MeterInput(
                            type = type,
                            name = displayName,
                            originalName = defaultName
                        )
                        state.copy(meters = state.meters + newMeter)
                    } else state
                }
            }
        }
    }

    fun removeMeter(roomNumber: String, meterId: String) {
        _uiState.update { currentStates ->
            currentStates.map { state ->
                if (state.roomNumber == roomNumber) {
                    state.copy(meters = state.meters.filterNot { it.id == meterId })
                } else state
            }
        }
    }

    fun onExtraFeeNameChange(roomNumber: String, feeId: String, newName: String) {
        _uiState.update { currentStates ->
            currentStates.map { state ->
                if (state.roomNumber == roomNumber) {
                    val updatedFees = state.extraFees.map { fee ->
                        if (fee.id == feeId) fee.copy(name = newName) else fee
                    }
                    state.copy(extraFees = updatedFees)
                } else state
            }
        }
    }

    fun onExtraFeeAmountChange(roomNumber: String, feeId: String, newAmount: String) {
        _uiState.update { currentStates ->
            currentStates.map { state ->
                if (state.roomNumber == roomNumber) {
                    val updatedFees = state.extraFees.map { fee ->
                        if (fee.id == feeId) fee.copy(amount = newAmount) else fee
                    }
                    state.copy(extraFees = updatedFees)
                } else state
            }
        }
    }

    fun addExtraFee(roomNumber: String) {
        _uiState.update { currentStates ->
            currentStates.map { state ->
                if (state.roomNumber == roomNumber) {
                    state.copy(extraFees = state.extraFees + ExtraFee())
                } else state
            }
        }
    }

    fun removeExtraFee(roomNumber: String, feeId: String) {
        _uiState.update { currentStates ->
            currentStates.map { state ->
                if (state.roomNumber == roomNumber) {
                    state.copy(extraFees = state.extraFees.filterNot { it.id == feeId })
                } else state
            }
        }
    }

    fun updateWaterReading(roomNumber: String, reading: String) {
        // This will be replaced with more granular update functions
    }

    fun updateElectricityReading(roomNumber: String, reading: String) {
        // This will be replaced with more granular update functions
    }

    fun saveBills() {
        viewModelScope.launch {
            val month = _selectedMonth.value
            // 获取所有租户信息
            val tenants = repository.allTenants.first()
            val tenantsMap = tenants.associateBy { it.roomNumber }
            
            _uiState.value.forEach { state ->
                val details = mutableListOf<BillDetail>()
                state.meters.forEach { meter ->
                    val prev = meter.previousReading.toDoubleOrNull() ?: 0.0
                    val currInput = meter.currentReading.toDoubleOrNull()
                    val usageCalc = if(currInput!=null) (currInput - prev).coerceAtLeast(0.0) else 0.0
                    val usage = meter.usage.toDoubleOrNull() ?: usageCalc
                    details.add(
                        BillDetail(
                            parentBillId = 0,
                            type = meter.type,
                            name = meter.originalName, // 使用原始名称保存到数据库
                            previousReading = prev,
                            currentReading = currInput,
                            usage = usage,
                            pricePerUnit = if (meter.type == "water") waterPrice else elecPrice,
                            amount = meter.amount.toDoubleOrNull() ?: (usage * if (meter.type == "water") waterPrice else elecPrice)
                        )
                    )
                }
                state.extraFees.forEach { fee ->
                    val amount = fee.amount.toDoubleOrNull() ?: 0.0
                    if (fee.name.isNotBlank()) {
                        details.add(BillDetail(parentBillId = 0, type = "extra", name = fee.name, amount = amount))
                    }
                }
                
                // 总金额只包含费用明细，不包含租金
                // 租金在收据显示时单独处理
                val totalAmount = details.sumOf { it.amount }
                
                val bill = Bill(tenantRoomNumber = state.roomNumber, month = month, totalAmount = totalAmount)
                
                // 使用带有自动备份的保存方法
                context?.let { ctx ->
                    repository.saveBillWithAutoBackup(ctx, bill, details)
                } ?: repository.saveBill(bill, details) // 如果没有上下文，则使用普通保存方法
            }
            loadData() // Reload after save to refresh next month's previous readings
        }
    }

    fun onMeterPreviousReadingChange(roomNumber: String, meterId: String, newReading: String) {
        _uiState.update { currentStates ->
            currentStates.map { state ->
                if (state.roomNumber == roomNumber) {
                    val updatedMeters = state.meters.map { meter ->
                        if (meter.id == meterId) {
                            val updatedMeter = meter.copy(
                                previousReading = newReading,
                                isUsageManual = false,
                                isAmountManual = false
                            )
                            calculateUsageAndAmount(updatedMeter)
                        } else meter
                    }
                    state.copy(meters = updatedMeters)
                } else state
            }
        }
    }

    fun togglePrevEditable(roomNumber: String, meterId: String) {
        _uiState.update { list ->
            list.map { state ->
                if (state.roomNumber == roomNumber) {
                    val newMeters = state.meters.map { m ->
                        if (m.id == meterId) {
                            val toggled = m.copy(isPrevEditable = !m.isPrevEditable)
                            if (!toggled.isPrevEditable) {
                                // 关闭编辑时根据最新读数重新计算
                                calculateUsageAndAmount(toggled)
                            } else toggled
                        } else m
                    }
                    state.copy(meters = newMeters)
                } else state
            }
        }
    }
    
    /**
     * 更新表的自定义名称
     * 只有非主表才能修改名称
     */
    fun onMeterNameChange(roomNumber: String, meterId: String, newName: String) {
        viewModelScope.launch {
            _uiState.update { currentStates ->
                currentStates.map { state ->
                    if (state.roomNumber == roomNumber) {
                        val updatedMeters = state.meters.map { meter ->
                            if (meter.id == meterId && !meter.isPrimary) {
                                // 保存自定义名称到数据库
                                try {
                                    if (newName.trim().isNotEmpty() && repository.validateMeterCustomName(newName.trim())) {
                                        // 查找租户信息以关联特定租户的表名
                                        val tenant = repository.getTenantByRoomNumber(roomNumber)
                                        if (tenant != null) {
                                        val result = repository.saveMeterCustomName(
                                            defaultName = meter.originalName,
                                            customName = newName.trim(),
                                                meterType = meter.type,
                                                tenantRoomNumber = tenant.roomNumber
                                        )
                                        
                                        if (result.isSuccess) {
                                                android.util.Log.d("AddBillAllViewModel", "成功保存自定义表名称: ${meter.originalName} -> ${newName.trim()} (租户: ${tenant.roomNumber})")
                                            
                                            // 通知其他界面刷新数据
                                                viewModelScope.launch {
                                                    com.morgen.rentalmanager.myapplication.DataSyncManager.notifyMeterNameChanged(
                                                        meter.originalName, 
                                                        newName.trim(),
                                                        tenant.roomNumber
                                                    )
                                                }
                                        } else {
                                            android.util.Log.e("AddBillAllViewModel", "保存自定义表名称失败: ${result.exceptionOrNull()}")
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("AddBillAllViewModel", "保存自定义表名称失败", e)
                                }
                                
                                meter.copy(name = newName)
                            } else meter
                        }
                        state.copy(meters = updatedMeters)
                    } else state
                }
            }
        }
    }

    private fun canEditMeter(detail: MeterInput): Boolean {
        // 使用更宽松的表名识别规则
        val containsWaterMeter = detail.originalName.contains("水表") 
        val containsElectricityMeter = detail.originalName.contains("电表")
        val isMainMeter = detail.originalName == "主水表" || detail.originalName == "主电表"
        val isExtraMeter = (containsWaterMeter || containsElectricityMeter) && !isMainMeter
        
        // 非主表的水电表允许修改
        return isExtraMeter
    }

    private fun updateDisplayNameForMeter(defaultName: String, roomNumber: String): String {
        return viewModelScope.async(Dispatchers.IO) {
            try {
                val customName = repository.getMeterDisplayName(defaultName, roomNumber)
                if (customName.isNotBlank()) {
                    android.util.Log.d("AddBillAllViewModel", "更新表名: $defaultName -> $customName (租户: $roomNumber)")
                    customName
                } else {
                    defaultName
                }
            } catch (e: Exception) {
                android.util.Log.w("AddBillAllViewModel", "获取表名失败: $defaultName", e)
                defaultName
            }
        }.getCompleted()
    }
}

class AddBillAllViewModelFactory(private val repository: TenantRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddBillAllViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddBillAllViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 