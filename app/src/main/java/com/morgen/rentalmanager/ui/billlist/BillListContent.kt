package com.morgen.rentalmanager.ui.billlist

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.morgen.rentalmanager.ui.components.ElevatedCard
import com.morgen.rentalmanager.ui.theme.*

/**
 * 账单列表内容组件
 * 显示所有租户的账单列表，使用Column布局避免嵌套滚动
 * 
 * @param bills 账单列表数据
 * @param selectedMonth 当前选中的月份
 * @param onBillClick 账单点击回调，传递房间号和月份
 * @param modifier 修饰符
 */
@Composable
fun BillListContent(
    bills: List<BillItem>,
    selectedMonth: String,
    onBillClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 添加空值和有效性检查
    val validBills = bills.filter { bill ->
        try {
            bill.isValid() && bill.roomNumber.isNotBlank()
        } catch (e: Exception) {
            android.util.Log.w("BillListContent", "账单数据无效", e)
            false
        }
    }
    
    if (validBills.isEmpty()) {
        // 空状态处理
        EmptyBillListState(
            selectedMonth = selectedMonth,
            modifier = modifier
        )
    } else {
        // 使用Column而不是LazyColumn，避免嵌套滚动
        // 因为在BillListScreen中已经使用LazyColumn统一管理
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
        ) {
            validBills.forEach { bill ->
                BillItemCard(
                    billItem = bill,
                    onClick = { 
                        try {
                            onBillClick(bill.roomNumber, selectedMonth)
                        } catch (e: Exception) {
                            android.util.Log.e("BillListContent", "点击处理失败", e)
                        }
                    }
                )
            }
        }
    }
}

/**
 * 账单项卡片组件
 * 显示单个租户的账单信息，包含房间号、租户姓名、各项费用
 * 
 * @param billItem 账单项数据
 * @param onClick 点击回调
 * @param modifier 修饰符
 */
@Composable
fun BillItemCard(
    billItem: BillItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 预先计算安全的显示值
    val safeRoomNumber = billItem.roomNumber.takeIf { it.isNotBlank() } ?: "未知房间"
    val safeTotalAmount = remember(billItem.totalAmount) {
        try {
            "¥${billItem.getFormattedTotalAmount()}"
        } catch (e: Exception) {
            "¥0.00"
        }
    }
    
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.Medium)
        ) {
            // 头部信息：房间号和租户姓名
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = safeRoomNumber,
                        style = MaterialTheme.typography.titleMedium,
                        color = ModernColors.OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (billItem.tenantName.isNotBlank()) {
                        Text(
                            text = billItem.tenantName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ModernColors.OnSurfaceVariant
                        )
                    }
                }
                
                // 总金额
                Text(
                    text = safeTotalAmount,
                    style = MaterialTheme.typography.titleMedium,
                    color = ModernColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(ModernSpacing.Small))
            
            // 费用详情
            BillItemDetails(billItem = billItem)
        }
    }
}

/**
 * 账单项详情组件
 * 显示租金、水费、电费的详细信息（支持自定义水表/电表名称）
 * 
 * @param billItem 账单项数据
 * @param modifier 修饰符
 */
@Composable
private fun BillItemDetails(
    billItem: BillItem,
    modifier: Modifier = Modifier
) {
    // 预先计算安全的显示值
    val safeRentAmount = remember(billItem.rent) {
        try { billItem.getFormattedRent() } catch (e: Exception) { "0.00" }
    }
    
    val hasRent = remember(billItem.rent) {
        try { billItem.rent > 0 } catch (e: Exception) { false }
    }
    val hasWater = remember(billItem.waterAmount) {
        try { billItem.hasWaterAmount() } catch (e: Exception) { false }
    }
    val hasElectricity = remember(billItem.electricityAmount) {
        try { billItem.hasElectricityAmount() } catch (e: Exception) { false }
    }
    val hasExtraFees = remember(billItem) {
        try { billItem.hasExtraFees() } catch (e: Exception) { false }
    }
    val hasAnyAmount = remember(billItem) {
        try { billItem.hasAnyAmount() } catch (e: Exception) { false }
    }
    
    Column(modifier = modifier) {
        // 租金
        if (hasRent) {
            BillDetailRow(
                label = "租金",
                amount = safeRentAmount,
                usage = null
            )
        }
        
        // 水费详情（支持多个水表和自定义名称）
        if (hasWater) {
            if (billItem.hasMultipleWaterMeters()) {
                // 多个水表时，显示每个水表的详细信息
                billItem.waterMeters.forEach { meter ->
                    BillDetailRow(
                        label = meter.name, // 使用自定义名称
                        amount = meter.getFormattedAmount(),
                        usage = if (meter.usage > 0) "${meter.getFormattedUsage()}${meter.getUsageUnit()}" else null
                    )
                }
            } else {
                // 单个水表时，显示汇总信息
                val safeWaterAmount = try { billItem.getFormattedWaterAmount() } catch (e: Exception) { "0.00" }
                val safeWaterUsage = try { billItem.getFormattedWaterUsage() } catch (e: Exception) { "0.0" }
                BillDetailRow(
                    label = "水费",
                    amount = safeWaterAmount,
                    usage = if (billItem.waterUsage > 0) "${safeWaterUsage}方" else null
                )
            }
        }
        
        // 电费详情（支持多个电表和自定义名称）
        if (hasElectricity) {
            if (billItem.hasMultipleElectricityMeters()) {
                // 多个电表时，显示每个电表的详细信息
                billItem.electricityMeters.forEach { meter ->
                    BillDetailRow(
                        label = meter.name, // 使用自定义名称
                        amount = meter.getFormattedAmount(),
                        usage = if (meter.usage > 0) "${meter.getFormattedUsage()}${meter.getUsageUnit()}" else null
                    )
                }
            } else {
                // 单个电表时，显示汇总信息
                val safeElectricityAmount = try { billItem.getFormattedElectricityAmount() } catch (e: Exception) { "0.00" }
                val safeElectricityUsage = try { billItem.getFormattedElectricityUsage() } catch (e: Exception) { "0.0" }
                BillDetailRow(
                    label = "电费",
                    amount = safeElectricityAmount,
                    usage = if (billItem.electricityUsage > 0) "${safeElectricityUsage}度" else null
                )
            }
        }
        
        // 其他费用详情
        if (hasExtraFees) {
            billItem.extraFees.forEach { fee ->
                BillDetailRow(
                    label = fee.name,
                    amount = fee.getFormattedAmount(),
                    usage = null
                )
            }
        }
        
        // 如果没有任何费用，显示提示
        if (!hasAnyAmount) {
            Text(
                text = "暂无费用数据",
                style = MaterialTheme.typography.bodySmall,
                color = ModernColors.OnSurfaceVariant,
                modifier = Modifier.padding(vertical = ModernSpacing.XSmall)
            )
        }
    }
}

/**
 * 账单详情行组件
 * 显示单项费用的标签、金额和用量
 * 
 * @param label 费用标签
 * @param amount 费用金额
 * @param usage 用量信息（可选）
 * @param modifier 修饰符
 */
@Composable
private fun BillDetailRow(
    label: String,
    amount: String,
    usage: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ModernSpacing.XSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ModernColors.OnSurfaceVariant
        )
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "¥$amount",
                style = MaterialTheme.typography.bodyMedium,
                color = ModernColors.OnSurface,
                fontWeight = FontWeight.Medium
            )
            
            if (usage != null) {
                Text(
                    text = usage,
                    style = MaterialTheme.typography.bodySmall,
                    color = ModernColors.OnSurfaceVariant
                )
            }
        }
    }
}

/**
 * 空状态组件
 * 当没有账单数据时显示的友好提示界面
 * 
 * @param selectedMonth 当前选中的月份
 * @param modifier 修饰符
 */
@Composable
private fun EmptyBillListState(
    selectedMonth: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ModernSpacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Receipt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = ModernColors.OnSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(ModernSpacing.Medium))
        
        Text(
            text = "${selectedMonth}月暂无账单数据",
            style = MaterialTheme.typography.titleMedium,
            color = ModernColors.OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(ModernSpacing.Small))
        
        Text(
            text = "当前月份还没有生成账单\n请先添加租户信息和费用数据",
            style = MaterialTheme.typography.bodyMedium,
            color = ModernColors.OnSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}
/**

 * 错误账单项占位符
 * 当单个账单项渲染失败时显示
 */
@Composable
private fun ErrorBillItemPlaceholder(
    roomNumber: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "房间 $roomNumber 数据异常",
                style = MaterialTheme.typography.bodyMedium,
                color = ModernColors.OnSurfaceVariant
            )
            Text(
                text = "请刷新重试",
                style = MaterialTheme.typography.bodySmall,
                color = ModernColors.OnSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 错误列表状态
 * 当整个列表渲染失败时显示
 */
@Composable
private fun ErrorBillListState(
    selectedMonth: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ModernSpacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "账单列表加载异常",
            style = MaterialTheme.typography.titleMedium,
            color = ModernColors.OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(ModernSpacing.Small))
        
        Text(
            text = "请尝试刷新页面或重启应用",
            style = MaterialTheme.typography.bodyMedium,
            color = ModernColors.OnSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}