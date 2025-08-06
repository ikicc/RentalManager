package com.morgen.rentalmanager.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow

/**
 * 现代化设计系统
 * 定义应用的色彩、动画和层级规范
 * 全面支持深色模式
 */
object ModernColors {
    // 从MaterialTheme获取颜色，自动适配深色模式
    private val colorScheme @Composable get() = MaterialTheme.colorScheme
    
    // 主色系 - 活力蓝，深色模式下自动调整
    val Primary @Composable get() = colorScheme.primary
    val PrimaryVariant @Composable get() = colorScheme.primaryContainer
    val PrimaryContainer @Composable get() = colorScheme.primaryContainer
    val OnPrimary @Composable get() = colorScheme.onPrimary
    val OnPrimaryContainer @Composable get() = colorScheme.onPrimaryContainer
    
    // 表面色系 - 自动适配深色模式
    val Surface @Composable get() = colorScheme.surface
    val SurfaceVariant @Composable get() = colorScheme.surfaceVariant
    val SurfaceTint @Composable get() = colorScheme.surfaceTint
    val SurfaceContainer @Composable get() = colorScheme.surfaceContainer
    val SurfaceContainerHigh @Composable get() = colorScheme.surfaceContainerHigh
    val SurfaceContainerHighest @Composable get() = colorScheme.surfaceContainerHighest
    val Background @Composable get() = colorScheme.background
    
    // 文字色系 - 自动适配深色模式，优化可读性
    val OnSurface @Composable get() = colorScheme.onSurface
    val OnSurfaceVariant @Composable get() = colorScheme.onSurfaceVariant
    val OnBackground @Composable get() = colorScheme.onBackground
    val OnSurfaceSecondary @Composable get() = colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    val OnSurfaceTertiary @Composable get() = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    
    // 边框和分割线 - 自动适配深色模式
    val Outline @Composable get() = colorScheme.outline
    val OutlineVariant @Composable get() = colorScheme.outlineVariant
    
    // 状态色系 - 深色模式下保持良好对比度
    val Success @Composable get() = if (colorScheme.surface.luminance() > 0.5f) {
        Color(0xFF34A853) // 浅色模式
    } else {
        Color(0xFF4CAF50) // 深色模式，更亮的绿色
    }
    val Warning @Composable get() = if (colorScheme.surface.luminance() > 0.5f) {
        Color(0xFFFBBC04) // 浅色模式
    } else {
        Color(0xFFFFD54F) // 深色模式，更亮的黄色
    }
    val Error @Composable get() = colorScheme.error
    val OnError @Composable get() = colorScheme.onError
    val ErrorContainer @Composable get() = colorScheme.errorContainer
    val OnErrorContainer @Composable get() = colorScheme.onErrorContainer
    
    // 透明度变体 - 自动适配深色模式
    val SurfaceAlpha12 @Composable get() = Surface.copy(alpha = 0.12f)
    val SurfaceAlpha24 @Composable get() = Surface.copy(alpha = 0.24f)
    val OnSurfaceAlpha12 @Composable get() = OnSurface.copy(alpha = 0.12f)
    val OnSurfaceAlpha24 @Composable get() = OnSurface.copy(alpha = 0.24f)
    
    // 特殊效果色 - 深色模式优化
    val ScrimColor @Composable get() = Color.Black.copy(alpha = 0.32f)
    val InverseSurface @Composable get() = colorScheme.inverseSurface
    val InverseOnSurface @Composable get() = colorScheme.inverseOnSurface
    val InversePrimary @Composable get() = colorScheme.inversePrimary
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

/**
 * 动画规范
 */
object ModernAnimations {
    // 基础动画时长 - 优化为帧率对齐，确保120fps
    const val FAST_DURATION = 100     // 12帧 @120fps
    const val STANDARD_DURATION = 166 // 20帧 @120fps 
    const val SLOW_DURATION = 250     // 30帧 @120fps
    
    // 优化的缓动函数 - 解决动画结束时的顿挫感
    val FastOutSlowIn = FastOutSlowInEasing
    val SlowInFastOut = LinearOutSlowInEasing // FastOutSlowIn的反向缓动
    val LinearOutSlowIn = LinearOutSlowInEasing
    val FastOutLinearIn = FastOutLinearInEasing
    
    // 自定义平滑缓动函数 - 专门解决接近完成时的过渡问题
    val SmoothOutEasing = CubicBezierEasing(0.0f, 0.0f, 0.15f, 1.0f)  // 更平滑的结束，减少最后阶段的急促感
    val ExtraSlowOut = CubicBezierEasing(0.0f, 0.0f, 0.05f, 1.0f)     // 极其平滑的结束，几乎无感知的最后阶段
    val UltraSmoothOut = CubicBezierEasing(0.33f, 0.0f, 0.2f, 1.0f)   // 更极端的平滑曲线，专门解决"快到顶"顿挫
    val PerfectSmoothOut = CubicBezierEasing(0.4f, 0.0f, 0.1f, 1.0f)  // 最平滑的曲线，几乎无感知的结尾
    
    // 预定义动画规范 - 使用优化的缓动函数
    val FastTransition = tween<Float>(
        durationMillis = FAST_DURATION,
        easing = SmoothOutEasing  // 使用更平滑的缓动
    )
    
    val StandardTransition = tween<Float>(
        durationMillis = STANDARD_DURATION,
        easing = SmoothOutEasing  // 使用更平滑的缓动
    )
    
    val SlowTransition = tween<Float>(
        durationMillis = SLOW_DURATION,
        easing = ExtraSlowOut  // 使用极其平滑的缓动
    )
    
    // 优化的弹簧动画 - 减少过冲和震荡，确保精确帧数
    val SpringTransition = spring<Float>(
        dampingRatio = 0.9f,  // 略低于临界阻尼，避免震荡
        stiffness = Spring.StiffnessMediumLow,  // 降低刚度，减少突兀感
    )
    
    // 优化的进入动画 - 更平滑的过渡，帧数对齐
    val EnterTransition = slideInVertically(
        animationSpec = tween(STANDARD_DURATION, easing = SmoothOutEasing)
    ) { it / 2 } + fadeIn(
        animationSpec = tween(STANDARD_DURATION, easing = SmoothOutEasing)
    )
    
    // 优化的退出动画 - 更平滑的过渡，帧数对齐
    val ExitTransition = slideOutVertically(
        animationSpec = tween(FAST_DURATION, easing = SmoothOutEasing)
    ) { it / 2 } + fadeOut(
        animationSpec = tween(FAST_DURATION, easing = SmoothOutEasing)
    )
}

/**
 * 层级系统
 */
object ModernElevation {
    val Level0 = 0.dp    // 背景层
    val Level1 = 1.dp    // 卡片层
    val Level2 = 4.dp    // 浮动按钮层
    val Level3 = 8.dp    // 弹窗层
    val Level4 = 16.dp   // 模态层
    val Level5 = 24.dp   // 最高层
}

/**
 * 间距系统 - 基于8dp网格
 */
object ModernSpacing {
    val XSmall = 4.dp
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp
    val XLarge = 32.dp
    val XXLarge = 48.dp
    val XXXLarge = 64.dp
}

/**
 * 圆角系统
 */
object ModernCorners {
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val XLarge = 20.dp
    val XXLarge = 28.dp
}

/**
 * 模糊效果
 */
object ModernBlur {
    val Light = 8.dp
    val Medium = 12.dp
    val Heavy = 16.dp
}

/**
 * 图标尺寸系统
 */
object ModernIconSize {
    val XSmall = 12.dp
    val Small = 16.dp
    val Medium = 20.dp
    val Large = 24.dp
    val XLarge = 32.dp
    val XXLarge = 48.dp
}

/**
 * 触摸目标尺寸
 */
object ModernTouchTarget {
    val Minimum = 48.dp  // Material Design 最小触摸目标
    val Comfortable = 56.dp  // 舒适的触摸目标
    val Large = 64.dp  // 大型触摸目标
}

/**
 * 文字系统 - 优化可读性
 */
object ModernTypography {
    // 字体大小
    val TitleLarge = 22.sp
    val TitleMedium = 18.sp
    val TitleSmall = 16.sp
    val BodyLarge = 16.sp
    val BodyMedium = 14.sp
    val BodySmall = 12.sp
    val LabelLarge = 14.sp
    val LabelMedium = 12.sp
    val LabelSmall = 10.sp
    
    // 行高 - 提高可读性
    val LineHeightLarge = 28.sp
    val LineHeightMedium = 20.sp
    val LineHeightSmall = 16.sp
    val LineHeightCompact = 14.sp
}