package com.morgen.rentalmanager.ui.receipt

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morgen.rentalmanager.myapplication.Bill
import com.morgen.rentalmanager.myapplication.BillDetail
import com.morgen.rentalmanager.myapplication.BillWithDetails
import com.morgen.rentalmanager.myapplication.Tenant
import com.morgen.rentalmanager.myapplication.TenantRepository
import com.morgen.rentalmanager.ui.theme.MyApplicationTheme
import com.morgen.rentalmanager.ui.theme.ModernColors
import com.morgen.rentalmanager.ui.theme.ModernSpacing
import com.morgen.rentalmanager.ui.theme.ModernCorners
import com.morgen.rentalmanager.utils.AmountFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptUi(
    tenant: Tenant, 
    billWithDetails: BillWithDetails, 
    repository: TenantRepository? = null, 
    privacyKeywords: List<String> = emptyList(),
    preloadedCustomNames: Map<String, String>? = null
) {
    // 添加详细日志
    android.util.Log.d("ReceiptUi", "=== ReceiptUi 开始渲染 ===")
    android.util.Log.d("ReceiptUi", "租户信息: 房号=${tenant.roomNumber}, 姓名=${tenant.name}, 租金=${tenant.rent}")
    
    // 直接使用传入的租户房号，不再进行隐私保护处理（已在MainActivity中处理）
    val protectedRoomNumber = tenant.roomNumber
    android.util.Log.d("ReceiptUi", "使用房号: $protectedRoomNumber")
    
    // 自定义表名称状态管理
    var customMeterNames by remember { mutableStateOf<Map<String, String>>(preloadedCustomNames ?: emptyMap()) }
    
    // 如果没有预加载的自定义名称，则异步加载
    LaunchedEffect(repository, billWithDetails, tenant, preloadedCustomNames) {
        // 如果已经有预加载的名称，就不需要再加载了
        if (preloadedCustomNames != null) {
            android.util.Log.d("ReceiptUi", "=== 使用预加载的自定义表名称 ===")
            android.util.Log.d("ReceiptUi", "预加载映射表: $preloadedCustomNames")
            customMeterNames = preloadedCustomNames
            return@LaunchedEffect
        }
        
        android.util.Log.d("ReceiptUi", "=== 开始异步加载自定义表名称 ===")
        
        if (repository != null) {
            try {
                val nameMap = mutableMapOf<String, String>()
                
                // 处理所有账单详情，预先获取自定义名称
                billWithDetails.details.forEach { detail ->
                    if (detail.type in listOf("water", "electricity")) {
                        // 使用更宽松的表名识别规则
                        val containsWaterMeter = detail.name.contains("水表") 
                        val containsElectricityMeter = detail.name.contains("电表")
                        val isMainMeter = detail.name == "主水表" || detail.name == "主电表"
                        val isExtraMeter = (containsWaterMeter || containsElectricityMeter) && !isMainMeter
                        
                        // 只有额外表才查询自定义名称
                        val displayName = if (isExtraMeter) {
                            val customName = repository.getMeterDisplayName(detail.name, tenant.roomNumber)
                            android.util.Log.d("ReceiptUi", "查询自定义名称: '${detail.name}' -> '$customName'")
                            customName.ifBlank { detail.name }
                        } else {
                            detail.name
                        }
                        nameMap[detail.name] = displayName
                    }
                }
                
                customMeterNames = nameMap
                android.util.Log.d("ReceiptUi", "异步自定义表名加载完成: $customMeterNames")
                
            } catch (e: Exception) {
                android.util.Log.e("ReceiptUi", "异步加载自定义表名失败", e)
                customMeterNames = emptyMap()
            }
        }
    }
    
    // 获取显示名称的辅助函数
    fun getDisplayName(originalName: String): String {
        return customMeterNames[originalName] ?: originalName
    }
    // 现代简约风格的收据设计 - 打印友好，支持自定义表名称
    Column(
        modifier = Modifier
            .widthIn(min = 350.dp, max = 600.dp) // 设置最小和最大宽度限制
            .wrapContentWidth() // 宽度自适应内容
            .wrapContentHeight(unbounded = true) // 确保高度不被限制
            .background(Color.White) // 纯白背景，打印友好
            .padding(horizontal = 24.dp, vertical = 24.dp) // 适当的内边距
    ) {
        // 简约标题区域
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "收 据",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ModernColors.OnSurface,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(ModernSpacing.Small))
            
            Text(
                text = billWithDetails.bill.month,
                style = MaterialTheme.typography.titleMedium,
                color = ModernColors.Primary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(ModernSpacing.Small))
            
            // 简约分割线
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(2.dp)
                    .background(ModernColors.Primary)
            )
        }
        
        Spacer(modifier = Modifier.height(ModernSpacing.Large))

        // 现代化租户信息区域
        Surface(
            shape = RoundedCornerShape(ModernCorners.Medium),
            color = Color.White,
            border = BorderStroke(0.5.dp, ModernColors.Outline)
        ) {
            Column(modifier = Modifier.padding(ModernSpacing.Medium)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "租户信息", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ModernColors.OnSurface
                    )
                    
                    // 房间号标签 - 应用隐私保护
                    Surface(
                        shape = RoundedCornerShape(ModernCorners.Small),
                        color = ModernColors.Primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = protectedRoomNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = ModernColors.Primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(ModernSpacing.Small))
                HorizontalDivider(color = ModernColors.OutlineVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(ModernSpacing.Small))
                
                // 租户详细信息 - 极简设计
                Column {
                    Text(
                        text = tenant.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ModernColors.OnSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "租户",
                        style = MaterialTheme.typography.bodySmall,
                        color = ModernColors.OnSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(ModernSpacing.Large))
        
        // 现代化金额信息区域
        Surface(
            shape = RoundedCornerShape(ModernCorners.Medium),
            color = Color.White,
            border = BorderStroke(0.5.dp, ModernColors.Outline)
        ) {
            Column(modifier = Modifier.padding(ModernSpacing.Medium)) {
                Text(
                    "金额信息", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ModernColors.OnSurface
                )
                
                Spacer(modifier = Modifier.height(ModernSpacing.Small))
                HorizontalDivider(color = ModernColors.OutlineVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(ModernSpacing.Small))
                
                // 基本租金
                Surface(
                    shape = RoundedCornerShape(ModernCorners.Small),
                    color = ModernColors.Primary.copy(alpha = 0.05f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ModernSpacing.Small),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "租金",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = ModernColors.OnSurface
                        )
                        Text(
                            text = AmountFormatter.formatAmountWithCurrency(tenant.rent).also { 
                                android.util.Log.d("ReceiptUi", "显示租金: ${tenant.rent} -> $it")
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ModernColors.Primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(ModernSpacing.Medium))
                
                // 水费区域
                val waterDetails = billWithDetails.details.filter { it.type == "water" }
                if (waterDetails.isNotEmpty()) {
                    // 水费标题
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = ModernSpacing.Small)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ModernColors.Success,
                            modifier = Modifier.size(width = 4.dp, height = 16.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(ModernSpacing.Small))
                        Text(
                            "水费详情", 
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ModernColors.Success
                        )
                    }
                    
                    // 水表详情卡片列表
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        waterDetails.sortedWith(compareByDescending<BillDetail> { it.name.startsWith("主") }.thenBy { it.name }).forEach { detail ->
                            val usage = detail.usage ?: 0.0
                            val displayName = getDisplayName(detail.name)
                            
                            // 独立卡片式布局
                            Surface(
                                shape = RoundedCornerShape(ModernCorners.Small),
                                color = ModernColors.Success.copy(alpha = 0.05f),
                                border = BorderStroke(0.5.dp, ModernColors.Success.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // 表名和金额 - 处理长名称
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = ModernColors.OnSurface,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2, // 允许最多2行显示长名称
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(ModernCorners.Small),
                                            color = ModernColors.Success.copy(alpha = 0.1f),
                                        ) {
                                            Text(
                                                text = AmountFormatter.formatAmountWithCurrency(detail.amount),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = ModernColors.Success,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // 读数信息 - 分两行显示更加清晰
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "上月读数",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ModernColors.OnSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Text(
                                            text = "本月读数",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ModernColors.OnSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Text(
                                            text = "用水量(m³)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ModernColors.OnSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // 实际数值
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = detail.previousReading?.let { AmountFormatter.formatUsage(it) } ?: "-",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Text(
                                            text = detail.currentReading?.let { AmountFormatter.formatUsage(it) } ?: "-",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Text(
                                            text = AmountFormatter.formatUsage(usage),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ModernColors.Success,
                                            modifier = Modifier.weight(1f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // 电费区域
                val elecDetails = billWithDetails.details.filter { it.type == "electricity" }
                if (elecDetails.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(ModernSpacing.Medium))
                    
                    // 电费标题
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = ModernSpacing.Small)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ModernColors.Warning,
                            modifier = Modifier.size(width = 4.dp, height = 16.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(ModernSpacing.Small))
                        Text(
                            "电费详情", 
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ModernColors.Warning
                        )
                    }
                    
                    // 电表详情卡片列表
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        elecDetails.sortedWith(compareByDescending<BillDetail> { it.name.startsWith("主") }.thenBy { it.name }).forEach { detail ->
                            val usage = detail.usage ?: 0.0
                            val displayName = getDisplayName(detail.name)
                            
                            // 独立卡片式布局
                            Surface(
                                shape = RoundedCornerShape(ModernCorners.Small),
                                color = ModernColors.Warning.copy(alpha = 0.05f),
                                border = BorderStroke(0.5.dp, ModernColors.Warning.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // 表名和金额 - 处理长名称
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = ModernColors.OnSurface,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2, // 允许最多2行显示长名称
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(ModernCorners.Small),
                                            color = ModernColors.Warning.copy(alpha = 0.1f),
                                        ) {
                                            Text(
                                                text = AmountFormatter.formatAmountWithCurrency(detail.amount),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = ModernColors.Warning,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // 读数信息 - 分两行显示更加清晰
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "上月读数",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ModernColors.OnSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Text(
                                            text = "本月读数",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ModernColors.OnSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Text(
                                            text = "用电量(kWh)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ModernColors.OnSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // 实际数值
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = detail.previousReading?.let { AmountFormatter.formatUsage(it) } ?: "-",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Text(
                                            text = detail.currentReading?.let { AmountFormatter.formatUsage(it) } ?: "-",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Text(
                                            text = AmountFormatter.formatUsage(usage),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ModernColors.Warning,
                                            modifier = Modifier.weight(1f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // 附加费用区域
                val extraFees = billWithDetails.details.filter { it.type == "extra" }
                if (extraFees.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(ModernSpacing.Medium))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = ModernSpacing.Small)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ModernColors.OnSurfaceVariant,
                            modifier = Modifier.size(width = 4.dp, height = 16.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(ModernSpacing.Small))
                        Text(
                            "附加费用", 
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ModernColors.OnSurfaceVariant
                        )
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(ModernCorners.Small),
                        color = ModernColors.OnSurfaceVariant.copy(alpha = 0.05f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(ModernSpacing.Small)) {
                            extraFees.forEach { fee ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = fee.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ModernColors.OnSurface
                                    )
                                    Text(
                                        text = AmountFormatter.formatAmountWithCurrency(fee.amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = ModernColors.OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 修改区块间的间隔和总计区域
        Spacer(modifier = Modifier.height(ModernSpacing.Large))

        // 总计金额区域 - 现代化突出显示
        Surface(
            shape = RoundedCornerShape(ModernCorners.Medium),
            color = ModernColors.Primary.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, ModernColors.Primary.copy(alpha = 0.2f))
        ) {
            // 正确计算总金额：租金 + 账单中的费用明细
            val totalAmount = tenant.rent + billWithDetails.bill.totalAmount
            android.util.Log.d("ReceiptUi", "总金额计算: 租金${tenant.rent} + 账单${billWithDetails.bill.totalAmount} = $totalAmount")
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ModernSpacing.Medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "总计金额", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ModernColors.OnSurface
                )
                Text(
                    AmountFormatter.formatAmountWithCurrency(totalAmount), 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = ModernColors.Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(ModernSpacing.Large))
        Text(
            text = "生成日期: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}",
            style = MaterialTheme.typography.bodySmall,
            color = ModernColors.OnSurfaceVariant,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

// ReceiptRow函数已被移除，因为我们现在使用更丰富的现代化UI组件

@Preview(showBackground = true)
@Composable
fun ReceiptUiPreview() {
    val sampleTenant = Tenant("101", "张三", 1500.0)
    val sampleBill = Bill(1, "101", "2023-10", 1755.5)
    val sampleDetails = listOf(
        BillDetail(1, 1, "water", "主水表", 100.0, 110.0, 10.0, 5.0, 50.0),
        BillDetail(4, 1, "water", "额外水表1", 50.0, 70.0, 20.0, 5.0, 100.0),
        BillDetail(2, 1, "electricity", "主电表", 2000.0, 2150.0, 150.0, 1.2, 180.0),
        BillDetail(5, 1, "electricity", "额外电表1", 100.0, 150.0, 50.0, 1.2, 60.0),
        BillDetail(3, 1, "extra", "卫生费", amount = 25.5)
    )
    val sampleBillWithDetails = BillWithDetails(sampleBill, sampleDetails)
    MyApplicationTheme {
        ReceiptUi(tenant = sampleTenant, billWithDetails = sampleBillWithDetails, repository = null, privacyKeywords = emptyList())
    }
} 