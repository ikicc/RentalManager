package com.morgen.rentalmanager.ui.billlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.morgen.rentalmanager.ui.theme.*

/**
 * BillChartCard组件演示
 * 展示不同状态下的图表显示效果
 */
@Composable
fun BillChartCardDemo() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(ModernSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(ModernSpacing.Large)
    ) {
        item {
            Text(
                text = "费用分布图表组件演示",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ModernColors.OnSurface
            )
        }
        
        item {
            Text(
                text = "1. 完整数据状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ModernColors.OnSurface
            )
            
            Spacer(modifier = Modifier.height(ModernSpacing.Small))
            
            // 完整数据的图表
            val fullDataSummary = BillSummary(
                totalRent = 3000.0,
                totalWater = 450.0,
                totalElectricity = 550.0,
                totalAmount = 4000.0
            )
            
            BillChartCard(
                chartData = ChartData.fromBillSummary(
                    summary = fullDataSummary,
                    rentColor = getChartColors().first,
                    waterColor = getChartColors().second,
                    electricityColor = getChartColors().third
                )
            )
        }
        
        item {
            Text(
                text = "2. 部分数据状态（无水费）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ModernColors.OnSurface
            )
            
            Spacer(modifier = Modifier.height(ModernSpacing.Small))
            
            // 部分数据的图表
            val partialDataSummary = BillSummary(
                totalRent = 2000.0,
                totalWater = 0.0,
                totalElectricity = 300.0,
                totalAmount = 2300.0
            )
            
            BillChartCard(
                chartData = ChartData.fromBillSummary(
                    summary = partialDataSummary,
                    rentColor = getChartColors().first,
                    waterColor = getChartColors().second,
                    electricityColor = getChartColors().third
                )
            )
        }
        
        item {
            Text(
                text = "3. 单项数据状态（仅租金）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ModernColors.OnSurface
            )
            
            Spacer(modifier = Modifier.height(ModernSpacing.Small))
            
            // 单项数据的图表
            val singleDataSummary = BillSummary(
                totalRent = 1500.0,
                totalWater = 0.0,
                totalElectricity = 0.0,
                totalAmount = 1500.0
            )
            
            BillChartCard(
                chartData = ChartData.fromBillSummary(
                    summary = singleDataSummary,
                    rentColor = getChartColors().first,
                    waterColor = getChartColors().second,
                    electricityColor = getChartColors().third
                )
            )
        }
        
        item {
            Text(
                text = "4. 空数据状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ModernColors.OnSurface
            )
            
            Spacer(modifier = Modifier.height(ModernSpacing.Small))
            
            // 空数据的图表
            BillChartCard(
                chartData = emptyList()
            )
        }
        
        item {
            Text(
                text = "5. 小数值数据状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ModernColors.OnSurface
            )
            
            Spacer(modifier = Modifier.height(ModernSpacing.Small))
            
            // 小数值数据的图表
            val smallDataSummary = BillSummary(
                totalRent = 123.45,
                totalWater = 67.89,
                totalElectricity = 98.76,
                totalAmount = 290.10
            )
            
            BillChartCard(
                chartData = ChartData.fromBillSummary(
                    summary = smallDataSummary,
                    rentColor = getChartColors().first,
                    waterColor = getChartColors().second,
                    electricityColor = getChartColors().third
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BillChartCardDemoPreview() {
    MyApplicationTheme {
        BillChartCardDemo()
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BillChartCardDemoDarkPreview() {
    MyApplicationTheme {
        BillChartCardDemo()
    }
}