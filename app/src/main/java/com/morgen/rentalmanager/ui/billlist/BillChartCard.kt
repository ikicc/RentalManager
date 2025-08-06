package com.morgen.rentalmanager.ui.billlist

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.morgen.rentalmanager.ui.components.ElevatedCard
import com.morgen.rentalmanager.ui.theme.*
import kotlin.math.*

/**
 * 费用分布图表卡片组件
 * 使用Canvas API绘制饼图，支持动画和深色模式
 */
@Composable
fun BillChartCard(
    chartData: List<ChartData>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.Large)
        ) {
            // 标题
            Text(
                text = "费用分布",
                style = MaterialTheme.typography.titleMedium,
                color = ModernColors.OnSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = ModernSpacing.Medium)
            )
            
            if (chartData.isEmpty() || chartData.all { it.value <= 0 }) {
                // 空状态显示
                EmptyChartState(
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // 图表内容
                ChartContent(
                    chartData = chartData,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 空状态显示组件
 */
@Composable
private fun EmptyChartState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.height(200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 空状态图表占位符
        Canvas(
            modifier = Modifier.size(120.dp)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 * 0.8f
            
            // 绘制空状态圆环
            drawCircle(
                color = Color.Gray.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = 8.dp.toPx())
            )
        }
        
        Spacer(modifier = Modifier.height(ModernSpacing.Medium))
        
        Text(
            text = "暂无费用数据",
            style = MaterialTheme.typography.bodyMedium,
            color = ModernColors.OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 图表内容组件
 */
@Composable
private fun ChartContent(
    chartData: List<ChartData>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 饼图
        PieChart(
            chartData = chartData,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        
        Spacer(modifier = Modifier.height(ModernSpacing.Large))
        
        // 图例
        ChartLegend(
            chartData = chartData,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 饼图组件
 */
@Composable
private fun PieChart(
    chartData: List<ChartData>,
    modifier: Modifier = Modifier
) {
    // 动画进度
    var animationPlayed by remember { mutableStateOf(false) }
    val animationProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(
            durationMillis = ModernAnimations.SLOW_DURATION * 2,
            easing = ModernAnimations.SmoothOutEasing
        ),
        label = "pieChartAnimation"
    )
    
    // 启动动画
    LaunchedEffect(chartData) {
        animationPlayed = true
    }
    
    // 重置动画当数据变化时
    LaunchedEffect(chartData) {
        animationPlayed = false
        animationPlayed = true
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(160.dp)
        ) {
            drawPieChart(
                chartData = chartData,
                animationProgress = animationProgress
            )
        }
        
        // 中心文字显示总金额
        val totalAmount = chartData.sumOf { it.value }
        if (totalAmount > 0) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "总计",
                    style = MaterialTheme.typography.labelMedium,
                    color = ModernColors.OnSurfaceVariant
                )
                Text(
                    text = "¥${String.format("%.2f", totalAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = ModernColors.OnSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 绘制饼图的扩展函数
 */
private fun DrawScope.drawPieChart(
    chartData: List<ChartData>,
    animationProgress: Float
) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 2 * 0.8f
    val strokeWidth = 24.dp.toPx()
    
    var startAngle = -90f // 从顶部开始
    
    chartData.forEach { data ->
        val sweepAngle = (data.percentage / 100f * 360f) * animationProgress
        
        if (sweepAngle > 0) {
            // 绘制扇形
            drawArc(
                color = data.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = Size(radius * 2, radius * 2),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
            
            startAngle += data.percentage / 100f * 360f
        }
    }
    
    // 绘制内圆背景
    drawCircle(
        color = Color.Transparent,
        radius = radius - strokeWidth / 2,
        center = center
    )
}

/**
 * 图例组件
 */
@Composable
private fun ChartLegend(
    chartData: List<ChartData>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ModernSpacing.Medium),
        contentPadding = PaddingValues(horizontal = ModernSpacing.Small)
    ) {
        items(chartData) { data ->
            LegendItem(
                chartData = data,
                modifier = Modifier
            )
        }
    }
}

/**
 * 图例项组件
 */
@Composable
private fun LegendItem(
    chartData: ChartData,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
    ) {
        // 颜色指示器
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .drawBehind {
                    drawCircle(color = chartData.color)
                }
        )
        
        // 标签和数值
        Column {
            Text(
                text = chartData.label,
                style = MaterialTheme.typography.labelMedium,
                color = ModernColors.OnSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "¥${chartData.getFormattedValue()}",
                style = MaterialTheme.typography.labelSmall,
                color = ModernColors.OnSurfaceVariant
            )
            Text(
                text = chartData.getFormattedPercentage(),
                style = MaterialTheme.typography.labelSmall,
                color = ModernColors.OnSurfaceVariant
            )
        }
    }
}

/**
 * 获取图表颜色的工具函数
 * 根据深色模式自动调整颜色
 */
@Composable
fun getChartColors(): Triple<Color, Color, Color> {
    val isLight = ModernColors.Surface.luminance() > 0.5f
    
    return if (isLight) {
        // 浅色模式颜色
        Triple(
            Color(0xFF2196F3), // 蓝色 - 租金
            Color(0xFF4CAF50), // 绿色 - 水费
            Color(0xFFFF9800)  // 橙色 - 电费
        )
    } else {
        // 深色模式颜色 - 更亮更饱和
        Triple(
            Color(0xFF64B5F6), // 亮蓝色 - 租金
            Color(0xFF81C784), // 亮绿色 - 水费
            Color(0xFFFFB74D)  // 亮橙色 - 电费
        )
    }
}

/**
 * 扩展函数：获取颜色的亮度值
 */
private fun Color.luminance(): Float {
    val r = if (red <= 0.03928f) red / 12.92f else ((red + 0.055f) / 1.055f).pow(2.4f)
    val g = if (green <= 0.03928f) green / 12.92f else ((green + 0.055f) / 1.055f).pow(2.4f)
    val b = if (blue <= 0.03928f) blue / 12.92f else ((blue + 0.055f) / 1.055f).pow(2.4f)
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}