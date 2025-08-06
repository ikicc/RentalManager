package com.morgen.rentalmanager.ui.billlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.morgen.rentalmanager.ui.theme.ModernSpacing
import com.morgen.rentalmanager.ui.theme.MyApplicationTheme

/**
 * 账单列表内容组件演示
 * 展示BillListContent和BillItemCard组件的各种状态
 */
@Composable
fun BillListContentDemo() {
    var selectedDemo by remember { mutableStateOf("有数据") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ModernSpacing.Medium)
    ) {
        Text(
            text = "账单列表组件演示",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = ModernSpacing.Medium)
        )
        
        // 演示选择器
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = ModernSpacing.Medium),
            horizontalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
        ) {
            listOf("有数据", "空状态", "单个卡片").forEach { demo ->
                FilterChip(
                    onClick = { selectedDemo = demo },
                    label = { Text(demo) },
                    selected = selectedDemo == demo
                )
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = ModernSpacing.Medium))
        
        // 根据选择显示不同演示
        when (selectedDemo) {
            "有数据" -> BillListWithDataDemo()
            "空状态" -> BillListEmptyDemo()
            "单个卡片" -> SingleBillItemDemo()
        }
    }
}

/**
 * 有数据的账单列表演示
 */
@Composable
private fun BillListWithDataDemo() {
    val sampleBills = listOf(
        BillItem(
            roomNumber = "101",
            tenantName = "张三",
            phone = "13800138001",
            rent = 1500.0,
            waterAmount = 50.0,
            waterUsage = 25.0,
            electricityAmount = 80.0,
            electricityUsage = 100.0,
            totalAmount = 1630.0
        ),
        BillItem(
            roomNumber = "102",
            tenantName = "李四",
            phone = "13800138002",
            rent = 1200.0,
            waterAmount = 30.0,
            waterUsage = 15.0,
            electricityAmount = 60.0,
            electricityUsage = 75.0,
            totalAmount = 1290.0
        ),
        BillItem(
            roomNumber = "103",
            tenantName = "王五",
            phone = "13800138003",
            rent = 1800.0,
            waterAmount = 0.0,
            waterUsage = 0.0,
            electricityAmount = 0.0,
            electricityUsage = 0.0,
            totalAmount = 1800.0
        ),
        BillItem(
            roomNumber = "104",
            tenantName = "赵六",
            phone = "13800138004",
            rent = 0.0,
            waterAmount = 0.0,
            waterUsage = 0.0,
            electricityAmount = 0.0,
            electricityUsage = 0.0,
            totalAmount = 0.0
        )
    )
    
    Text(
        text = "包含多种数据状态的账单列表",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = ModernSpacing.Small)
    )
    
    BillListContent(
        bills = sampleBills,
        selectedMonth = "2024-01",
        onBillClick = { roomNumber, month ->
            // 演示用，实际会导航到详情页面
            println("点击了房间 $roomNumber 的 $month 账单")
        },
        modifier = Modifier.height(400.dp)
    )
}

/**
 * 空状态账单列表演示
 */
@Composable
private fun BillListEmptyDemo() {
    Text(
        text = "空状态账单列表",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = ModernSpacing.Small)
    )
    
    BillListContent(
        bills = emptyList(),
        selectedMonth = "2024-02",
        onBillClick = { _, _ -> },
        modifier = Modifier.height(300.dp)
    )
}

/**
 * 单个账单卡片演示
 */
@Composable
private fun SingleBillItemDemo() {
    Text(
        text = "不同状态的账单卡片",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = ModernSpacing.Medium)
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
    ) {
        // 完整数据的卡片
        Text(
            text = "完整数据卡片",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = ModernSpacing.XSmall)
        )
        BillItemCard(
            billItem = BillItem(
                roomNumber = "201",
                tenantName = "完整数据租户",
                rent = 1500.0,
                waterAmount = 50.0,
                waterUsage = 25.0,
                electricityAmount = 80.0,
                electricityUsage = 100.0,
                totalAmount = 1630.0
            ),
            onClick = { println("点击了完整数据卡片") }
        )
        
        Spacer(modifier = Modifier.height(ModernSpacing.Small))
        
        // 只有租金的卡片
        Text(
            text = "只有租金卡片",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = ModernSpacing.XSmall)
        )
        BillItemCard(
            billItem = BillItem(
                roomNumber = "202",
                tenantName = "只有租金租户",
                rent = 1200.0,
                waterAmount = 0.0,
                waterUsage = 0.0,
                electricityAmount = 0.0,
                electricityUsage = 0.0,
                totalAmount = 1200.0
            ),
            onClick = { println("点击了只有租金卡片") }
        )
        
        Spacer(modifier = Modifier.height(ModernSpacing.Small))
        
        // 无费用数据的卡片
        Text(
            text = "无费用数据卡片",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = ModernSpacing.XSmall)
        )
        BillItemCard(
            billItem = BillItem(
                roomNumber = "203",
                tenantName = "无费用租户",
                rent = 0.0,
                waterAmount = 0.0,
                waterUsage = 0.0,
                electricityAmount = 0.0,
                electricityUsage = 0.0,
                totalAmount = 0.0
            ),
            onClick = { println("点击了无费用卡片") }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BillListContentDemoPreview() {
    MyApplicationTheme {
        BillListContentDemo()
    }
}

@Preview(showBackground = true, name = "有数据列表")
@Composable
fun BillListWithDataPreview() {
    MyApplicationTheme {
        BillListWithDataDemo()
    }
}

@Preview(showBackground = true, name = "空状态列表")
@Composable
fun BillListEmptyPreview() {
    MyApplicationTheme {
        BillListEmptyDemo()
    }
}

@Preview(showBackground = true, name = "单个卡片")
@Composable
fun SingleBillItemPreview() {
    MyApplicationTheme {
        SingleBillItemDemo()
    }
}