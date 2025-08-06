package com.morgen.rentalmanager.ui.billdetail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.morgen.rentalmanager.ui.components.ElevatedCard
import com.morgen.rentalmanager.ui.theme.*
import com.morgen.rentalmanager.utils.AmountFormatter

/**
 * 租户基本信息卡片
 */
@Composable
fun TenantInfoCard(
    billDetail: BillDetailData,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
        ) {
            Text(
                text = "租户信息",
                style = MaterialTheme.typography.titleMedium,
                color = ModernColors.OnSurface,
                fontWeight = FontWeight.Bold
            )
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = ModernSpacing.Small),
                color = ModernColors.OutlineVariant
            )
            
            // 房间号
            InfoRow(
                icon = Icons.Default.Home,
                label = "房间号",
                value = billDetail.roomNumber
            )
            
            // 租户姓名
            if (billDetail.tenantName.isNotBlank()) {
                InfoRow(
                    icon = Icons.Default.Person,
                    label = "租户姓名",
                    value = billDetail.tenantName
                )
            }
            
            // 联系电话
            if (billDetail.phone.isNotBlank()) {
                InfoRow(
                    icon = Icons.Default.Phone,
                    label = "联系电话",
                    value = billDetail.phone
                )
            }
        }
    }
}

/**
 * 账单汇总详情卡片
 */
@Composable
fun BillSummaryDetailCard(
    billDetail: BillDetailData,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
        ) {
            Text(
                text = "费用汇总",
                style = MaterialTheme.typography.titleMedium,
                color = ModernColors.OnSurface,
                fontWeight = FontWeight.Bold
            )
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = ModernSpacing.Small),
                color = ModernColors.OutlineVariant
            )
            
            // 租金
            if (billDetail.rent > 0) {
                SummaryRow(
                    label = "租金",
                    amount = billDetail.getFormattedRent()
                )
            }
            
            // 水费
            if (billDetail.waterAmount > 0) {
                SummaryRow(
                    label = "水费",
                    amount = billDetail.getFormattedWaterAmount(),
                    usage = if (billDetail.waterUsage > 0) "${billDetail.getFormattedWaterUsage()}方" else null
                )
            }
            
            // 电费
            if (billDetail.electricityAmount > 0) {
                SummaryRow(
                    label = "电费",
                    amount = billDetail.getFormattedElectricityAmount(),
                    usage = if (billDetail.electricityUsage > 0) "${billDetail.getFormattedElectricityUsage()}度" else null
                )
            }
            
            // 其他费用
            if (billDetail.extraAmount > 0) {
                SummaryRow(
                    label = "其他费用",
                    amount = billDetail.getFormattedExtraAmount()
                )
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = ModernSpacing.Small),
                color = ModernColors.OutlineVariant
            )
            
            // 总金额
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "总金额",
                    style = MaterialTheme.typography.titleMedium,
                    color = ModernColors.OnSurface,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = AmountFormatter.formatAmountWithCurrency(billDetail.totalAmount),
                    style = MaterialTheme.typography.titleLarge,
                    color = ModernColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 费用明细卡片
 */
@Composable
fun BillDetailsCard(
    billDetail: BillDetailData,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
        ) {
            Text(
                text = "费用明细",
                style = MaterialTheme.typography.titleMedium,
                color = ModernColors.OnSurface,
                fontWeight = FontWeight.Bold
            )
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = ModernSpacing.Small),
                color = ModernColors.OutlineVariant
            )
            
            // 按类型分组显示明细
            val groupedDetails = billDetail.details.groupBy { it.type }
            
            // 水表明细
            groupedDetails["water"]?.let { waterDetails ->
                if (waterDetails.isNotEmpty()) {
                    DetailGroupSection(
                        title = "水表",
                        details = waterDetails
                    )
                }
            }
            
            // 电表明细
            groupedDetails["electricity"]?.let { electricityDetails ->
                if (electricityDetails.isNotEmpty()) {
                    DetailGroupSection(
                        title = "电表",
                        details = electricityDetails
                    )
                }
            }
            
            // 额外费用明细
            groupedDetails["extra"]?.let { extraDetails ->
                if (extraDetails.isNotEmpty()) {
                    DetailGroupSection(
                        title = "额外费用",
                        details = extraDetails
                    )
                }
            }
        }
    }
}

/**
 * 计费说明卡片
 */
@Composable
fun CalculationInfoCard(
    billDetail: BillDetailData,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
        ) {
            Text(
                text = "计费说明",
                style = MaterialTheme.typography.titleMedium,
                color = ModernColors.OnSurface,
                fontWeight = FontWeight.Bold
            )
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = ModernSpacing.Small),
                color = ModernColors.OutlineVariant
            )
            
            // 水费计算说明
            if (billDetail.waterUsage > 0 && billDetail.waterPricePerUnit > 0) {
                CalculationRow(
                    type = "水费",
                    previousReading = billDetail.waterPreviousReading,
                    currentReading = billDetail.waterCurrentReading,
                    usage = billDetail.waterUsage,
                    pricePerUnit = billDetail.waterPricePerUnit,
                    amount = billDetail.waterAmount
                )
            }
            
            // 电费计算说明
            if (billDetail.electricityUsage > 0 && billDetail.electricityPricePerUnit > 0) {
                CalculationRow(
                    type = "电费",
                    previousReading = billDetail.electricityPreviousReading,
                    currentReading = billDetail.electricityCurrentReading,
                    usage = billDetail.electricityUsage,
                    pricePerUnit = billDetail.electricityPricePerUnit,
                    amount = billDetail.electricityAmount
                )
            }
        }
    }
}

/**
 * 信息行组件
 */
@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = ModernColors.OnSurfaceVariant
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ModernColors.OnSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = ModernColors.OnSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 汇总行组件
 */
@Composable
private fun SummaryRow(
    label: String,
    amount: String,
    usage: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
                text = AmountFormatter.formatAmountWithCurrency(AmountFormatter.parseAmount(amount)),
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
 * 明细分组区域组件
 */
@Composable
private fun DetailGroupSection(
    title: String,
    details: List<BillDetailItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
    ) {
        // 分组标题
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = ModernColors.Primary,
            fontWeight = FontWeight.SemiBold
        )
        
        // 该分组下的所有明细项
        details.forEach { detail ->
            DetailRow(detail = detail)
        }
        
        // 分组间的分隔线（除了最后一组）
        HorizontalDivider(
            modifier = Modifier.padding(vertical = ModernSpacing.Small),
            color = ModernColors.OutlineVariant.copy(alpha = 0.5f)
        )
    }
}

/**
 * 明细行组件
 */
@Composable
private fun DetailRow(
    detail: BillDetailItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ModernSpacing.XSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = detail.name,
                style = MaterialTheme.typography.bodyMedium,
                color = ModernColors.OnSurface,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = AmountFormatter.formatAmountWithCurrency(detail.amount),
                style = MaterialTheme.typography.bodyMedium,
                color = ModernColors.OnSurface,
                fontWeight = FontWeight.Medium
            )
        }
        
        if (detail.hasUsageInfo()) {
            Text(
                text = detail.getUsageDescription(),
                style = MaterialTheme.typography.bodySmall,
                color = ModernColors.OnSurfaceVariant
            )
        }
    }
}

/**
 * 计算行组件
 */
@Composable
private fun CalculationRow(
    type: String,
    previousReading: Double?,
    currentReading: Double?,
    usage: Double,
    pricePerUnit: Double,
    amount: Double,
    modifier: Modifier = Modifier
) {
    // 根据类型确定单位
    val unit = when (type) {
        "水费" -> "方"
        "电费" -> "度"
        else -> "度"
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ModernSpacing.XSmall)
    ) {
        Text(
            text = type,
            style = MaterialTheme.typography.bodyMedium,
            color = ModernColors.OnSurface,
            fontWeight = FontWeight.Medium
        )
        
        if (previousReading != null && currentReading != null) {
            Text(
                text = "上月读数: ${AmountFormatter.formatUsage(previousReading)}$unit",
                style = MaterialTheme.typography.bodySmall,
                color = ModernColors.OnSurfaceVariant
            )
            
            Text(
                text = "本月读数: ${AmountFormatter.formatUsage(currentReading)}$unit",
                style = MaterialTheme.typography.bodySmall,
                color = ModernColors.OnSurfaceVariant
            )
        }
        
        Text(
            text = "用量: ${AmountFormatter.formatUsage(usage)}$unit × ¥${AmountFormatter.formatAmount(pricePerUnit)}/$unit = ¥${AmountFormatter.formatAmount(amount)}",
            style = MaterialTheme.typography.bodySmall,
            color = ModernColors.OnSurfaceVariant
        )
    }
}