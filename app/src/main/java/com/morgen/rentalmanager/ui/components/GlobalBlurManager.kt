package com.morgen.rentalmanager.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.morgen.rentalmanager.ui.theme.ModernBlur
import com.morgen.rentalmanager.ui.theme.ModernAnimations
import com.morgen.rentalmanager.utils.DevicePerformanceTier
import com.morgen.rentalmanager.utils.PerformanceUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 全局模糊管理器
 * 用于在界面切换时提供全局模糊效果
 */
object GlobalBlurManager {
    private val _isBlurring = mutableStateOf(false)
    val isBlurring: State<Boolean> = _isBlurring
    
    // 添加初始化标志，避免冷启动时就应用模糊效果
    private val _isInitialized = mutableStateOf(false)
    val isInitialized: State<Boolean> = _isInitialized
    
    // 添加性能等级缓存
    private var performanceTier: DevicePerformanceTier? = null
    
    fun initialize(context: android.content.Context) {
        if (!_isInitialized.value) {
            // 检测设备性能等级并缓存结果
            performanceTier = PerformanceUtils.detectDevicePerformanceTier(context)
            _isInitialized.value = true
        }
    }
    
    fun startBlur() {
        // 只有在初始化后才应用模糊效果
        if (_isInitialized.value) {
            _isBlurring.value = true
        }
    }
    
    fun stopBlur() {
        _isBlurring.value = false
    }
    
    // 获取根据性能调整的模糊半径
    fun getOptimizedBlurRadius(baseRadius: Float): Float {
        return when (performanceTier) {
            DevicePerformanceTier.LOW -> baseRadius * 0.5f // 低端设备减少50%模糊半径
            DevicePerformanceTier.MEDIUM -> baseRadius * 0.8f // 中端设备减少20%模糊半径
            DevicePerformanceTier.HIGH -> baseRadius // 高端设备使用完整模糊效果
            null -> baseRadius // 默认值
        }
    }
    
    suspend fun blurForDuration(durationMs: Long = 300L) {
        // 只有在初始化后才应用模糊效果
        if (_isInitialized.value) {
            startBlur()
            delay(durationMs)
            stopBlur()
        }
    }
}

/**
 * 全局高斯模糊容器 - 纯高斯模糊，不变暗
 * 包装整个应用内容，提供全局模糊效果
 */
@Composable
fun GlobalBlurContainer(
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val isBlurring by GlobalBlurManager.isBlurring
    val isInitialized by GlobalBlurManager.isInitialized
    
    // 确保模糊管理器已初始化
    LaunchedEffect(Unit) {
        // 延迟初始化，避免首次加载卡顿
        delay(500) // 等待应用基本加载完成
        GlobalBlurManager.initialize(context)
    }
    
    // 检测设备性能
    val performanceTier = remember { PerformanceUtils.detectDevicePerformanceTier(context) }
    
    // 根据性能调整模糊半径和动画时间
    val baseBlurRadius = ModernBlur.Medium.value
    val optimizedBlurRadius = remember(performanceTier) {
        PerformanceUtils.optimizeBlurRadius(baseBlurRadius, performanceTier)
    }
    
    val animDuration = remember(performanceTier) {
        when(performanceTier) {
            DevicePerformanceTier.LOW -> 100 // 低端设备使用更短的动画时间
            DevicePerformanceTier.MEDIUM -> 150
            DevicePerformanceTier.HIGH -> 200
        }
    }
    
    // 使用平滑的高斯模糊动画过渡
    val blurRadius by animateFloatAsState(
        targetValue = if (isBlurring && isInitialized) optimizedBlurRadius else 0f,
        animationSpec = tween(
            durationMillis = animDuration, // 使用根据性能调整的动画时间
            easing = ModernAnimations.PerfectSmoothOut // 使用更平滑的缓动函数
        ),
        label = "global_gaussian_blur"
    )
    
    // 纯高斯模糊效果，保持原始亮度
    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(radius = blurRadius.dp)
    ) {
        content()
    }
}

/**
 * 导航扩展函数
 * 在导航时自动添加模糊效果
 */
fun NavController.navigateWithBlur(route: String) {
    GlobalBlurManager.startBlur()
    navigate(route)
    
    // 使用协程延迟关闭模糊效果 - 减少等待时间提高响应速度
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        delay(300) // 减少等待时间，提高响应速度
        GlobalBlurManager.stopBlur()
    }
}