package com.morgen.rentalmanager.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import com.morgen.rentalmanager.ui.theme.*
import kotlinx.coroutines.delay

/**
 * 全局动画系统组件
 * 提供统一的动画效果和交互反馈
 */

/**
 * 覆盖式动画规范
 */
object ScreenTransitions {
    // 覆盖式动画 - 新界面从左侧覆盖当前界面
    val coverFromLeft = slideInHorizontally(
        animationSpec = tween(
            durationMillis = 500,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f) // 使用特殊缓动曲线，开始快然后减速
        )
    ) { fullWidth -> -fullWidth }

    // 覆盖式动画 - 当前界面被覆盖时的移动效果
    val slideRightWhenCovered = slideOutHorizontally(
        animationSpec = tween(
            durationMillis = 500,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f) // 与coverFromLeft使用相同的缓动曲线
        )
    ) { fullWidth -> fullWidth } // 向右移出屏幕

    // 覆盖式动画 - 覆盖层收回
    val uncoverToLeft = slideOutHorizontally(
        animationSpec = tween(
            durationMillis = 450,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        )
    ) { fullWidth -> -fullWidth }

    // 覆盖式动画 - 被覆盖层恢复原位
    val slideBackWhenUncovered = slideInHorizontally(
        animationSpec = tween(
            durationMillis = 450,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        )
    ) { fullWidth -> fullWidth } // 从右侧完全恢复

    // 覆盖式动画 - 新界面从右侧覆盖当前界面
    val coverFromRight = slideInHorizontally(
        animationSpec = tween(
            durationMillis = 500,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        )
    ) { fullWidth -> fullWidth }

    // 覆盖式动画 - 当前界面被右侧覆盖时向左移动
    val slideLeftWhenCovered = slideOutHorizontally(
        animationSpec = tween(
            durationMillis = 500,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        )
    ) { fullWidth -> -fullWidth } // 向左移出屏幕

    // 覆盖式动画 - 右侧覆盖层收回
    val uncoverToRight = slideOutHorizontally(
        animationSpec = tween(
            durationMillis = 450,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        )
    ) { fullWidth -> fullWidth }

    // 覆盖式动画 - 被右侧覆盖层恢复原位
    val slideBackWhenRightUncovered = slideInHorizontally(
        animationSpec = tween(
            durationMillis = 450,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        )
    ) { fullWidth -> -fullWidth } // 从左侧恢复

    // 垂直覆盖式动画 - 新界面从底部覆盖，不使用淡入效果
    val coverFromBottom = slideInVertically(
        animationSpec = tween(
            durationMillis = 500,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        )
    ) { fullHeight -> fullHeight }

    // 垂直覆盖式动画 - 当前界面被覆盖时向上移动
    val slideUpWhenCovered = slideOutVertically(
        animationSpec = tween(
            durationMillis = 500,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        )
    ) { fullHeight -> -fullHeight } // 向上移动完全移出屏幕

    // 垂直覆盖式动画 - 覆盖层收回
    val uncoverToBottom = slideOutVertically(
        animationSpec = tween(
            durationMillis = 450,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        )
    ) { fullHeight -> fullHeight }

    // 垂直覆盖式动画 - 被覆盖层恢复原位
    val slideBackWhenVerticalUncovered = slideInVertically(
        animationSpec = tween(
            durationMillis = 450,
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
        )
    ) { fullHeight -> -fullHeight } // 从上方完全恢复
}

/**
 * 带点击反馈动画的按钮
 */
@Composable
fun AnimatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(ModernCorners.Small),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val animationSpec = tween<Float>(
        durationMillis = 150,
        easing = ModernAnimations.UltraSmoothOut
    )
    
    val targetScale = if (isPressed) 0.95f else 1f
    
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = animationSpec,
        label = "button_scale"
    )
    
    Button(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = modifier.scale(scale),
        enabled = enabled,
        colors = colors,
        elevation = elevation,
        shape = shape,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(75L)
            isPressed = false
        }
    }
}