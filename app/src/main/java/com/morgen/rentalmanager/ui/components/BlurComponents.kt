package com.morgen.rentalmanager.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.morgen.rentalmanager.ui.theme.ModernAnimations
import com.morgen.rentalmanager.ui.theme.ModernBlur
import com.morgen.rentalmanager.utils.DevicePerformanceTier
import com.morgen.rentalmanager.utils.PerformanceUtils
import kotlinx.coroutines.delay

/**
 * 真正的高斯模糊组件 - 不会让界面变暗
 * 
 * @param isVisible Whether the blur overlay is visible
 * @param blurRadius The radius of the blur effect
 * @param content The content to be displayed
 */
@Composable
fun GaussianBlurOverlay(
    isVisible: Boolean,
    blurRadius: Float = ModernBlur.Medium.value,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val performanceTier = remember { PerformanceUtils.detectDevicePerformanceTier(context) }
    
    // 根据设备性能优化模糊半径
    val optimizedBlurRadius = remember(blurRadius, performanceTier) {
        PerformanceUtils.optimizeBlurRadius(blurRadius, performanceTier)
    }
    
    // 优化动画持续时间
    val animDuration = remember(performanceTier) {
        PerformanceUtils.optimizeAnimationDuration(
            ModernAnimations.STANDARD_DURATION, 
            performanceTier
        )
    }
    
    // 延迟初始化模糊效果，解决首次渲染卡顿问题
    var isInitialized by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // 延迟200毫秒初始化，让UI先渲染出来
        delay(200)
        isInitialized = true
    }
    
    // 使用平滑的动画过渡
    val animatedBlurRadius by animateFloatAsState(
        targetValue = if (isVisible && isInitialized) optimizedBlurRadius else 0f,
        animationSpec = tween(
            durationMillis = animDuration,
            easing = ModernAnimations.SmoothOutEasing
        ),
        label = "gaussianBlurRadius"
    )
    
    // 纯高斯模糊效果，不添加任何遮罩层，保持原始亮度
    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(radius = animatedBlurRadius.dp)
    ) {
        content()
    }
}

/**
 * 兼容旧版本的模糊组件 - 保持向后兼容
 */
@Composable
fun BlurOverlay(
    isVisible: Boolean,
    blurRadius: Float = ModernBlur.Medium.value,
    overlayColor: Color = Color.Transparent, // 默认透明，不添加遮罩
    overlayAlpha: Float = 0f, // 默认不透明度为0，不变暗
    content: @Composable BoxScope.() -> Unit
) {
    // 直接使用高斯模糊，不添加变暗效果
    GaussianBlurOverlay(
        isVisible = isVisible,
        blurRadius = blurRadius,
        content = content
    )
}

/**
 * 分层UI组件 - 使用纯高斯模糊，不变暗
 * 支持性能优化，延迟初始化
 * 
 * @param showBlur Whether to show the blur effect
 * @param backgroundContent The content to be displayed in the background (will be blurred)
 * @param foregroundContent The content to be displayed in the foreground (will not be blurred)
 */
@Composable
fun LayeredUI(
    showBlur: Boolean,
    backgroundContent: @Composable () -> Unit,
    foregroundContent: @Composable () -> Unit
) {
    val context = LocalContext.current
    val performanceTier = remember { PerformanceUtils.detectDevicePerformanceTier(context) }
    
    // 延迟初始化模糊效果，解决首次渲染卡顿问题
    var isInitialized by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // 延迟时间根据设备性能调整
        val delayTime = when(performanceTier) {
            DevicePerformanceTier.LOW -> 500L // 低端设备延迟更长
            DevicePerformanceTier.MEDIUM -> 300L
            DevicePerformanceTier.HIGH -> 100L // 高端设备延迟短
        }
        delay(delayTime)
        isInitialized = true
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 使用纯高斯模糊效果，不添加遮罩层
        if (isInitialized) {
            GaussianBlurOverlay(
                isVisible = showBlur,
                content = { backgroundContent() }
            )
        } else {
            // 未初始化时显示不带模糊的内容
            Box(modifier = Modifier.fillMaxSize()) {
                backgroundContent()
            }
        }
        
        // 前景内容始终在顶部，不管是否模糊
        foregroundContent()
    }
}

/**
 * 模态弹窗组件 - 使用纯高斯模糊，不变暗
 * 延迟初始化提高性能
 * 
 * @param isVisible Whether the modal is visible
 * @param onDismiss Callback when the modal is dismissed
 * @param modalContent The content to be displayed in the modal
 */
@Composable
fun BlurredModal(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modalContent: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val performanceTier = remember { PerformanceUtils.detectDevicePerformanceTier(context) }
    
    // 根据设备性能优化模糊半径
    val optimizedBlurRadius = remember(performanceTier) {
        PerformanceUtils.optimizeBlurRadius(ModernBlur.Medium.value, performanceTier)
    }
    
    if (isVisible) {
        // 使用纯高斯模糊效果，不添加背景色和透明度
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = optimizedBlurRadius.dp) // 使用优化的模糊半径
        ) {
            modalContent()
        }
    }
}