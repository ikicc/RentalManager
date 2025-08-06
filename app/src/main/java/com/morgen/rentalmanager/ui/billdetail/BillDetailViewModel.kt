package com.morgen.rentalmanager.ui.billdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.morgen.rentalmanager.myapplication.TenantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.sql.SQLException

/**
 * 账单详情页面ViewModel
 * 管理账单详情数据的加载和状态
 */
class BillDetailViewModel(
    private val repository: TenantRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<BillDetailUiState>(BillDetailUiState.Loading)
    val uiState: StateFlow<BillDetailUiState> = _uiState.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * 加载账单详情数据
     * 
     * @param roomNumber 房间号
     * @param month 月份
     */
    fun loadBillDetail(roomNumber: String, month: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _uiState.value = BillDetailUiState.Loading
                
                // 验证输入参数
                if (roomNumber.isBlank() || month.isBlank()) {
                    _uiState.value = BillDetailUiState.Error("房间号或月份参数无效")
                    return@launch
                }
                
                // 获取租户信息
                val tenant = repository.getTenantByRoomNumber(roomNumber)
                if (tenant == null) {
                    _uiState.value = BillDetailUiState.NotFound
                    return@launch
                }
                
                // 获取账单信息
                val bill = repository.getBillByRoomAndMonth(roomNumber, month)
                if (bill == null) {
                    _uiState.value = BillDetailUiState.NotFound
                    return@launch
                }
                
                // 获取账单详情
                val billDetails = repository.getBillDetailsByBillId(bill.billId)
                
                // 为账单详情应用自定义名称
                val billDetailsWithCustomNames = billDetails.map { detail ->
                    // 使用更宽松的表名识别规则
                    val containsWaterMeter = detail.name.contains("水表") 
                    val containsElectricityMeter = detail.name.contains("电表")
                    val isMainMeter = detail.name == "主水表" || detail.name == "主电表"
                    val isExtraMeter = (containsWaterMeter || containsElectricityMeter) && !isMainMeter
                    
                    // 仅处理水表和电表，并且只有额外表名可以自定义
                    if ((detail.type == "water" || detail.type == "electricity") && isExtraMeter) {
                        try {
                            android.util.Log.d("BillDetailViewModel", "处理账单详情自定义名称: '${detail.name}'")
                            // 获取自定义表名
                            val customName = repository.getMeterDisplayName(detail.name, bill.tenantRoomNumber)
                            android.util.Log.d("BillDetailViewModel", "获取自定义名称结果: '${detail.name}' -> '$customName'")
                            
                            // 只有当获取到的名称非空且不同于原名时才替换
                            if (customName.isNotBlank() && customName != detail.name) {
                                android.util.Log.d("BillDetailViewModel", "应用自定义名称: '${detail.name}' -> '$customName'")
                                detail.copy(name = customName)
                            } else {
                                detail
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("BillDetailViewModel", "处理自定义名称异常: '${detail.name}'", e)
                            detail
                        }
                    } else {
                        // 主表名称或其他费用项保持原样
                        detail
                    }
                }
                
                // 计算各项费用
                val waterDetails = billDetailsWithCustomNames.filter { it.type == "water" }
                val electricityDetails = billDetailsWithCustomNames.filter { it.type == "electricity" }
                val extraDetails = billDetailsWithCustomNames.filter { it.type == "extra" }
                
                val waterAmount = waterDetails.sumOf { it.amount }
                val electricityAmount = electricityDetails.sumOf { it.amount }
                val extraAmount = extraDetails.sumOf { it.amount }
                
                // 智能处理总金额计算，兼容新旧数据格式
                val detailsSum = waterAmount + electricityAmount + extraAmount
                val calculatedTotalWithRent = tenant.rent + detailsSum
                val storedTotal = bill.totalAmount
                
                // 判断数据库中存储的是哪种格式
                val isOldFormat = kotlin.math.abs(storedTotal - detailsSum) < 0.01
                val isNewFormat = kotlin.math.abs(storedTotal - calculatedTotalWithRent) < 0.01
                
                val totalAmount = when {
                    isNewFormat -> {
                        // 新格式：数据库中已包含租金
                        storedTotal
                    }
                    isOldFormat -> {
                        // 旧格式：数据库中不包含租金，需要加上租金
                        calculatedTotalWithRent
                    }
                    else -> {
                        // 数据不匹配，使用计算值并记录警告
                        println("警告: 数据格式不匹配 - 存储: ¥${String.format("%.2f", storedTotal)}, 仅明细: ¥${String.format("%.2f", detailsSum)}, 含租金: ¥${String.format("%.2f", calculatedTotalWithRent)}")
                        calculatedTotalWithRent
                    }
                }
                
                // 获取用量和单价信息
                val waterUsage = waterDetails.sumOf { it.usage ?: 0.0 }
                val electricityUsage = electricityDetails.sumOf { it.usage ?: 0.0 }
                
                val waterPricePerUnit = waterDetails.firstOrNull()?.pricePerUnit ?: 0.0
                val electricityPricePerUnit = electricityDetails.firstOrNull()?.pricePerUnit ?: 0.0
                
                val waterPreviousReading = waterDetails.firstOrNull()?.previousReading
                val waterCurrentReading = waterDetails.firstOrNull()?.currentReading
                val electricityPreviousReading = electricityDetails.firstOrNull()?.previousReading
                val electricityCurrentReading = electricityDetails.firstOrNull()?.currentReading
                
                // 验证数据完整性
                if (tenant.rent < 0 || waterAmount < 0 || electricityAmount < 0 || extraAmount < 0 || totalAmount < 0) {
                    _uiState.value = BillDetailUiState.Error("账单数据异常，包含负数金额")
                    return@launch
                }
                
                // 构建UI数据模型
                val billDetailData = BillDetailData(
                    roomNumber = tenant.roomNumber,
                    tenantName = tenant.name,
                    phone = "", // Tenant entity doesn't have phone field
                    month = month,
                    rent = tenant.rent, // rent is in Tenant entity
                    waterAmount = waterAmount,
                    waterUsage = waterUsage,
                    waterPreviousReading = waterPreviousReading,
                    waterCurrentReading = waterCurrentReading,
                    waterPricePerUnit = waterPricePerUnit,
                    electricityAmount = electricityAmount,
                    electricityUsage = electricityUsage,
                    electricityPreviousReading = electricityPreviousReading,
                    electricityCurrentReading = electricityCurrentReading,
                    electricityPricePerUnit = electricityPricePerUnit,
                    extraAmount = extraAmount,
                    totalAmount = totalAmount,
                    details = billDetailsWithCustomNames.toBillDetailItems()
                )
                
                // 记录详细的调试信息
                println("=== 账单详情调试信息 ===")
                println("房间: $roomNumber, 月份: $month")
                println("租金: ¥${String.format("%.2f", tenant.rent)}")
                println("水费: ¥${String.format("%.2f", waterAmount)} (用量: ${String.format("%.1f", waterUsage)}度)")
                println("电费: ¥${String.format("%.2f", electricityAmount)} (用量: ${String.format("%.1f", electricityUsage)}度)")
                println("其他费用: ¥${String.format("%.2f", extraAmount)}")
                println("数据库存储总金额: ¥${String.format("%.2f", bill.totalAmount)}")
                println("最终显示总金额: ¥${String.format("%.2f", totalAmount)}")
                println("账单详情数量: ${billDetailsWithCustomNames.size}")
                billDetailsWithCustomNames.forEach { detail ->
                    println("  - ${detail.name} (${detail.type}): ¥${String.format("%.2f", detail.amount)}")
                }
                println("========================")
                
                _uiState.value = BillDetailUiState.Success(billDetailData)
                
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 处理错误
     */
    private fun handleError(throwable: Throwable) {
        val errorMessage = when (throwable) {
            is IOException -> "网络连接异常，请检查网络设置"
            is SQLException -> "数据库访问失败，请重试"
            is IllegalArgumentException -> "参数错误: ${throwable.message}"
            is NullPointerException -> "数据不完整，请检查账单数据"
            else -> {
                // 提供更详细的错误信息用于调试
                val detailMessage = throwable.message ?: "未知错误"
                "加载账单详情失败: $detailMessage"
            }
        }
        _uiState.value = BillDetailUiState.Error(errorMessage, throwable)
    }
}

/**
 * BillDetailViewModel工厂类
 */
class BillDetailViewModelFactory(
    private val repository: TenantRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BillDetailViewModel::class.java)) {
            return BillDetailViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}