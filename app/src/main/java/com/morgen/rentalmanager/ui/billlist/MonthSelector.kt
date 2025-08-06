package com.morgen.rentalmanager.ui.billlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.morgen.rentalmanager.ui.components.ModernMonthYearPicker
import com.morgen.rentalmanager.ui.theme.*
import java.util.Calendar

/**
 * 月份选择组件
 * 集成现有的ModernMonthYearPicker，提供月份选择功能
 * 
 * @param selectedMonth 当前选中的月份字符串 (格式: "YYYY-MM")
 * @param onMonthSelected 月份选择回调
 * @param modifier 修饰符
 */
@Composable
fun MonthSelector(
    selectedMonth: String,
    onMonthSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    
    // 解析当前选中的年月
    val (currentYear, currentMonth) = remember(selectedMonth) {
        parseMonthString(selectedMonth)
    }
    
    // 格式化显示文本
    val displayText = remember(selectedMonth) {
        formatMonthDisplay(selectedMonth)
    }
    
    // 月份选择器按钮
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ModernCorners.Medium))
            .clickable { showPicker = true },
        shape = RoundedCornerShape(ModernCorners.Medium),
        color = ModernColors.SurfaceContainer,
        shadowElevation = ModernElevation.Level1
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.Medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：日历图标和月份文本
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "选择月份",
                    tint = ModernColors.Primary,
                    modifier = Modifier.size(ModernIconSize.Medium)
                )
                
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ModernColors.OnSurface
                )
            }
            
            // 右侧：下拉箭头
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "展开选择",
                tint = ModernColors.OnSurfaceVariant,
                modifier = Modifier.size(ModernIconSize.Medium)
            )
        }
    }
    
    // 月份年份选择器弹窗
    ModernMonthYearPicker(
        show = showPicker,
        initialYear = currentYear,
        initialMonth = currentMonth,
        onDismiss = { showPicker = false },
        onConfirm = { year, month ->
            val monthString = formatMonthString(year, month)
            onMonthSelected(monthString)
            showPicker = false
        }
    )
}

/**
 * 紧凑版月份选择组件
 * 用于空间受限的场景
 * 
 * @param selectedMonth 当前选中的月份字符串 (格式: "YYYY-MM")
 * @param onMonthSelected 月份选择回调
 * @param modifier 修饰符
 */
@Composable
fun CompactMonthSelector(
    selectedMonth: String,
    onMonthSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    
    // 解析当前选中的年月
    val (currentYear, currentMonth) = remember(selectedMonth) {
        parseMonthString(selectedMonth)
    }
    
    // 格式化显示文本
    val displayText = remember(selectedMonth) {
        formatMonthDisplay(selectedMonth)
    }
    
    // 紧凑型按钮
    OutlinedButton(
        onClick = { showPicker = true },
        modifier = modifier,
        shape = RoundedCornerShape(ModernCorners.Medium),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = ModernColors.Primary,
            containerColor = Color.Transparent
        ),
        border = ButtonDefaults.outlinedButtonBorder,
        contentPadding = PaddingValues(
            horizontal = ModernSpacing.Medium,
            vertical = ModernSpacing.Small
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ModernSpacing.XSmall)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "选择月份",
                modifier = Modifier.size(ModernIconSize.Small)
            )
            
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "展开选择",
                modifier = Modifier.size(ModernIconSize.Small)
            )
        }
    }
    
    // 月份年份选择器弹窗
    ModernMonthYearPicker(
        show = showPicker,
        initialYear = currentYear,
        initialMonth = currentMonth,
        onDismiss = { showPicker = false },
        onConfirm = { year, month ->
            val monthString = formatMonthString(year, month)
            onMonthSelected(monthString)
            showPicker = false
        }
    )
}

/**
 * 解析月份字符串，返回年份和月份
 * 
 * @param monthString 月份字符串 (格式: "YYYY-MM")
 * @return Pair<年份, 月份>
 */
private fun parseMonthString(monthString: String): Pair<Int, Int> {
    return try {
        if (monthString.contains("-") && monthString.length >= 7) {
            val parts = monthString.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            Pair(year, month)
        } else {
            // 如果格式不正确，返回当前年月
            val calendar = Calendar.getInstance()
            Pair(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
        }
    } catch (e: Exception) {
        // 解析失败时返回当前年月
        val calendar = Calendar.getInstance()
        Pair(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    }
}

/**
 * 格式化月份字符串用于显示
 * 
 * @param monthString 月份字符串 (格式: "YYYY-MM")
 * @return 格式化的显示文本 (格式: "YYYY年MM月")
 */
private fun formatMonthDisplay(monthString: String): String {
    val (year, month) = parseMonthString(monthString)
    return "${year}年${String.format("%02d", month)}月"
}

/**
 * 格式化年月为字符串
 * 
 * @param year 年份
 * @param month 月份 (1-12)
 * @return 格式化的月份字符串 (格式: "YYYY-MM")
 */
private fun formatMonthString(year: Int, month: Int): String {
    return "$year-${String.format("%02d", month)}"
}

/**
 * 获取当前月份字符串
 * 
 * @return 当前月份字符串 (格式: "YYYY-MM")
 */
fun getCurrentMonthString(): String {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH 是从0开始的
    return formatMonthString(year, month)
}

/**
 * 获取指定月份的下一个月
 * 
 * @param monthString 当前月份字符串 (格式: "YYYY-MM")
 * @return 下一个月的字符串 (格式: "YYYY-MM")
 */
fun getNextMonth(monthString: String): String {
    val (year, month) = parseMonthString(monthString)
    return if (month == 12) {
        formatMonthString(year + 1, 1)
    } else {
        formatMonthString(year, month + 1)
    }
}

/**
 * 获取指定月份的上一个月
 * 
 * @param monthString 当前月份字符串 (格式: "YYYY-MM")
 * @return 上一个月的字符串 (格式: "YYYY-MM")
 */
fun getPreviousMonth(monthString: String): String {
    val (year, month) = parseMonthString(monthString)
    return if (month == 1) {
        formatMonthString(year - 1, 12)
    } else {
        formatMonthString(year, month - 1)
    }
}