package com.morgen.rentalmanager.ui.billlist

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.morgen.rentalmanager.ui.components.ElevatedCard
import com.morgen.rentalmanager.ui.theme.*

/**
 * 账单汇总卡片组件
 * 使用网格布局显示统计数据，集成ElevatedCard设计，支持深色模式
 * 
 * @param summary 账单汇总数据
 * @param modifier 修饰符
 */
@Composable
fun BillSummaryCard(
    summary: BillSummary,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        defaultElevation = ModernElevation.Level1
    ) {
        Column(
            modifier = Modifier.padding(ModernSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(ModernSpacing.Medium)
        ) {
            // 标题
            Text(
                text = "账单汇总",
                style = MaterialTheme.typography.titleMedium,
                color = ModernColors.OnSurface,
                fontWeight = FontWeight.SemiBold
            )
            
            // 检查是否有数据
            if (!summary.hasAnyAmount()) {
                // 空状态处理
                EmptyBillSummaryContent()
            } else {
                // 网格布局显示统计数据
                BillSummaryGrid(summary = summary)
            }
        }
    }
}

/**
 * 账单汇总网格布局
 * 
 * @param summary 账单汇总数据
 */
@Composable
private fun BillSummaryGrid(
    summary: BillSummary
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
    ) {
        // 第一行：租金和水费
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
        ) {
            BillSummaryItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Home,
                label = "总租金",
                amount = summary.getFormattedTotalRent(),
                color = ModernColors.Primary
            )
            
            BillSummaryItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Water,
                label = "总水费",
                amount = summary.getFormattedTotalWater(),
                color = ModernColors.Success
            )
        }
        
        // 第二行：电费和总金额
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
        ) {
            BillSummaryItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ElectricBolt,
                label = "总电费",
                amount = summary.getFormattedTotalElectricity(),
                color = ModernColors.Warning
            )
            
            BillSummaryItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AccountBalance,
                label = "总金额",
                amount = summary.getFormattedTotalAmount(),
                color = ModernColors.OnSurface,
                isTotal = true
            )
        }
    }
}

/**
 * 单个账单汇总项
 * 
 * @param modifier 修饰符
 * @param icon 图标
 * @param label 标签
 * @param amount 金额
 * @param color 颜色
 * @param isTotal 是否为总金额项
 */
@Composable
private fun BillSummaryItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    amount: String,
    color: androidx.compose.ui.graphics.Color,
    isTotal: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isTotal) {
                ModernColors.PrimaryContainer
            } else {
                ModernColors.SurfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
        ) {
            // 图标
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isTotal) ModernColors.OnPrimaryContainer else color,
                modifier = Modifier.size(ModernIconSize.Large)
            )
            
            // 标签
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isTotal) ModernColors.OnPrimaryContainer else ModernColors.OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            // 金额
            Text(
                text = "¥$amount",
                style = if (isTotal) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = if (isTotal) ModernColors.OnPrimaryContainer else ModernColors.OnSurface,
                fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 空状态内容
 */
@Composable
private fun EmptyBillSummaryContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ModernSpacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ModernSpacing.Medium)
    ) {
        // 空状态图标
        Icon(
            imageVector = Icons.Default.AccountBalance,
            contentDescription = "暂无数据",
            tint = ModernColors.OnSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        
        // 空状态文字
        Text(
            text = "暂无账单数据",
            style = MaterialTheme.typography.bodyMedium,
            color = ModernColors.OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // 提示文字
        Text(
            text = "当前月份还没有生成账单",
            style = MaterialTheme.typography.bodySmall,
            color = ModernColors.OnSurfaceTertiary,
            textAlign = TextAlign.Center
        )
    }
}

// 预览组件
@Preview(name = "账单汇总卡片 - 有数据")
@Composable
private fun BillSummaryCardPreview() {
    MaterialTheme {
        BillSummaryCard(
            summary = BillSummary(
                totalRent = 3000.0,
                totalWater = 150.50,
                totalElectricity = 280.75,
                totalAmount = 3431.25
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "账单汇总卡片 - 空状态")
@Composable
private fun BillSummaryCardEmptyPreview() {
    MaterialTheme {
        BillSummaryCard(
            summary = BillSummary(),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "账单汇总卡片 - 深色模式", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BillSummaryCardDarkPreview() {
    MaterialTheme {
        BillSummaryCard(
            summary = BillSummary(
                totalRent = 3000.0,
                totalWater = 150.50,
                totalElectricity = 280.75,
                totalAmount = 3431.25
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}