package com.morgen.rentalmanager.ui.components

import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import com.morgen.rentalmanager.ui.theme.ModernAnimations
import com.morgen.rentalmanager.ui.theme.ModernColors
import com.morgen.rentalmanager.ui.theme.ModernCorners
import kotlinx.coroutines.delay

// 优化的动画曲线
private val EaseOutQuart = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * 手机应用风格的启动按钮
 * 点击时会执行放大→淡出→导航的动画序列
 */
@Composable
fun AppLaunchButton(
    onClick: () -> Unit,
    navController: NavController,
    route: String,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = ModernColors.Primary,
    contentColor: Color = Color.White,
    onLaunchStateChange: ((Boolean) -> Unit)? = null,
    onRouteNavigate: (() -> Boolean)? = null // 返回true表示执行默认导航，返回false表示由调用者处理导航
) {
    var isLaunching by remember { mutableStateOf(false) }
    var buttonVisible by remember { mutableStateOf(true) }
    var isPressed by remember { mutableStateOf(false) }
    
    // 点击反馈动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(
            durationMillis = 150,
            easing = ModernAnimations.UltraSmoothOut
        ),
        label = "button_scale"
    )
    
    // 按钮透明度动画
    val buttonAlpha by animateFloatAsState(
        targetValue = when {
            isLaunching -> 0f  // 淡出
            buttonVisible -> 1f
            else -> 0f
        },
        animationSpec = tween(
            durationMillis = 200,
            easing = EaseOutQuart
        ),
        label = "button_alpha"
    )
    
    // 启动动画完成后的处理
    LaunchedEffect(isLaunching) {
        if (isLaunching) {
            // 通知父组件启动状态变化，开始模糊主界面
            onLaunchStateChange?.invoke(true)
            
            // 立即执行路由导航回调
            val shouldUseDefaultNavigation = onRouteNavigate?.invoke() ?: true
            
            // 等待按钮动画完成
            delay(200)
            
            // 仅在需要默认导航时执行
            if (shouldUseDefaultNavigation) {
                navController.navigate(route)
            }
            
            // 等待页面完全展开
            delay(50)
            
            // 重置模糊效果和按钮状态
            onLaunchStateChange?.invoke(false)
            
            // 延迟重置按钮状态
            delay(100)
            isLaunching = false
            buttonVisible = true
        }
    }
    
    // 点击效果重置
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(150)
            isPressed = false
        }
    }
    
    // 组件销毁时确保状态重置
    DisposableEffect(Unit) {
        onDispose {
            // 确保模糊效果被清除
            onLaunchStateChange?.invoke(false)
        }
    }
    
    // 使用简洁的按钮样式，参考收据按钮
    OutlinedButton(
        onClick = {
            if (!isLaunching) {
                isPressed = true
                onClick()
                isLaunching = true
            }
        },
        shape = RoundedCornerShape(ModernCorners.Large),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = BorderStroke(0.dp, Color.Transparent), // 无边框
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 1.dp,
            pressedElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), // 更大的内边距
        modifier = modifier
            .height(48.dp) // 更大的高度
            .scale(scale)
            .alpha(buttonAlpha)
    ) {
        // 简洁的内容布局
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(0.dp) // 无额外内边距
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp) // 更大的图标
            )
            Spacer(modifier = Modifier.width(8.dp)) // 更大的间距
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}